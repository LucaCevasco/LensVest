//! Chat with the LensVest agent over the user's own Berry account.
//!
//! Each user has one Managed Agents session bound to their vault, looked up
//! from the JWT: the app never sends a session id, so nobody can reach another
//! user's Berry account.

use std::time::Duration;

use loco_rs::prelude::*;
use serde::Deserialize;
use serde_json::{json, Value};

use crate::{
    data::managed_agents::{ManagedAgents, Turn},
    models::{berry_connections, users},
};

#[derive(Debug, Deserialize)]
pub struct MessageParams {
    pub text: String,
}

#[derive(Debug, Deserialize)]
pub struct ConfirmParams {
    /// Applies to every action the agent is waiting on (usually one order).
    pub approve: bool,
}

/// Tool calls a session is paused on, remembered between requests.
// ponytail: in-memory cache, lost on restart; read the session's events instead if that bites.
fn pending_key(session_id: &str) -> String {
    format!("assistant_pending:{session_id}")
}

/// The user's session, or a 400 when Berry isn't connected yet.
async fn user_session(ctx: &AppContext, api: &ManagedAgents, pid: &str) -> Result<String> {
    let user = users::Model::find_by_pid(&ctx.db, pid).await?;
    let conn = berry_connections::Model::find_by_user(&ctx.db, user.id)
        .await?
        .ok_or_else(|| Error::BadRequest("Conectá tu cuenta de Berry primero".to_string()))?;
    conn.session(&ctx.db, api).await
}

fn confirmations(action_ids: &[String], approve: bool) -> Vec<Value> {
    action_ids
        .iter()
        .map(|id| {
            if approve {
                json!({ "type": "user.tool_confirmation", "tool_use_id": id, "result": "allow" })
            } else {
                json!({
                    "type": "user.tool_confirmation",
                    "tool_use_id": id,
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
    let turn = api.run_turn(session_id, Value::Array(events)).await?;
    let ids: Vec<String> = turn.pending_actions.iter().map(|a| a.id.clone()).collect();
    ctx.cache
        .insert_with_expiry(&pending_key(session_id), &ids, Duration::from_secs(86_400))
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
    let session_id = user_session(&ctx, &api, &auth.claims.pid).await?;

    // A new message means the user moved on: reject any order still awaiting
    // confirmation, or the session would keep waiting for it.
    let pending: Vec<String> = ctx
        .cache
        .get(&pending_key(&session_id))
        .await
        .map_err(Error::wrap)?
        .unwrap_or_default();
    let mut events = confirmations(&pending, false);
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
    let session_id = user_session(&ctx, &api, &auth.claims.pid).await?;
    let pending: Vec<String> = ctx
        .cache
        .get(&pending_key(&session_id))
        .await
        .map_err(Error::wrap)?
        .unwrap_or_default();
    if pending.is_empty() {
        return bad_request("No hay ninguna operación esperando confirmación");
    }
    format::json(
        run(
            &ctx,
            &api,
            &session_id,
            confirmations(&pending, params.approve),
        )
        .await?,
    )
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

    #[test]
    fn deny_tells_the_agent_why() {
        let events = confirmations(&["sevt_1".to_string()], false);
        assert_eq!(events[0]["result"], "deny");
        assert_eq!(events[0]["tool_use_id"], "sevt_1");
        assert!(events[0]["deny_message"].is_string());
        assert_eq!(
            confirmations(&["sevt_1".to_string()], true)[0]["result"],
            "allow"
        );
    }
}
