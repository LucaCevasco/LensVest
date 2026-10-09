use loco_rs::schema::*;
use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
    async fn up(&self, m: &SchemaManager) -> Result<(), DbErr> {
        create_table(
            m,
            "berry_connections",
            &[
                ("id", ColType::PkAuto),
                ("access_token", ColType::Text),
                ("refresh_token", ColType::TextNull),
                ("expires_at", ColType::TimestampWithTimeZone),
            ],
            &[("user", "")],
        )
        .await
    }

    async fn down(&self, m: &SchemaManager) -> Result<(), DbErr> {
        drop_table(m, "berry_connections").await
    }
}
