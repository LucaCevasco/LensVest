//! Chat with the LensVest agent over the user's own Berry account.
//!
//! Each user has one Managed Agents session bound to their vault, looked up
//! from the JWT: the app never sends a session id, so nobody can reach another
//! user's Berry account.

use std::time::Duration;

use loco_rs::prelude::*;
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};

use crate::{
    data::{
        berry_data::{self, Query},
        managed_agents::{ManagedAgents, PendingAction, Turn, BERRY_RECONNECT, BUDGET_REACHED},
    },
    models::{berry_connections, strategies, users},
};

/// Our own tool: the agent proposes a strategy and the user activates it.
const PROPOSE_STRATEGY: &str = "propose_strategy";

#[derive(Debug, Deserialize)]
pub struct MessageParams {
    pub text: String,
}

#[derive(Debug, Deserialize)]
pub struct ConfirmParams {
    /// Applies to every action the agent is waiting on (usually one order).
    pub approve: bool,
}

/// A Berry tool the user approved, and whether Berry says it went through.
#[derive(Debug, Serialize)]
struct Executed {
    tool: String,
    input: Value,
    ok: bool,
    result: String,
}

#[derive(Debug, Serialize)]
struct TurnResponse {
    #[serde(flatten)]
    turn: Turn,
    /// Only on confirm. The app shows an order as done only from here (RNF06).
    executed: Vec<Executed>,
    /// The strategy the user just activated, if that's what they confirmed.
    strategy: Option<strategies::Model>,
}

/// Tool calls a session is paused on, remembered between requests.
// ponytail: in-memory cache, lost on restart; read the session's events instead if that bites.
fn pending_key(session_id: &str) -> String {
    format!("assistant_pending:{session_id}")
}

/// The user and their session, or a 400 when Berry isn't connected yet.
async fn user_session(
    ctx: &AppContext,
    api: &ManagedAgents,
    pid: &str,
) -> Result<(users::Model, String)> {
    let user = users::Model::find_by_pid(&ctx.db, pid).await?;
    let conn = berry_connections::Model::find_by_user(&ctx.db, user.id)
        .await?
        .ok_or_else(|| Error::BadRequest("Conectá tu cuenta de Berry primero".to_string()))?;
    let session_id = conn.session(&ctx.db, api).await?;
    Ok((user, session_id))
}

async fn pending(ctx: &AppContext, session_id: &str) -> Result<Vec<PendingAction>> {
    Ok(ctx
        .cache
        .get(&pending_key(session_id))
        .await
        .map_err(Error::wrap)?
        .unwrap_or_default())
}

/// The events that answer what the session waits on: approve/deny for Berry
/// tools, and a result for our custom tools.
fn answers(actions: &[PendingAction], approve: bool, custom_result: &str) -> Vec<Value> {
    actions
        .iter()
        .map(|action| {
            if action.tool == PROPOSE_STRATEGY {
                json!({
                    "type": "user.custom_tool_result",
                    "custom_tool_use_id": action.id,
                    "content": [{ "type": "text", "text": custom_result }]
                })
            } else if approve {
                json!({ "type": "user.tool_confirmation", "tool_use_id": action.id, "result": "allow" })
            } else {
                json!({
                    "type": "user.tool_confirmation",
                    "tool_use_id": action.id,
                    "result": "deny",
                    "deny_message": "El usuario no confirmó la operación."
                })
            }
        })
        .collect()
}

/// Runs a turn and remembers what the session is now waiting on.
async fn run(
    ctx: &AppContext,
    api: &ManagedAgents,
    session_id: &str,
    events: Vec<Value>,
) -> Result<Turn> {
    let turn = match api.run_turn(session_id, Value::Array(events)).await {
        Ok(turn) => turn,
        // A capped session can't take events anymore: start over on the next message.
        Err(err) if err.to_string().contains(BUDGET_REACHED) => {
            berry_connections::Model::forget_session(&ctx.db, session_id).await?;
            return Err(Error::BadRequest(
                "Esta conversación llegó a su límite. Mandá el mensaje de nuevo y arrancamos una nueva."
                    .to_string(),
            ));
        }
        Err(err) => return Err(err),
    };
    // Without a working Berry credential the agent can only apologize: ask to reconnect instead.
    if turn.berry_auth_failed && turn.pending_actions.is_empty() {
        return Err(Error::BadRequest(BERRY_RECONNECT.to_string()));
    }
    ctx.cache
        .insert_with_expiry(
            &pending_key(session_id),
            &turn.pending_actions,
            Duration::from_secs(86_400),
        )
        .await
        .map_err(Error::wrap)?;
    Ok(turn)
}

#[debug_handler]
async fn message(
    auth: auth::JWT,
    State(ctx): State<AppContext>,
    Json(params): Json<MessageParams>,
) -> Result<Response> {
    let text = params.text.trim();
    if text.is_empty() {
        return bad_request("El mensaje está vacío");
    }
    let api = ManagedAgents::from_context(&ctx)?;
    let (_, session_id) = user_session(&ctx, &api, &auth.claims.pid).await?;

    // A new message means the user moved on: reject any order still awaiting
    // confirmation, or the session would keep waiting for it.
    let mut events = answers(
        &pending(&ctx, &session_id).await?,
        false,
        "El usuario no activó la estrategia.",
    );
    events.push(json!({ "type": "user.message", "content": [{ "type": "text", "text": text }] }));

    format::json(run(&ctx, &api, &session_id, events).await?)
}

#[debug_handler]
async fn confirm(
    auth: auth::JWT,
    State(ctx): State<AppContext>,
    Json(params): Json<ConfirmParams>,
) -> Result<Response> {
    let api = ManagedAgents::from_context(&ctx)?;
    let (user, session_id) = user_session(&ctx, &api, &auth.claims.pid).await?;
    let pending = pending(&ctx, &session_id).await?;
    if pending.is_empty() {
        return bad_request("No hay ninguna operación esperando confirmación");
    }

    let mut strategy = None;
    let mut custom_result = "El usuario no activó la estrategia.".to_string();
    if params.approve {
        if let Some(proposal) = pending.iter().find(|a| a.tool == PROPOSE_STRATEGY) {
            let new: strategies::NewStrategy = serde_json::from_value(proposal.input.clone())?;
            match strategies::Model::create(&ctx.db, user.id, &new).await {
                Ok(created) => {
                    custom_result = "El usuario activó la estrategia.".to_string();
                    strategy = Some(created);
                }
                Err(err) => custom_result = format!("No se pudo guardar la estrategia: {err}"),
            }
        }
    }

    let turn = run(
        &ctx,
        &api,
        &session_id,
        answers(&pending, params.approve, &custom_result),
    )
    .await?;

    let executed: Vec<Executed> = pending
        .iter()
        .filter_map(|action| {
            let result = turn.tool_results.iter().find(|r| r.tool_use_id == action.id)?;
            Some(Executed {
                tool: action.tool.clone(),
                input: action.input.clone(),
                ok: !result.is_error,
                result: result.text.clone(),
            })
        })
        .collect();
    // An order changes the portfolio and that asset's position: don't show stale data.
    for done in executed.iter().filter(|e| e.ok) {
        berry_data::forget(&ctx, user.id, &Query::Portfolio).await?;
        if let Some(ticker) = done.input["ticker"].as_str() {
            berry_data::forget(&ctx, user.id, &Query::Asset(ticker.to_uppercase())).await?;
        }
    }

    format::json(TurnResponse {
        turn,
        executed,
        strategy,
    })
}

pub fn routes() -> Routes {
    Routes::new()
        .prefix("/api/assistant")
        .add("/messages", post(message))
        .add("/confirm", post(confirm))
}

#[cfg(test)]
mod tests {
    use super::*;

    fn action(id: &str, tool: &str) -> PendingAction {
        PendingAction {
            id: id.into(),
            tool: tool.into(),
            input: json!({}),
        }
    }

    #[test]
    fn deny_tells_the_agent_why() {
        let order = [action("sevt_1", "place_order")];
        let events = answers(&order, false, "");
        assert_eq!(events[0]["result"], "deny");
        assert_eq!(events[0]["tool_use_id"], "sevt_1");
        assert!(events[0]["deny_message"].is_string());
        assert_eq!(answers(&order, true, "")[0]["result"], "allow");
    }

    #[test]
    fn a_strategy_proposal_gets_a_custom_tool_result() {
        let events = answers(&[action("sevt_2", PROPOSE_STRATEGY)], true, "activada");
        assert_eq!(events[0]["type"], "user.custom_tool_result");
        assert_eq!(events[0]["custom_tool_use_id"], "sevt_2");
        assert_eq!(events[0]["content"][0]["text"], "activada");
    }
}
