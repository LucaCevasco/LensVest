pub use super::_entities::scans::{ActiveModel, Column, Entity, Model};
use loco_rs::prelude::*;
use sea_orm::{entity::prelude::*, QueryOrder, QuerySelect};
pub type Scans = Entity;

#[async_trait::async_trait]
impl ActiveModelBehavior for ActiveModel {
    async fn before_save<C>(self, _db: &C, insert: bool) -> std::result::Result<Self, DbErr>
    where
        C: ConnectionTrait,
    {
        if !insert && self.updated_at.is_unchanged() {
            let mut this = self;
            this.updated_at = sea_orm::ActiveValue::Set(chrono::Utc::now().into());
            Ok(this)
        } else {
            Ok(self)
        }
    }
}

const HISTORY_LIMIT: u64 = 50;

impl Model {
    /// The user's latest scans, newest first.
    ///
    /// # Errors
    /// When the query fails.
    pub async fn list_for(db: &DatabaseConnection, user_id: i64) -> Result<Vec<Self>> {
        Ok(Entity::find()
            .filter(Column::UserId.eq(user_id))
            .order_by_desc(Column::CreatedAt)
            .limit(HISTORY_LIMIT)
            .all(db)
            .await?)
    }

    /// # Errors
    /// When it doesn't exist or isn't the user's.
    pub async fn find_for(db: &DatabaseConnection, user_id: i64, id: i64) -> Result<Self> {
        Entity::find_by_id(id)
            .filter(Column::UserId.eq(user_id))
            .one(db)
            .await?
            .ok_or(Error::NotFound)
    }
}
