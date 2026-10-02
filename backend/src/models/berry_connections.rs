use base64::{engine::general_purpose::URL_SAFE_NO_PAD, Engine};
use chrono::{Duration, Utc};
use loco_rs::prelude::*;
use serde::Deserialize;
use serde_json::{json, Value};
use sha2::{Digest, Sha256};

pub use super::_entities::berry_connections::{self, ActiveModel, Entity, Model};
pub type BerryConnections = Entity;

const MCP_PROTOCOL_VERSION: &str = "2025-06-18";

#[async_trait::async_trait]
impl ActiveModelBehavior for ActiveModel {
    async fn before_save<C>(self, _db: &C, insert: bool) -> std::result::Result<Self, DbErr>
    where
        C: ConnectionTrait,
    {
        if !insert && self.updated_at.is_unchanged() {
            let mut this = self;
            this.updated_at = sea_orm::ActiveValue::Set(chrono::Utc::now().into());
            Ok(this)
        } else {
            Ok(self)
        }
    }
}

/// `settings.berry` from `config/<env>.yaml`.
#[derive(Debug, Clone, Deserialize)]
pub struct BerrySettings {
    pub mcp_url: String,
    pub authorize_url: String,
    pub token_url: String,
    pub client_id: String,
    pub redirect_uri: String,
    pub scope: String,
}

#[derive(Debug, Deserialize)]
pub struct TokenResponse {
    pub access_token: String,
    pub refresh_token: Option<String>,
    pub expires_in: i64,
}

/// PKCE S256 challenge for a code verifier (RFC 7636).
#[must_use]
pub fn pkce_challenge(verifier: &str) -> String {
    URL_SAFE_NO_PAD.encode(Sha256::digest(verifier.as_bytes()))
}

impl BerrySettings {
    /// # Errors
    /// When `settings.berry` is missing or malformed.
    pub fn from_context(ctx: &AppContext) -> Result<Self> {
        let berry = ctx
            .config
            .settings
            .as_ref()
            .and_then(|s| s.get("berry"))
            .cloned()
            .ok_or_else(|| Error::string("missing `settings.berry` in config"))?;
        Ok(serde_json::from_value(berry)?)
    }

    /// # Errors
    /// When `authorize_url` in config is not a valid URL.
    pub fn authorize_url(&self, state: &str, challenge: &str) -> Result<String> {
        let url = reqwest::Url::parse_with_params(
            &self.authorize_url,
            &[
                ("response_type", "code"),
                ("client_id", self.client_id.as_str()),
                ("redirect_uri", self.redirect_uri.as_str()),
                ("scope", self.scope.as_str()),
                ("state", state),
                ("code_challenge", challenge),
                ("code_challenge_method", "S256"),
                ("resource", self.mcp_url.as_str()),
            ],
        )
        .map_err(Error::wrap)?;
        Ok(url.into())
    }

    /// # Errors
    /// When Berry rejects the code or is unreachable.
    pub async fn exchange_code(&self, code: &str, verifier: &str) -> Result<TokenResponse> {
        self.token_request(&[
            ("grant_type", "authorization_code"),
            ("code", code),
            ("redirect_uri", &self.redirect_uri),
            ("client_id", &self.client_id),
            ("code_verifier", verifier),
            ("resource", &self.mcp_url),
        ])
        .await
    }

    async fn token_request(&self, form: &[(&str, &str)]) -> Result<TokenResponse> {
        let res = reqwest::Client::new()
            .post(&self.token_url)
            .form(form)
            .send()
            .await
            .map_err(Error::wrap)?;
        if !res.status().is_success() {
            let body = res.text().await.unwrap_or_default();
            return Err(Error::BadRequest(format!("berry token endpoint: {body}")));
        }
        res.json().await.map_err(Error::wrap)
    }

    /// Names of the tools the Berry MCP server exposes to this token.
    ///
    /// # Errors
    /// When the token is rejected or the server is unreachable.
    pub async fn list_tools(&self, token: &str) -> Result<Vec<String>> {
        let client = reqwest::Client::new();
        let init = json!({
            "jsonrpc": "2.0", "id": 1, "method": "initialize",
            "params": {
                "protocolVersion": MCP_PROTOCOL_VERSION,
                "capabilities": {},
                "clientInfo": { "name": "lensvest", "version": env!("CARGO_PKG_VERSION") }
            }
        });
        let (_, session) = self.mcp_post(&client, token, None, init).await?;
        let session = session.as_deref();
        let initialized = json!({ "jsonrpc": "2.0", "method": "notifications/initialized" });
        self.mcp_post(&client, token, session, initialized).await?;
        let list = json!({ "jsonrpc": "2.0", "id": 2, "method": "tools/list" });
        let (res, _) = self.mcp_post(&client, token, session, list).await?;

        Ok(res["result"]["tools"]
            .as_array()
            .map(|tools| {
                tools
                    .iter()
                    .filter_map(|t| t["name"].as_str().map(String::from))
                    .collect()
            })
            .unwrap_or_default())
    }

    // ponytail: hand-rolled MCP streamable-HTTP client (one request → one response).
    // Switch to the `rmcp` crate once we call tools or need server notifications.
    async fn mcp_post(
        &self,
        client: &reqwest::Client,
        token: &str,
        session: Option<&str>,
        body: Value,
    ) -> Result<(Value, Option<String>)> {
        let mut req = client
            .post(&self.mcp_url)
            .bearer_auth(token)
            .header("accept", "application/json, text/event-stream")
            .header("mcp-protocol-version", MCP_PROTOCOL_VERSION)
            .json(&body);
        if let Some(session) = session {
            req = req.header("mcp-session-id", session);
        }
        let res = req.send().await.map_err(Error::wrap)?;
        if res.status() == reqwest::StatusCode::UNAUTHORIZED {
            return Err(Error::Unauthorized("berry rejected the access token".to_string()));
        }
        if !res.status().is_success() {
            let body = res.text().await.unwrap_or_default();
            return Err(Error::string(&format!("berry mcp: {body}")));
        }
        let session = res
            .headers()
            .get("mcp-session-id")
            .and_then(|v| v.to_str().ok())
            .map(String::from);
        let text = res.text().await.map_err(Error::wrap)?;
        // An SSE response carries the JSON-RPC message on its `data:` line.
        let payload = text
            .lines()
            .filter_map(|l| l.strip_prefix("data:"))
            .last()
            .unwrap_or(&text)
            .trim();
        let value: Value = if payload.is_empty() {
            Value::Null
        } else {
            serde_json::from_str(payload)?
        };
        if let Some(err) = value.get("error") {
            return Err(Error::string(&format!("berry mcp: {err}")));
        }
        Ok((value, session))
    }
}

impl Model {
    /// # Errors
    /// When the query fails.
    pub async fn find_by_user(db: &DatabaseConnection, user_id: i64) -> Result<Option<Self>> {
        Ok(Entity::find()
            .filter(berry_connections::Column::UserId.eq(user_id))
            .one(db)
            .await?)
    }

    /// Stores fresh tokens for the user, creating the connection on first use.
    ///
    /// # Errors
    /// When the query fails.
    // ponytail: tokens stored in plain text; encrypt with a config key before production.
    pub async fn upsert(db: &DatabaseConnection, user_id: i64, tok: TokenResponse) -> Result<Self> {
        let existing = Self::find_by_user(db, user_id).await?;
        let is_new = existing.is_none();
        let mut conn = existing.map_or_else(
            || ActiveModel {
                user_id: Set(user_id),
                ..Default::default()
            },
            IntoActiveModel::into_active_model,
        );
        conn.access_token = Set(tok.access_token);
        // Keep the previous refresh token when the server does not rotate it.
        if tok.refresh_token.is_some() {
            conn.refresh_token = Set(tok.refresh_token);
        }
        conn.expires_at = Set((Utc::now() + Duration::seconds(tok.expires_in)).into());
        Ok(if is_new {
            conn.insert(db).await?
        } else {
            conn.update(db).await?
        })
    }

    /// A non-expired access token, refreshing it when it is about to expire.
    ///
    /// # Errors
    /// When the session cannot be refreshed and the user must reconnect.
    pub async fn access_token(self, db: &DatabaseConnection, settings: &BerrySettings) -> Result<String> {
        if self.expires_at > Utc::now() + Duration::seconds(60) {
            return Ok(self.access_token);
        }
        let Some(refresh_token) = self.refresh_token.as_deref() else {
            return Err(Error::Unauthorized("berry session expired, reconnect".to_string()));
        };
        let tok = settings
            .token_request(&[
                ("grant_type", "refresh_token"),
                ("refresh_token", refresh_token),
                ("client_id", &settings.client_id),
            ])
            .await?;
        Ok(Self::upsert(db, self.user_id, tok).await?.access_token)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn pkce_challenge_matches_reference() {
        // Reference computed with Python's hashlib + base64.urlsafe_b64encode.
        assert_eq!(
            pkce_challenge("lensvest-pkce-test-verifier-0123456789abcdef"),
            "ohBBFDwdsEdOyIgfv77IRSygA1-8vDHgkLFrBivkE4U"
        );
    }

    #[test]
    fn authorize_url_carries_pkce_and_state() {
        let s = BerrySettings {
            mcp_url: "https://connect.berry.app/mcp".into(),
            authorize_url: "https://auth.example/oauth/authorize".into(),
            token_url: "https://auth.example/oauth/token".into(),
            client_id: "cid".into(),
            redirect_uri: "lensvest://berry/callback".into(),
            scope: "openid offline_access".into(),
        };
        let url = s.authorize_url("st", "ch").unwrap();
        for part in [
            "client_id=cid",
            "state=st",
            "code_challenge=ch",
            "code_challenge_method=S256",
            "redirect_uri=lensvest%3A%2F%2Fberry%2Fcallback",
        ] {
            assert!(url.contains(part), "{url} missing {part}");
        }
    }
}
