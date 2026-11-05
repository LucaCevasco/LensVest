//! The user's conditional strategies (RF04). They're created from the
//! assistant (`propose_strategy` + confirm); here they're listed, paused,
//! resumed and deleted.

use loco_rs::prelude::*;
use serde::Deserialize;

use crate::models::{
    strategies::{self, ACTIVE, PAUSED},
    users,
};

#[derive(Debug, Deserialize)]
pub struct UpdateParams {
    pub status: String,
}

#[debug_handler]
async fn list(auth: auth::JWT, State(ctx): State<AppContext>) -> Result<Response> {
    let user = users::Model::find_by_pid(&ctx.db, &auth.claims.pid).await?;
    format::json(strategies::Model::list_for(&ctx.db, user.id).await?)
}

#[debug_handler]
async fn update(
    auth: auth::JWT,
    State(ctx): State<AppContext>,
    Path(id): Path<i64>,
    Json(params): Json<UpdateParams>,
) -> Result<Response> {
    if ![ACTIVE, PAUSED].contains(&params.status.as_str()) {
        return bad_request("El estado tiene que ser active o paused");
    }
    let user = users::Model::find_by_pid(&ctx.db, &auth.claims.pid).await?;
    let mut strategy = strategies::Model::find_for(&ctx.db, user.id, id)
        .await?
        .into_active_model();
    strategy.status = Set(params.status);
    format::json(strategy.update(&ctx.db).await?)
}

#[debug_handler]
async fn remove(
    auth: auth::JWT,
    State(ctx): State<AppContext>,
    Path(id): Path<i64>,
) -> Result<Response> {
    let user = users::Model::find_by_pid(&ctx.db, &auth.claims.pid).await?;
    strategies::Model::find_for(&ctx.db, user.id, id)
        .await?
        .delete(&ctx.db)
        .await?;
    format::empty()
}

pub fn routes() -> Routes {
    Routes::new()
        .prefix("/api/strategies")
        .add("/", get(list))
        .add("/{id}", patch(update))
        .add("/{id}", delete(remove))
}
