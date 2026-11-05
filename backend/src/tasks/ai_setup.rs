//! One-time setup of the Claude Managed Agents resources LensVest uses.
//!
//! `cargo loco task ai_setup` creates the environment and the agent the first
//! time (copy the printed ids into `settings.anthropic`). Once the ids are in
//! the config, running it again pushes a new version of the agent definition.

use loco_rs::prelude::*;
use serde_json::{json, Value};

use sea_orm::{sea_query::Expr, EntityTrait};

use crate::{
    data::managed_agents::ManagedAgents,
    models::{_entities::berry_connections, berry_connections::BerrySettings},
};

const MODEL: &str = "claude-opus-5-5";
/// The data agent only reads and reshapes: a faster model is enough.
const DATA_MODEL: &str = "claude-sonnet-5-5";

/// Berry tools that move money: the session pauses until the user approves them.
const TOOLS_REQUIRING_APPROVAL: [&str; 2] = ["place_order", "cancel_order"];

const SYSTEM_PROMPT: &str = "\
Sos el asistente de LensVest, una app que opera sobre la cuenta de Berry del usuario.
Respondé en español rioplatense, breve y claro: el usuario está en el celular.

Usá las herramientas de Berry para todo lo que sea de su cuenta (portfolio, órdenes, activos, precios).
- Para comprar o vender, llamá directamente a place_order con los parámetros exactos; no la describas
  ni pidas permiso por texto. La app le muestra la orden al usuario y él la confirma o la rechaza.
- Si falta un dato para la orden (activo, compra/venta o monto), preguntalo en vez de suponerlo.
- Los montos son en USD salvo que el usuario diga otra cosa.
- Nunca digas que una orden se ejecutó hasta que el resultado de la herramienta lo confirme.
  Si el usuario la rechaza, aceptalo y no la vuelvas a intentar.
- No des recomendaciones personalizadas de inversión: informá datos, no aconsejes qué comprar.

Estrategias: si el usuario pide algo condicional a futuro (\"si NVDA baja 10 %, comprá USD 100\"),
no operes. Consultá el precio actual con get_asset_data y llamá a propose_strategy: la app le muestra la
estrategia y él decide si activarla. Una estrategia tiene una sola condición de precio y una sola orden.";

const DATA_SYSTEM_PROMPT: &str = "\
Sos el servicio de datos de LensVest. Cada pedido dice qué herramientas de Berry consultar y con qué
herramienta show_* responder. Consultá en paralelo cuando puedas, llamá la herramienta show_* una sola
vez con los datos reales que devolvió Berry y no escribas texto. Nunca inventes números: si un dato no
está disponible, mandalo en null. Los montos van en USD.";

/// A client-side tool: the session pauses and the backend reads its input.
fn custom_tool(name: &str, description: &str, properties: Value) -> Value {
    let required: Vec<&String> = properties
        .as_object()
        .map(|p| p.keys().collect())
        .unwrap_or_default();
    json!({
        "type": "custom",
        "name": name,
        "description": description,
        "input_schema": { "type": "object", "properties": properties, "required": required }
    })
}

fn num() -> Value {
    json!({ "type": "number" })
}

fn nullable_num() -> Value {
    json!({ "type": ["number", "null"] })
}

fn string() -> Value {
    json!({ "type": "string" })
}

fn nullable_string() -> Value {
    json!({ "type": ["string", "null"] })
}

fn propose_strategy_tool() -> Value {
    custom_tool(
        "propose_strategy",
        "Le muestra al usuario una estrategia condicional para que la active. No ejecuta nada.",
        json!({
            "ticker": string(),
            "name": { "type": "string", "description": "Nombre de la empresa" },
            "condition": {
                "type": "string",
                "enum": ["price_below", "price_above", "drop_pct", "rise_pct"],
                "description": "drop_pct / rise_pct: variación % desde el precio actual"
            },
            "threshold": { "type": "number", "description": "Precio en USD, o porcentaje (10 = 10 %)" },
            "reference_price": { "type": "number", "description": "Precio actual en USD" },
            "side": { "type": "string", "enum": ["buy", "sell"] },
            "amount_usd": num(),
            "title": { "type": "string", "description": "Ej: \"Comprar NVDA si baja 10 %\"" }
        }),
    )
}

/// The `show_*` tools the data agent answers with, one per kind of screen.
fn data_tools() -> Vec<Value> {
    let position = json!({
        "type": ["object", "null"],
        "properties": {
            "quantity": num(), "value_usd": num(), "gain_usd": nullable_num(),
            "gain_pct": nullable_num(), "avg_price_usd": nullable_num()
        },
        "required": ["quantity", "value_usd", "gain_usd", "gain_pct", "avg_price_usd"]
    });
    let quote = json!({
        "type": "object",
        "properties": { "ticker": string(), "name": string(), "price_usd": num(), "change_pct": nullable_num() },
        "required": ["ticker", "name", "price_usd", "change_pct"]
    });
    vec![
        custom_tool(
            "show_portfolio",
            "Muestra el portfolio en el inicio de la app.",
            json!({
                "total_value_usd": num(),
                "cash_usd": nullable_num(),
                "change_usd": { "type": ["number", "null"], "description": "Variación del último mes" },
                "change_pct": { "type": ["number", "null"], "description": "Variación % del último mes" },
                "history": { "type": "array", "items": num(), "description": "Valor del portfolio en el último mes, hasta 30 puntos, del más viejo al más nuevo" },
                "positions": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                            "ticker": string(), "name": string(), "quantity": num(),
                            "value_usd": num(), "change_pct": nullable_num()
                        },
                        "required": ["ticker", "name", "quantity", "value_usd", "change_pct"]
                    }
                }
            }),
        ),
        custom_tool(
            "show_asset",
            "Muestra la ficha de un activo.",
            json!({
                "ticker": string(),
                "name": string(),
                "price_usd": num(),
                "change_pct": { "type": ["number", "null"], "description": "Variación % de hoy" },
                "history": { "type": "array", "items": num(), "description": "Precio del último mes, hasta 30 puntos, del más viejo al más nuevo" },
                "about": { "type": "string", "description": "Una o dos frases en español rioplatense simple sobre qué hace la empresa" },
                "brands": { "type": "array", "items": string(), "description": "Hasta 4 marcas conocidas de la empresa" },
                "position": position,
                "cash_usd": { "type": ["number", "null"], "description": "Efectivo disponible para invertir" },
                "open": { "type": ["number", "null"], "description": "Apertura de hoy" },
                "day_high": nullable_num(),
                "day_low": nullable_num(),
                "previous_close": nullable_num(),
                "volume": { "type": ["string", "null"], "description": "Volumen de hoy, tal como lo da Berry" },
                "ceo": nullable_string(),
                "founded": { "type": ["string", "null"], "description": "Año de fundación" },
                "employees": nullable_string(),
                "headquarters": nullable_string()
            }),
        ),
        custom_tool(
            "show_quotes",
            "Muestra el precio actual de varios activos.",
            json!({ "quotes": { "type": "array", "items": quote } }),
        ),
        custom_tool(
            "show_search_results",
            "Muestra resultados de búsqueda de activos.",
            json!({
                "results": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": { "ticker": string(), "name": string() },
                        "required": ["ticker", "name"]
                    }
                }
            }),
        ),
    ]
}

fn data_agent_definition(berry: &BerrySettings) -> Value {
    let mut tools = vec![json!({
        "type": "mcp_toolset",
        "mcp_server_name": "berry",
        "default_config": { "enabled": true, "permission_policy": { "type": "always_allow" } },
        // Read-only: this agent never moves money.
        "configs": TOOLS_REQUIRING_APPROVAL
            .iter()
            .map(|name| json!({ "name": name, "enabled": false }))
            .collect::<Vec<_>>()
    })];
    tools.extend(data_tools());
    json!({
        "name": "LensVest datos",
        "model": { "id": DATA_MODEL, "effort": "low" },
        "system": DATA_SYSTEM_PROMPT,
        "mcp_servers": [{ "type": "url", "name": "berry", "url": berry.mcp_url }],
        "tools": tools
    })
}

fn agent_definition(berry: &BerrySettings) -> Value {
    let approvals: Vec<Value> = TOOLS_REQUIRING_APPROVAL
        .iter()
        .map(|name| json!({ "name": name, "permission_policy": { "type": "always_ask" } }))
        .collect();
    json!({
        "name": "LensVest",
        "model": MODEL,
        "system": SYSTEM_PROMPT,
        "mcp_servers": [{ "type": "url", "name": "berry", "url": berry.mcp_url }],
        // Only Berry's tools: no bash or file tools in the session container.
        "tools": [{
            "type": "mcp_toolset",
            "mcp_server_name": "berry",
            "default_config": { "enabled": true, "permission_policy": { "type": "always_allow" } },
            "configs": approvals
        }, propose_strategy_tool()]
    })
}

pub struct AiSetup;
#[async_trait]
impl Task for AiSetup {
    fn task(&self) -> TaskInfo {
        TaskInfo {
            name: "ai_setup".to_string(),
            detail: "Create or update the Managed Agents environment and agent".to_string(),
        }
    }

    async fn run(&self, ctx: &AppContext, _vars: &task::Vars) -> Result<()> {
        let api = ManagedAgents::from_context(ctx)?;
        let berry = BerrySettings::from_context(ctx)?;
        let agent = agent_definition(&berry);
        let data_agent = data_agent_definition(&berry);

        let environment_id = if api.settings.environment_id.is_empty() {
            let env = api
                .post(
                    "/environments",
                    &json!({
                        "name": "lensvest",
                        // The agent only talks to Berry's MCP server.
                        "config": {
                            "type": "cloud",
                            "networking": { "type": "limited", "allow_mcp_servers": true }
                        }
                    }),
                )
                .await?;
            ManagedAgents::id(&env)?
        } else {
            api.settings.environment_id.clone()
        };

        let agent_id = if api.settings.agent_id.is_empty() {
            ManagedAgents::id(&api.post("/agents", &agent).await?)?
        } else {
            let updated = api
                .post(&format!("/agents/{}", api.settings.agent_id), &agent)
                .await?;
            println!("agent updated to version {}", updated["version"]);
            api.settings.agent_id.clone()
        };

        let data_agent_id = if api.settings.data_agent_id.is_empty() {
            ManagedAgents::id(&api.post("/agents", &data_agent).await?)?
        } else {
            let updated = api
                .post(
                    &format!("/agents/{}", api.settings.data_agent_id),
                    &data_agent,
                )
                .await?;
            println!("data agent updated to version {}", updated["version"]);
            api.settings.data_agent_id.clone()
        };

        // Sessions stay on the agent version they were created with: drop them so
        // each user's next message starts one on the new version.
        berry_connections::Entity::update_many()
            .col_expr(
                berry_connections::Column::SessionId,
                Expr::value(Option::<String>::None),
            )
            .exec(&ctx.db)
            .await?;

        println!("settings.anthropic.environment_id: {environment_id}");
        println!("settings.anthropic.agent_id: {agent_id}");
        println!("settings.anthropic.data_agent_id: {data_agent_id}");
        Ok(())
    }
}
