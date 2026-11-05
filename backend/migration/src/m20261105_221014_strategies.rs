use loco_rs::schema::*;
use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
    async fn up(&self, m: &SchemaManager) -> Result<(), DbErr> {
        create_table(
            m,
            "strategies",
            &[
                ("id", ColType::PkAuto),
                ("ticker", ColType::String),
                ("name", ColType::String),
                ("title", ColType::String),
                ("condition", ColType::String),
                ("threshold", ColType::Double),
                ("reference_price", ColType::Double),
                ("side", ColType::String),
                ("amount_usd", ColType::Double),
                ("status", ColType::String),
            ],
            &[("user", "")],
        )
        .await
    }

    async fn down(&self, m: &SchemaManager) -> Result<(), DbErr> {
        drop_table(m, "strategies").await
    }
}
