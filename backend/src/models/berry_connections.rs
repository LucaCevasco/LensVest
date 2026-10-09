use base64::{engine::general_purpose::URL_SAFE_NO_PAD, Engine};
use chrono::{Duration, SecondsFormat, Utc};
use loco_rs::prelude::*;
use serde::Deserialize;
use serde_json::{json, Value};
use sha2::{Digest, Sha256};

pub use super::_entities::berry_connections::{self, ActiveModel, Entity, Model};
use crate::data::managed_agents::ManagedAgents;
pub type BerryConnections = Entity;

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

    /// Exchanges the authorization code for tokens. This is the only call the
    /// backend makes to Berry's token endpoint: refreshing belongs to the vault.
    ///
    /// # Errors
    /// When Berry rejects the code or is unreachable.
    pub async fn exchange_code(&self, code: &str, verifier: &str) -> Result<TokenResponse> {
        let res = reqwest::Client::new()
            .post(&self.token_url)
            .form(&[
                ("grant_type", "authorization_code"),
                ("code", code),
                ("redirect_uri", &self.redirect_uri),
                ("client_id", &self.client_id),
                ("code_verifier", verifier),
                ("resource", &self.mcp_url),
            ])
            .send()
            .await
            .map_err(Error::wrap)?;
        if !res.status().is_success() {
            let body = res.text().await.unwrap_or_default();
            return Err(Error::BadRequest(format!("berry token endpoint: {body}")));
        }
        res.json().await.map_err(Error::wrap)
    }

    /// Vault credential that lets the agent call Berry's MCP server as this user.
    /// With a refresh token, Anthropic refreshes it on its own from then on.
    #[must_use]
    pub fn mcp_oauth_credential(&self, tokens: &TokenResponse) -> Value {
        let expires_at = (Utc::now() + Duration::seconds(tokens.expires_in))
            .to_rfc3339_opts(SecondsFormat::Secs, true);
        let mut auth = json!({
            "type": "mcp_oauth",
            "mcp_server_url": self.mcp_url,
            "access_token": tokens.access_token,
            "expires_at": expires_at,
        });
        if let Some(refresh_token) = &tokens.refresh_token {
            auth["refresh"] = json!({
                "refresh_token": refresh_token,
                "client_id": self.client_id,
                "token_endpoint": self.token_url,
                "token_endpoint_auth": { "type": "none" },
            });
        }
        json!({ "display_name": "Berry", "auth": auth })
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

    /// Hands freshly issued Berry tokens to the user's own vault (created on
    /// first connect). Tokens are never stored in our database.
    ///
    /// # Errors
    /// When the Anthropic API or the database fails.
    pub async fn connect(
        db: &DatabaseConnection,
        api: &ManagedAgents,
        berry: &BerrySettings,
        user: &super::users::Model,
        tokens: &TokenResponse,
    ) -> Result<Self> {
        let existing = Self::find_by_user(db, user.id).await?;
        let vault_id = match &existing {
            Some(conn) => {
                // Credentials are keyed by MCP URL: free the key for the new tokens.
                // ponytail: ignore the error, the credential may already be archived.
                let _ = api
                    .post(
                        &format!(
                            "/vaults/{}/credentials/{}/archive",
                            conn.vault_id, conn.credential_id
                        ),
                        &json!({}),
                    )
                    .await;
                conn.vault_id.clone()
            }
            None => {
                let vault = api
                    .post(
                        "/vaults",
                        &json!({ "display_name": format!("LensVest user {}", user.pid) }),
                    )
                    .await?;
                ManagedAgents::id(&vault)?
            }
        };
        let credential = api
            .post(
                &format!("/vaults/{vault_id}/credentials"),
                &berry.mcp_oauth_credential(tokens),
            )
            .await?;
        let credential_id = ManagedAgents::id(&credential)?;

        Ok(match existing {
            Some(conn) => {
                let mut conn = conn.into_active_model();
                conn.credential_id = Set(credential_id);
                conn.update(db).await?
            }
            None => {
                ActiveModel {
                    user_id: Set(user.id),
                    vault_id: Set(vault_id),
                    credential_id: Set(credential_id),
                    ..Default::default()
                }
                .insert(db)
                .await?
            }
        })
    }

    /// The user's assistant session, created on first use or after it ended.
    ///
    /// # Errors
    /// When the Anthropic API or the database fails.
    pub async fn session(self, db: &DatabaseConnection, api: &ManagedAgents) -> Result<String> {
        if let Some(session_id) = &self.session_id {
            if api.session_is_usable(session_id).await? {
                return Ok(session_id.clone());
            }
        }
        let session_id = api.create_session(&self.vault_id).await?;
        let mut conn = self.into_active_model();
        conn.session_id = Set(Some(session_id.clone()));
        conn.update(db).await?;
        Ok(session_id)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn settings() -> BerrySettings {
        BerrySettings {
            mcp_url: "https://connect.berry.app/mcp".into(),
            authorize_url: "https://auth.example/oauth/authorize".into(),
            token_url: "https://auth.example/oauth/token".into(),
            client_id: "cid".into(),
            redirect_uri: "lensvest://berry/callback".into(),
            scope: "openid offline_access".into(),
        }
    }

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
        let url = settings().authorize_url("st", "ch").unwrap();
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

    #[test]
    fn credential_lets_the_vault_refresh_only_with_a_refresh_token() {
        let mut tokens = TokenResponse {
            access_token: "at".into(),
            refresh_token: Some("rt".into()),
            expires_in: 3600,
        };
        let cred = settings().mcp_oauth_credential(&tokens);
        assert_eq!(
            cred["auth"]["mcp_server_url"],
            "https://connect.berry.app/mcp"
        );
        assert_eq!(
            cred["auth"]["refresh"]["token_endpoint"],
            "https://auth.example/oauth/token"
        );
        assert_eq!(
            cred["auth"]["refresh"]["token_endpoint_auth"]["type"],
            "none"
        );

        tokens.refresh_token = None;
        assert!(settings().mcp_oauth_credential(&tokens)["auth"]
            .get("refresh")
            .is_none());
    }
}
