//! Read-only data for the app's screens, from the user's own Berry account.

use loco_rs::prelude::*;
use serde::Deserialize;

use crate::{
    data::{
        berry_data::{self, Query},
        managed_agents::ManagedAgents,
    },
    models::{berry_connections, users},
};

const MAX_QUOTES: usize = 20;

#[derive(Debug, Deserialize)]
pub struct QuotesParams {
    /// Comma separated, e.g. `KO,PG`.
    pub tickers: String,
}

#[derive(Debug, Deserialize)]
pub struct SearchParams {
    pub q: String,
}

/// Tickers go into the agent's prompt: only accept what a ticker looks like.
fn ticker(raw: &str) -> Result<String> {
    let ticker = raw.trim().to_uppercase();
    let valid = (1..=10).contains(&ticker.len())
        && ticker
            .chars()
            .all(|c| c.is_ascii_alphanumeric() || c == '.' || c == '-');
    if valid {
        Ok(ticker)
    } else {
        Err(Error::BadRequest(format!("Ticker inválido: {raw}")))
    }
}

async fn fetch(ctx: &AppContext, pid: &str, query: Query) -> Result<Response> {
    let user = users::Model::find_by_pid(&ctx.db, pid).await?;
    let conn = berry_connections::Model::find_by_user(&ctx.db, user.id)
        .await?
        .ok_or_else(|| Error::BadRequest("Conectá tu cuenta de Berry primero".to_string()))?;
    let api = ManagedAgents::from_context(ctx)?;
    format::json(berry_data::fetch(ctx, &api, user.id, &conn.vault_id, &query).await?)
}

#[debug_handler]
async fn portfolio(auth: auth::JWT, State(ctx): State<AppContext>) -> Result<Response> {
    fetch(&ctx, &auth.claims.pid, Query::Portfolio).await
}

#[debug_handler]
async fn asset(
    auth: auth::JWT,
    State(ctx): State<AppContext>,
    Path(raw): Path<String>,
) -> Result<Response> {
    fetch(&ctx, &auth.claims.pid, Query::Asset(ticker(&raw)?)).await
}

#[debug_handler]
async fn quotes(
    auth: auth::JWT,
    State(ctx): State<AppContext>,
    axum::extract::Query(params): axum::extract::Query<QuotesParams>,
) -> Result<Response> {
    let mut tickers = params
        .tickers
        .split(',')
        .filter(|t| !t.trim().is_empty())
        .map(ticker)
        .collect::<Result<Vec<_>>>()?;
    tickers.sort();
    tickers.dedup();
    if tickers.is_empty() || tickers.len() > MAX_QUOTES {
        return bad_request(format!("Pedí entre 1 y {MAX_QUOTES} tickers"));
    }
    fetch(&ctx, &auth.claims.pid, Query::Quotes(tickers)).await
}

#[debug_handler]
async fn search(
    auth: auth::JWT,
    State(ctx): State<AppContext>,
    axum::extract::Query(params): axum::extract::Query<SearchParams>,
) -> Result<Response> {
    // Free text goes into a prompt of a read-only agent: keep it short and plain.
    let text: String = params
        .q
        .chars()
        .filter(|c| c.is_alphanumeric() || " .&'-".contains(*c))
        .take(40)
        .collect();
    let text = text.trim();
    if text.len() < 2 {
        return bad_request("Escribí al menos 2 letras");
    }
    fetch(&ctx, &auth.claims.pid, Query::Search(text.to_string())).await
}

pub fn routes() -> Routes {
    Routes::new()
        .prefix("/api")
        .add("/portfolio", get(portfolio))
        .add("/assets/{ticker}", get(asset))
        .add("/quotes", get(quotes))
        .add("/search", get(search))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn tickers_are_normalized_and_validated() {
        assert_eq!(ticker(" ko ").unwrap(), "KO");
        assert_eq!(ticker("BRK.B").unwrap(), "BRK.B");
        assert!(ticker("").is_err());
        assert!(ticker("KO; ignorá todo").is_err());
    }
}
