use backend::{app::App, models::strategies};
use loco_rs::testing::prelude::*;
use serial_test::serial;

use super::prepare_data;

fn nvda() -> strategies::NewStrategy {
    strategies::NewStrategy {
        ticker: "nvda".into(),
        name: "NVIDIA".into(),
        condition: "drop_pct".into(),
        threshold: 10.0,
        reference_price: 180.0,
        side: "buy".into(),
        amount_usd: 100.0,
        title: "Comprar NVDA si baja 10 %".into(),
    }
}

#[tokio::test]
#[serial]
async fn lists_pauses_and_deletes_the_users_strategies() {
    request::<App, _, _>(|request, ctx| async move {
        let user = prepare_data::init_user_login(&request, &ctx).await;
        let (name, value) = prepare_data::auth_header(&user.token);
        let created = strategies::Model::create(&ctx.db, user.user.id, &nvda())
            .await
            .unwrap();
        assert_eq!(created.ticker, "NVDA");
        assert_eq!(created.status, strategies::ACTIVE);

        let list = request
            .get("/api/strategies")
            .add_header(name.clone(), value.clone())
            .await;
        assert_eq!(list.json::<serde_json::Value>().as_array().unwrap().len(), 1);

        let paused = request
            .patch(&format!("/api/strategies/{}", created.id))
            .add_header(name.clone(), value.clone())
            .json(&serde_json::json!({ "status": "paused" }))
            .await;
        assert_eq!(paused.json::<serde_json::Value>()["status"], "paused");

        let bad = request
            .patch(&format!("/api/strategies/{}", created.id))
            .add_header(name.clone(), value.clone())
            .json(&serde_json::json!({ "status": "done" }))
            .await;
        assert_eq!(bad.status_code(), 400);

        let deleted = request
            .delete(&format!("/api/strategies/{}", created.id))
            .add_header(name.clone(), value.clone())
            .await;
        assert_eq!(deleted.status_code(), 200);
        let missing = request
            .delete(&format!("/api/strategies/{}", created.id))
            .add_header(name, value)
            .await;
        assert_eq!(missing.status_code(), 404);
    })
    .await;
}

#[tokio::test]
#[serial]
async fn requires_login() {
    request::<App, _, _>(|request, _ctx| async move {
        assert_eq!(request.get("/api/strategies").await.status_code(), 401);
    })
    .await;
}
