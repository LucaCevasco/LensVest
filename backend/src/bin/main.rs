use backend::app::App;
use loco_rs::cli;
use migration::Migrator;

#[tokio::main]
async fn main() -> loco_rs::Result<()> {
    // Local secrets such as ANTHROPIC_API_KEY (see .env.example); real env vars win.
    dotenvy::dotenv().ok();
    cli::main::<App, Migrator>().await
}
