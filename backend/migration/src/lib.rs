#![allow(elided_lifetimes_in_paths)]
#![allow(clippy::wildcard_imports)]
pub use sea_orm_migration::prelude::*;
mod m20220101_000001_users;

mod m20261001_231038_berry_connections;
mod m20261009_010000_berry_connections_to_vaults;
mod m20261105_220900_scans;
mod m20261105_221014_strategies;
pub struct Migrator;

#[async_trait::async_trait]
impl MigratorTrait for Migrator {
    fn migrations() -> Vec<Box<dyn MigrationTrait>> {
        vec![
            Box::new(m20220101_000001_users::Migration),
            Box::new(m20261001_231038_berry_connections::Migration),
            Box::new(m20261009_010000_berry_connections_to_vaults::Migration),
            Box::new(m20261105_220900_scans::Migration),
            Box::new(m20261105_221014_strategies::Migration),
            // inject-above (do not remove this comment)
        ]
    }
}
