//! Lens Mode: recognize the brand in a camera photo.

use loco_rs::prelude::*;
use serde::Deserialize;

use crate::data::{
    brand_recognition::{self, SUPPORTED_MEDIA_TYPES},
    managed_agents::ManagedAgents,
};

/// ~1.5 MB of base64; the app sends a downscaled JPEG well under this.
const MAX_IMAGE_BASE64_LEN: usize = 1_500_000;

#[derive(Debug, Deserialize)]
pub struct RecognizeParams {
    pub image_base64: String,
    pub media_type: String,
}

#[debug_handler]
async fn recognize(
    _auth: auth::JWT,
    State(ctx): State<AppContext>,
    Json(params): Json<RecognizeParams>,
) -> Result<Response> {
    if !SUPPORTED_MEDIA_TYPES.contains(&params.media_type.as_str()) {
        return bad_request("Formato de imagen no soportado");
    }
    if params.image_base64.is_empty() || params.image_base64.len() > MAX_IMAGE_BASE64_LEN {
        return bad_request("La imagen está vacía o es demasiado grande");
    }
    let settings = ManagedAgents::from_context(&ctx)?.settings;
    let brand =
        brand_recognition::recognize(&settings, &params.image_base64, &params.media_type).await?;
    format::json(brand)
}

pub fn routes() -> Routes {
    Routes::new()
        .prefix("/api/lens")
        .add("/recognize", post(recognize))
}
