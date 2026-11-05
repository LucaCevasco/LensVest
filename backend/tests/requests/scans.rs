use backend::app::App;
use loco_rs::testing::prelude::*;
use serial_test::serial;

use super::prepare_data;

#[tokio::test]
#[serial]
async fn records_scans_and_favorites() {
    request::<App, _, _>(|request, ctx| async move {
        let user = prepare_data::init_user_login(&request, &ctx).await;
        let (name, value) = prepare_data::auth_header(&user.token);

        let scan = request
            .post("/api/scans")
            .add_header(name.clone(), value.clone())
            .json(&serde_json::json!({ "brand": "Sprite", "company": "The Coca-Cola Company", "ticker": "ko" }))
            .await
            .json::<serde_json::Value>();
        assert_eq!(scan["ticker"], "KO");
        assert_eq!(scan["favorite"], false);

        let favorite = request
            .post(&format!("/api/scans/{}/favorite", scan["id"]))
            .add_header(name.clone(), value.clone())
            .json(&serde_json::json!({ "favorite": true }))
            .await
            .json::<serde_json::Value>();
        assert_eq!(favorite["favorite"], true);

        let list = request
            .get("/api/scans")
            .add_header(name.clone(), value.clone())
            .await
            .json::<serde_json::Value>();
        assert_eq!(list[0]["brand"], "Sprite");

        let empty = request
            .post("/api/scans")
            .add_header(name, value)
            .json(&serde_json::json!({ "brand": " " }))
            .await;
        assert_eq!(empty.status_code(), 400);
    })
    .await;
}
