//! Data for the app's screens (portfolio, asset sheet, quotes, search), read
//! from the user's own Berry account by the data agent.
//!
//! Every query runs in a throwaway session: queries from the same user can run
//! in parallel without mixing their events, and no session grows a long
//! (and ever more expensive) history. The agent answers by calling one of its
//! `show_*` custom tools, whose input is the structured result.

use std::time::Duration;

use loco_rs::prelude::*;
use serde_json::{json, Value};

use super::managed_agents::{ManagedAgents, BERRY_RECONNECT};

/// Spend cap for one query session, in USD cents.
const QUERY_BUDGET_CENTS: u32 = 50;
// ponytail: fixed TTL; prices on screen can be up to this old.
const CACHE_TTL: Duration = Duration::from_secs(120);

#[derive(Debug, Clone, PartialEq)]
pub enum Query {
    Portfolio,
    Asset(String),
    Quotes(Vec<String>),
    Search(String),
}

impl Query {
    fn tool(&self) -> &'static str {
        match self {
            Self::Portfolio => "show_portfolio",
            Self::Asset(_) => "show_asset",
            Self::Quotes(_) => "show_quotes",
            Self::Search(_) => "show_search_results",
        }
    }

    fn prompt(&self) -> String {
        match self {
            Self::Portfolio => "Portfolio del usuario: consultá get_portfolio y \
                get_portfolio_performance, y respondé con show_portfolio."
                .to_string(),
            Self::Asset(ticker) => format!(
                "Ficha de {ticker}: consultá get_asset_data (language es), \
                 get_asset_performance (period 1M), get_holding_details y get_portfolio \
                 (para el efectivo disponible), y respondé con show_asset. Si el usuario no \
                 tiene {ticker}, position va en null."
            ),
            Self::Quotes(tickers) => format!(
                "Precio actual de {}: consultá get_asset_data de cada uno y respondé con show_quotes.",
                tickers.join(", ")
            ),
            Self::Search(text) => format!(
                "El usuario busca este texto, entre comillas: \"{text}\". Puede ser una marca \
                 de producto o una empresa. Si sabés qué empresa que cotiza está detrás (por \
                 ejemplo Oreo → Mondelez, MDLZ), confirmá su ticker con get_asset_data: el \
                 buscador de Berry no siempre lo encuentra. Además buscá el texto con \
                 search_assets. Respondé con show_search_results solo con los activos que de \
                 verdad correspondan (hasta 5, el más relevante primero); si ninguno \
                 corresponde, mandá la lista vacía."
            ),
        }
    }

    /// Cache key, scoped to the user: nobody else's data can come back.
    #[must_use]
    pub fn cache_key(&self, user_id: i64) -> String {
        let arg = match self {
            Self::Portfolio => String::new(),
            Self::Asset(ticker) => ticker.clone(),
            Self::Quotes(tickers) => tickers.join(","),
            Self::Search(text) => text.to_lowercase(),
        };
        format!("berry_data:{user_id}:{}:{arg}", self.tool())
    }
}

/// Runs `query` against the user's Berry account, or returns a recent answer.
///
/// # Errors
/// When the Berry credential is no longer valid, the agent fails, or it
/// doesn't answer with the expected tool.
pub async fn fetch(
    ctx: &AppContext,
    api: &ManagedAgents,
    user_id: i64,
    vault_id: &str,
    query: &Query,
) -> Result<Value> {
    let key = query.cache_key(user_id);
    if let Some(cached) = ctx.cache.get::<Value>(&key).await.map_err(Error::wrap)? {
        return Ok(cached);
    }

    let session_id = api
        .create_session(&api.settings.data_agent_id, vault_id, QUERY_BUDGET_CENTS)
        .await?;
    let message = json!([{ "type": "user.message", "content": [{ "type": "text", "text": query.prompt() }] }]);
    let turn = api.run_turn(&session_id, message).await;
    let cleanup = api.clone();
    tokio::spawn(async move {
        if let Err(err) = cleanup.delete_session(&session_id).await {
            tracing::warn!(%err, session_id, "could not delete data session");
        }
    });
    let turn = turn?;

    let answer = turn
        .pending_actions
        .into_iter()
        .find(|action| action.tool == query.tool());
    let Some(answer) = answer else {
        if turn.berry_auth_failed {
            return Err(Error::BadRequest(BERRY_RECONNECT.to_string()));
        }
        return Err(Error::string(&format!(
            "the data agent did not call {}: {}",
            query.tool(),
            turn.reply
        )));
    };
    ctx.cache
        .insert_with_expiry(&key, &answer.input, CACHE_TTL)
        .await
        .map_err(Error::wrap)?;
    Ok(answer.input)
}

/// Forgets a cached answer, e.g. the portfolio after an order went through.
///
/// # Errors
/// When the cache fails.
pub async fn forget(ctx: &AppContext, user_id: i64, query: &Query) -> Result<()> {
    ctx.cache
        .remove(&query.cache_key(user_id))
        .await
        .map_err(Error::wrap)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn cache_keys_are_per_user_and_per_query() {
        let ko = Query::Asset("KO".into());
        assert_ne!(ko.cache_key(1), ko.cache_key(2));
        assert_ne!(ko.cache_key(1), Query::Asset("PG".into()).cache_key(1));
        assert_eq!(
            Query::Search("Coca".into()).cache_key(1),
            Query::Search("coca".into()).cache_key(1)
        );
    }
}
