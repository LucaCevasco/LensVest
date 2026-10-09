use loco_rs::schema::*;
use sea_orm_migration::prelude::*;

/// Berry tokens move to the user's Claude Managed Agents vault: the vault is
/// now the only place that holds (and refreshes) them. Existing rows hold
/// tokens that must not be reused, so users reconnect Berry once.
#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
    async fn up(&self, m: &SchemaManager) -> Result<(), DbErr> {
        drop_table(m, "berry_connections").await?;
        create_table(
            m,
            "berry_connections",
            &[
                ("id", ColType::PkAuto),
                ("vault_id", ColType::String),
                ("credential_id", ColType::String),
                ("session_id", ColType::StringNull),
            ],
            &[("user", "")],
        )
        .await
    }

    async fn down(&self, m: &SchemaManager) -> Result<(), DbErr> {
        drop_table(m, "berry_connections").await?;
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
}
