use std::time::Duration;

use crate::models::{
    berry_connections::{self, pkce_challenge, BerrySettings},
    users,
};
use loco_rs::{hash, prelude::*};
use serde::{Deserialize, Serialize};
use serde_json::json;

/// What `connect` remembers until Berry redirects back with a code.
#[derive(Debug, Deserialize, Serialize)]
struct PendingAuth {
    user_pid: String,
    verifier: String,
}

#[derive(Debug, Deserialize)]
pub struct CallbackParams {
    pub code: String,
    pub state: String,
}

#[derive(Debug, Serialize)]
struct StatusResponse {
    connected: bool,
    tools: Vec<String>,
}

fn pending_key(state: &str) -> String {
    format!("berry_oauth:{state}")
}

/// Starts the OAuth flow: returns the Berry URL the app opens in the browser.
#[debug_handler]
async fn connect(auth: auth::JWT, State(ctx): State<AppContext>) -> Result<Response> {
    let settings = BerrySettings::from_context(&ctx)?;
    let state = hash::random_string(32);
    let verifier = hash::random_string(64);
    let challenge = pkce_challenge(&verifier);
    let pending = PendingAuth {
        user_pid: auth.claims.pid,
        verifier,
    };
    ctx.cache
        .insert_with_expiry(&pending_key(&state), &pending, Duration::from_secs(600))
        .await
        .map_err(Error::wrap)?;
    format::json(json!({ "authorize_url": settings.authorize_url(&state, &challenge)? }))
}

/// Finishes the OAuth flow with the `code` and `state` Berry sent to the app.
#[debug_handler]
async fn callback(
    auth: auth::JWT,
    State(ctx): State<AppContext>,
    Json(params): Json<CallbackParams>,
) -> Result<Response> {
    let key = pending_key(&params.state);
    let Some(pending) = ctx
        .cache
        .get::<PendingAuth>(&key)
        .await
        .map_err(Error::wrap)?
    else {
        return bad_request("invalid or expired state");
    };
    ctx.cache.remove(&key).await.map_err(Error::wrap)?;
    if pending.user_pid != auth.claims.pid {
        return unauthorized("state belongs to another user");
    }

    let user = users::Model::find_by_pid(&ctx.db, &auth.claims.pid).await?;
    let settings = BerrySettings::from_context(&ctx)?;
    let tokens = settings.exchange_code(&params.code, &pending.verifier).await?;
    let conn = berry_connections::Model::upsert(&ctx.db, user.id, tokens).await?;
    let tools = settings.list_tools(&conn.access_token).await?;
    format::json(StatusResponse {
        connected: true,
        tools,
    })
}

#[debug_handler]
async fn status(auth: auth::JWT, State(ctx): State<AppContext>) -> Result<Response> {
    let user = users::Model::find_by_pid(&ctx.db, &auth.claims.pid).await?;
    let Some(conn) = berry_connections::Model::find_by_user(&ctx.db, user.id).await? else {
        return format::json(StatusResponse {
            connected: false,
            tools: vec![],
        });
    };
    let settings = BerrySettings::from_context(&ctx)?;
    let token = conn.access_token(&ctx.db, &settings).await?;
    let tools = settings.list_tools(&token).await?;
    format::json(StatusResponse {
        connected: true,
        tools,
    })
}

pub fn routes() -> Routes {
    Routes::new()
        .prefix("/api/berry")
        .add("/connect", post(connect))
        .add("/callback", post(callback))
        .add("/status", get(status))
}
