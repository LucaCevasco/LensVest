use backend::{app::App, models::_entities::berry_connections};
use loco_rs::testing::prelude::*;
use sea_orm::EntityTrait;
use serial_test::serial;

macro_rules! configure_insta {
    ($($expr:expr),*) => {
        let mut settings = insta::Settings::clone_current();
        settings.set_prepend_module_to_snapshot(false);
        let _guard = settings.bind_to_scope();
    };
}

/// The `BerryConnections` entity matches the table its migration created.
///
/// Selecting every column is the cheapest assertion that says something true:
/// it fails if the migration and the entity disagree — a renamed column, a
/// type that does not round-trip, a migration that never ran — which is the
/// most common way a generated model breaks. Extend it as the model grows.
#[tokio::test]
#[serial]
async fn can_query_berry_connections() {
    configure_insta!();

    let boot = boot_test::<App>().await.unwrap();
    seed::<App>(&boot.app_context).await.unwrap();

    // The query is the assertion. Bind the result and compare it once you have
    // seed data to compare against, e.g.:
    //
    // let items = berry_connections::Entity::find().all(&boot.app_context.db).await.unwrap();
    // assert_debug_snapshot!(items);
    berry_connections::Entity::find()
        .all(&boot.app_context.db)
        .await
        .expect("`berry_connections` should be queryable — entity and migration must agree");
}

/// A session that hit its spend cap is forgotten, so the user's next message starts a new one.
#[tokio::test]
#[serial]
async fn forget_session_clears_only_that_session() {
    use backend::models::{berry_connections::Model, users};
    use sea_orm::{ActiveModelTrait, ActiveValue::Set};

    let boot = boot_test::<App>().await.unwrap();
    let db = &boot.app_context.db;
    let user = users::Model::create_with_password(
        db,
        &users::RegisterParams { email: "cap@lensvest.dev".into(), password: "12341234".into(), name: "Cap".into() },
    )
    .await
    .unwrap();
    let conn = berry_connections::ActiveModel {
        user_id: Set(user.id),
        vault_id: Set("vlt_1".into()),
        credential_id: Set("vcrd_1".into()),
        session_id: Set(Some("sesn_capped".into())),
        ..Default::default()
    }
    .insert(db)
    .await
    .unwrap();

    Model::forget_session(db, "sesn_other").await.unwrap();
    let kept = berry_connections::Entity::find_by_id(conn.id).one(db).await.unwrap().unwrap();
    assert_eq!(kept.session_id.as_deref(), Some("sesn_capped"));

    Model::forget_session(db, "sesn_capped").await.unwrap();
    let cleared = berry_connections::Entity::find_by_id(conn.id).one(db).await.unwrap().unwrap();
    assert_eq!(cleared.session_id, None);
}
