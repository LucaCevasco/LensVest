use backend::app::App;
use loco_rs::testing::prelude::*;
use serial_test::serial;

use super::prepare_data;

#[tokio::test]
#[serial]
async fn recognize_requires_login() {
    request::<App, _, _>(|request, _ctx| async move {
        let res = request
            .post("/api/lens/recognize")
            .json(&serde_json::json!({ "image_base64": "AAAA", "media_type": "image/jpeg" }))
            .await;
        assert_eq!(res.status_code(), 401);
    })
    .await;
}

#[tokio::test]
#[serial]
async fn recognize_rejects_unsupported_images_before_calling_claude() {
    request::<App, _, _>(|request, ctx| async move {
        let user = prepare_data::init_user_login(&request, &ctx).await;
        let (name, value) = prepare_data::auth_header(&user.token);
        let res = request
            .post("/api/lens/recognize")
            .add_header(name, value)
            .json(&serde_json::json!({ "image_base64": "AAAA", "media_type": "application/pdf" }))
            .await;
        assert_eq!(res.status_code(), 400);
    })
    .await;
}
