//! Minimal client for Claude Managed Agents. There is no Anthropic Rust SDK, so
//! this speaks the REST API directly (beta `managed-agents-2026-04-01`).

use std::time::Duration;

use loco_rs::prelude::*;
use reqwest::{Method, RequestBuilder};
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};

const API_URL: &str = "https://api.anthropic.com/v1";
const BETA: &str = "managed-agents-2026-04-01";
/// Upper bound for one assistant turn, so an app request can't hang forever.
const TURN_TIMEOUT: Duration = Duration::from_secs(120);

/// `settings.anthropic` from `config/<env>.yaml`.
#[derive(Debug, Clone, Deserialize)]
pub struct AnthropicSettings {
    pub api_key: String,
    /// Empty until `cargo loco task ai_setup` has been run once.
    #[serde(default)]
    pub agent_id: String,
    #[serde(default)]
    pub environment_id: String,
    /// Hard spend cap for each session, in USD cents.
    pub session_budget_cents: u32,
}

/// A tool call the agent is waiting on the user to approve (e.g. `place_order`).
#[derive(Debug, Clone, Serialize, PartialEq)]
pub struct PendingAction {
    /// Session event id (`sevt_...`) to answer with `user.tool_confirmation`.
    pub id: String,
    pub tool: String,
    pub input: Value,
}

/// What the agent produced until it stopped and needs the user again.
#[derive(Debug, Default, Serialize, PartialEq)]
pub struct Turn {
    pub reply: String,
    pub pending_actions: Vec<PendingAction>,
    /// Berry rejected the user's credential during the turn (expired or revoked).
    #[serde(skip)]
    pub berry_auth_failed: bool,
}

pub struct ManagedAgents {
    http: reqwest::Client,
    pub settings: AnthropicSettings,
}

impl ManagedAgents {
    /// # Errors
    /// When `settings.anthropic` is missing or malformed.
    pub fn from_context(ctx: &AppContext) -> Result<Self> {
        let raw = ctx
            .config
            .settings
            .as_ref()
            .and_then(|s| s.get("anthropic"))
            .cloned()
            .ok_or_else(|| Error::string("missing `settings.anthropic` in config"))?;
        Ok(Self {
            http: reqwest::Client::new(),
            settings: serde_json::from_value(raw)?,
        })
    }

    fn request(&self, method: Method, path: &str) -> RequestBuilder {
        self.http
            .request(method, format!("{API_URL}{path}"))
            .header("x-api-key", &self.settings.api_key)
            .header("anthropic-version", "2023-06-01")
            .header("anthropic-beta", BETA)
    }

    async fn send(req: RequestBuilder) -> Result<Value> {
        let res = req.send().await.map_err(Error::wrap)?;
        let status = res.status();
        let body = res.text().await.map_err(Error::wrap)?;
        if !status.is_success() {
            return Err(Error::string(&format!("anthropic {status}: {body}")));
        }
        Ok(if body.is_empty() {
            Value::Null
        } else {
            serde_json::from_str(&body)?
        })
    }

    /// # Errors
    /// When the API rejects the request or is unreachable.
    pub async fn post(&self, path: &str, body: &Value) -> Result<Value> {
        Self::send(self.request(Method::POST, path).json(body)).await
    }

    /// # Errors
    /// When the API rejects the request or is unreachable.
    pub async fn get(&self, path: &str) -> Result<Value> {
        Self::send(self.request(Method::GET, path)).await
    }

    /// Id of a field in an API response, e.g. the `id` of a created object.
    ///
    /// # Errors
    /// When the field is absent.
    pub fn id(value: &Value) -> Result<String> {
        value["id"]
            .as_str()
            .map(String::from)
            .ok_or_else(|| Error::string(&format!("anthropic response without id: {value}")))
    }

    /// Creates a session for one user: their vault is the only Berry credential it can use.
    ///
    /// # Errors
    /// When the API rejects the request.
    pub async fn create_session(&self, vault_id: &str) -> Result<String> {
        let session = self
            .post(
                "/sessions",
                &json!({
                    "agent": self.settings.agent_id,
                    "environment_id": self.settings.environment_id,
                    "vault_ids": [vault_id],
                    "budget": {
                        "type": "limit",
                        "max_list_cost": {
                            "amount": self.settings.session_budget_cents.to_string(),
                            "currency": "USD"
                        }
                    }
                }),
            )
            .await?;
        Self::id(&session)
    }

    /// Whether the vault credential still works against the MCP server
    /// (Anthropic probes the server and refreshes the token if it can).
    ///
    /// # Errors
    /// When the API rejects the request or is unreachable.
    pub async fn credential_is_valid(&self, vault_id: &str, credential_id: &str) -> Result<bool> {
        let result = self
            .post(
                &format!("/vaults/{vault_id}/credentials/{credential_id}/mcp_oauth_validate"),
                &json!({}),
            )
            .await?;
        Ok(result["status"] == "valid")
    }

    /// Whether a session can still take events.
    ///
    /// # Errors
    /// When the API is unreachable.
    pub async fn session_is_usable(&self, session_id: &str) -> Result<bool> {
        match self.get(&format!("/sessions/{session_id}")).await {
            Ok(session) => {
                Ok(session["status"] != "terminated" && session["archived_at"].is_null())
            }
            // ponytail: any API error (404, archived) means "make a new one".
            Err(_) => Ok(false),
        }
    }

    /// Sends `events` and follows the session until it stops: either the turn
    /// ended, or the agent waits for the user to approve tool calls.
    ///
    /// # Errors
    /// When the session fails, terminates, or takes longer than [`TURN_TIMEOUT`].
    pub async fn run_turn(&self, session_id: &str, events: Value) -> Result<Turn> {
        // Open the stream before sending, or the first events can be missed.
        let mut stream = self
            .request(
                Method::GET,
                &format!("/sessions/{session_id}/events/stream"),
            )
            .send()
            .await
            .map_err(Error::wrap)?;
        if !stream.status().is_success() {
            let body = stream.text().await.unwrap_or_default();
            return Err(Error::string(&format!("anthropic stream: {body}")));
        }
        self.post(
            &format!("/sessions/{session_id}/events"),
            &json!({ "events": events }),
        )
        .await?;

        let mut turn = Turn::default();
        let mut asks = Vec::new();
        let mut buf: Vec<u8> = Vec::new();
        let deadline = tokio::time::Instant::now() + TURN_TIMEOUT;
        loop {
            let chunk = tokio::time::timeout_at(deadline, stream.chunk())
                .await
                .map_err(|_| Error::string("el asistente tardó demasiado en responder"))?
                .map_err(Error::wrap)?
                .ok_or_else(|| Error::string("anthropic stream closed mid-turn"))?;
            buf.extend(chunk.iter().filter(|b| **b != b'\r'));

            // SSE frames are separated by a blank line; the JSON is on `data:` lines.
            while let Some(end) = buf.windows(2).position(|w| w == b"\n\n") {
                let frame: Vec<u8> = buf.drain(..end + 2).collect();
                let frame = String::from_utf8_lossy(&frame);
                for data in frame.lines().filter_map(|l| l.strip_prefix("data:")) {
                    let event: Value = serde_json::from_str(data.trim())?;
                    if apply_event(&event, &mut turn, &mut asks)? {
                        return Ok(turn);
                    }
                }
            }
        }
    }
}

/// Folds one session event into the turn. Returns `true` when the turn is over.
fn apply_event(event: &Value, turn: &mut Turn, asks: &mut Vec<PendingAction>) -> Result<bool> {
    tracing::debug!(%event, "managed agents event");
    match event["type"].as_str().unwrap_or_default() {
        "agent.message" => {
            let text: Vec<&str> = event["content"]
                .as_array()
                .into_iter()
                .flatten()
                .filter_map(|block| block["text"].as_str())
                .collect();
            if !text.is_empty() {
                if !turn.reply.is_empty() {
                    turn.reply.push_str("\n\n");
                }
                turn.reply.push_str(&text.join(""));
            }
        }
        "agent.mcp_tool_use" | "agent.tool_use" if event["evaluated_permission"] == "ask" => {
            asks.push(PendingAction {
                id: event["id"].as_str().unwrap_or_default().to_string(),
                tool: event["name"].as_str().unwrap_or_default().to_string(),
                input: event["input"].clone(),
            });
        }
        "session.error" => {
            tracing::warn!(%event, "managed agents session error");
            if event["error"]["type"] == "mcp_authentication_failed_error" {
                turn.berry_auth_failed = true;
            }
        }
        "session.status_terminated" => {
            return Err(Error::string("la sesión del asistente terminó"));
        }
        "session.status_idle" => {
            let stop = &event["stop_reason"];
            match stop["type"].as_str().unwrap_or_default() {
                "requires_action" => {
                    let waiting: Vec<&str> = stop["event_ids"]
                        .as_array()
                        .into_iter()
                        .flatten()
                        .filter_map(Value::as_str)
                        .collect();
                    let matching: Vec<PendingAction> = asks
                        .iter()
                        .filter(|a| waiting.contains(&a.id.as_str()))
                        .cloned()
                        .collect();
                    // Never leave the user without a card for a session that waits on them.
                    turn.pending_actions = if matching.is_empty() {
                        std::mem::take(asks)
                    } else {
                        matching
                    };
                }
                "budget_reached" => {
                    return Err(Error::string(
                        "se alcanzó el límite de gasto de esta conversación",
                    ))
                }
                "retries_exhausted" => {
                    return Err(Error::string("el asistente no pudo completar la respuesta"))
                }
                _ => {}
            }
            return Ok(true);
        }
        _ => {}
    }
    Ok(false)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn turn_collects_text_and_only_the_asks_the_session_waits_on() {
        let events = [
            json!({"type": "agent.message", "content": [{"type": "text", "text": "Preparo la orden."}]}),
            json!({"type": "agent.mcp_tool_use", "id": "sevt_read", "name": "get_asset_data",
                   "evaluated_permission": "allow", "input": {"symbol": "KO"}}),
            json!({"type": "agent.mcp_tool_use", "id": "sevt_buy", "name": "place_order",
                   "evaluated_permission": "ask", "input": {"symbol": "KO", "amount": 1}}),
            json!({"type": "session.status_idle",
                   "stop_reason": {"type": "requires_action", "event_ids": ["sevt_buy"]}}),
        ];
        let (mut turn, mut asks) = (Turn::default(), Vec::new());
        let done: Vec<bool> = events
            .iter()
            .map(|e| apply_event(e, &mut turn, &mut asks).unwrap())
            .collect();

        assert_eq!(done, [false, false, false, true]);
        assert_eq!(turn.reply, "Preparo la orden.");
        assert_eq!(
            turn.pending_actions,
            [PendingAction {
                id: "sevt_buy".into(),
                tool: "place_order".into(),
                input: json!({"symbol": "KO", "amount": 1}),
            }]
        );
    }

    #[test]
    fn budget_reached_is_an_error() {
        let idle =
            json!({"type": "session.status_idle", "stop_reason": {"type": "budget_reached"}});
        assert!(apply_event(&idle, &mut Turn::default(), &mut Vec::new()).is_err());
    }

    #[test]
    fn berry_auth_failure_is_flagged() {
        let error = json!({"type": "session.error",
                           "error": {"type": "mcp_authentication_failed_error", "mcp_server_name": "berry"}});
        let mut turn = Turn::default();
        assert!(!apply_event(&error, &mut turn, &mut Vec::new()).unwrap());
        assert!(turn.berry_auth_failed);
    }
}
