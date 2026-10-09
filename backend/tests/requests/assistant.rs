use backend::app::App;
use loco_rs::testing::prelude::*;
use serial_test::serial;

use super::prepare_data;

#[tokio::test]
#[serial]
async fn assistant_requires_login() {
    request::<App, _, _>(|request, _ctx| async move {
        let res = request
            .post("/api/assistant/messages")
            .json(&serde_json::json!({ "text": "hola" }))
            .await;
        assert_eq!(res.status_code(), 401);
    })
    .await;
}

#[tokio::test]
#[serial]
async fn assistant_asks_to_connect_berry_first() {
    request::<App, _, _>(|request, ctx| async move {
        let user = prepare_data::init_user_login(&request, &ctx).await;
        let (name, value) = prepare_data::auth_header(&user.token);
        let res = request
            .post("/api/assistant/messages")
            .add_header(name, value)
            .json(&serde_json::json!({ "text": "¿Qué tengo en mi portfolio?" }))
            .await;
        assert_eq!(res.status_code(), 400);
        assert!(res.text().contains("Berry"));
    })
    .await;
}
