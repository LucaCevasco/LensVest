use backend::app::App;
use loco_rs::testing::prelude::*;
use serial_test::serial;

use super::prepare_data;

#[tokio::test]
#[serial]
async fn market_data_requires_login() {
    request::<App, _, _>(|request, _ctx| async move {
        assert_eq!(request.get("/api/portfolio").await.status_code(), 401);
        assert_eq!(request.get("/api/assets/KO").await.status_code(), 401);
    })
    .await;
}

#[tokio::test]
#[serial]
async fn rejects_bad_input_and_users_without_berry_before_calling_claude() {
    request::<App, _, _>(|request, ctx| async move {
        let user = prepare_data::init_user_login(&request, &ctx).await;
        let (name, value) = prepare_data::auth_header(&user.token);
        for path in [
            "/api/assets/KO%3B%20ignor%C3%A1",
            "/api/quotes?tickers=",
            "/api/search?q=a",
            // Valid input, but this user never connected Berry.
            "/api/portfolio",
        ] {
            let res = request
                .get(path)
                .add_header(name.clone(), value.clone())
                .await;
            assert_eq!(res.status_code(), 400, "{path}");
        }
    })
    .await;
}
