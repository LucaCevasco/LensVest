pub use super::_entities::strategies::{ActiveModel, Column, Entity, Model};
use loco_rs::prelude::*;
use sea_orm::{entity::prelude::*, QueryOrder};
use serde::Deserialize;
pub type Strategies = Entity;

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

pub const ACTIVE: &str = "active";
pub const PAUSED: &str = "paused";
const CONDITIONS: [&str; 4] = ["price_below", "price_above", "drop_pct", "rise_pct"];

/// A strategy as the assistant proposes it (`propose_strategy`'s input).
#[derive(Debug, Clone, Deserialize)]
pub struct NewStrategy {
    pub ticker: String,
    pub name: String,
    pub condition: String,
    pub threshold: f64,
    pub reference_price: f64,
    pub side: String,
    pub amount_usd: f64,
    pub title: String,
}

impl NewStrategy {
    /// The input comes from the model: check it before it becomes an order rule.
    ///
    /// # Errors
    /// When a field is out of range.
    pub fn validate(&self) -> Result<()> {
        let pct = self.condition.ends_with("_pct");
        let ok = CONDITIONS.contains(&self.condition.as_str())
            && ["buy", "sell"].contains(&self.side.as_str())
            && !self.ticker.trim().is_empty()
            && self.threshold > 0.0
            && (!pct || self.threshold < 100.0)
            && self.reference_price > 0.0
            && self.amount_usd > 0.0;
        if ok {
            Ok(())
        } else {
            Err(Error::BadRequest(format!("Estrategia inválida: {self:?}")))
        }
    }
}

impl Model {
    /// # Errors
    /// When the query fails.
    pub async fn list_for(db: &DatabaseConnection, user_id: i64) -> Result<Vec<Self>> {
        Ok(Entity::find()
            .filter(Column::UserId.eq(user_id))
            .order_by_desc(Column::CreatedAt)
            .all(db)
            .await?)
    }

    /// The user's strategy `id`; someone else's is a 404.
    ///
    /// # Errors
    /// When it doesn't exist or isn't the user's.
    pub async fn find_for(db: &DatabaseConnection, user_id: i64, id: i64) -> Result<Self> {
        Entity::find_by_id(id)
            .filter(Column::UserId.eq(user_id))
            .one(db)
            .await?
            .ok_or(Error::NotFound)
    }

    /// # Errors
    /// When the strategy is invalid or the insert fails.
    pub async fn create(db: &DatabaseConnection, user_id: i64, new: &NewStrategy) -> Result<Self> {
        new.validate()?;
        Ok(ActiveModel {
            user_id: Set(user_id),
            ticker: Set(new.ticker.trim().to_uppercase()),
            name: Set(new.name.clone()),
            title: Set(new.title.clone()),
            condition: Set(new.condition.clone()),
            threshold: Set(new.threshold),
            reference_price: Set(new.reference_price),
            side: Set(new.side.clone()),
            amount_usd: Set(new.amount_usd),
            status: Set(ACTIVE.to_string()),
            ..Default::default()
        }
        .insert(db)
        .await?)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn nvda() -> NewStrategy {
        NewStrategy {
            ticker: "NVDA".into(),
            name: "NVIDIA".into(),
            condition: "drop_pct".into(),
            threshold: 10.0,
            reference_price: 180.0,
            side: "buy".into(),
            amount_usd: 100.0,
            title: "Comprar NVDA si baja 10 %".into(),
        }
    }

    #[test]
    fn validates_what_the_model_proposes() {
        assert!(nvda().validate().is_ok());
        for broken in [
            NewStrategy { condition: "whenever".into(), ..nvda() },
            NewStrategy { side: "short".into(), ..nvda() },
            NewStrategy { threshold: 150.0, ..nvda() },
            NewStrategy { amount_usd: 0.0, ..nvda() },
        ] {
            assert!(broken.validate().is_err(), "{broken:?}");
        }
    }
}
