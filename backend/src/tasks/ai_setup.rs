//! One-time setup of the Claude Managed Agents resources LensVest uses.
//!
//! `cargo loco task ai_setup` creates the environment and the agent the first
//! time (copy the printed ids into `settings.anthropic`). Once the ids are in
//! the config, running it again pushes a new version of the agent definition.

use loco_rs::prelude::*;
use serde_json::{json, Value};

use crate::{data::managed_agents::ManagedAgents, models::berry_connections::BerrySettings};

const MODEL: &str = "claude-opus-5-5";

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
- No des recomendaciones personalizadas de inversión: informá datos, no aconsejes qué comprar.";

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
        }]
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
        let agent = agent_definition(&BerrySettings::from_context(ctx)?);

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

        println!("settings.anthropic.environment_id: {environment_id}");
        println!("settings.anthropic.agent_id: {agent_id}");
        Ok(())
    }
}
