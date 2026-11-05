//! Lens Mode history: the brands the user confirmed, and their favorites.

use loco_rs::prelude::*;
use serde::Deserialize;

use crate::models::{scans, users};

#[derive(Debug, Deserialize)]
pub struct NewScan {
    pub brand: String,
    pub company: Option<String>,
    pub ticker: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct FavoriteParams {
    pub favorite: bool,
}

#[debug_handler]
async fn list(auth: auth::JWT, State(ctx): State<AppContext>) -> Result<Response> {
    let user = users::Model::find_by_pid(&ctx.db, &auth.claims.pid).await?;
    format::json(scans::Model::list_for(&ctx.db, user.id).await?)
}

#[debug_handler]
async fn create(
    auth: auth::JWT,
    State(ctx): State<AppContext>,
    Json(params): Json<NewScan>,
) -> Result<Response> {
    let brand = params.brand.trim();
    if brand.is_empty() || brand.len() > 100 {
        return bad_request("Falta la marca");
    }
    let user = users::Model::find_by_pid(&ctx.db, &auth.claims.pid).await?;
    let scan = scans::ActiveModel {
        user_id: Set(user.id),
        brand: Set(brand.to_string()),
        company: Set(params.company),
        ticker: Set(params.ticker.map(|t| t.trim().to_uppercase())),
        favorite: Set(false),
        ..Default::default()
    }
    .insert(&ctx.db)
    .await?;
    format::json(scan)
}

#[debug_handler]
async fn favorite(
    auth: auth::JWT,
    State(ctx): State<AppContext>,
    Path(id): Path<i64>,
    Json(params): Json<FavoriteParams>,
) -> Result<Response> {
    let user = users::Model::find_by_pid(&ctx.db, &auth.claims.pid).await?;
    let mut scan = scans::Model::find_for(&ctx.db, user.id, id)
        .await?
        .into_active_model();
    scan.favorite = Set(params.favorite);
    format::json(scan.update(&ctx.db).await?)
}

pub fn routes() -> Routes {
    Routes::new()
        .prefix("/api/scans")
        .add("/", get(list))
        .add("/", post(create))
        .add("/{id}/favorite", post(favorite))
}
