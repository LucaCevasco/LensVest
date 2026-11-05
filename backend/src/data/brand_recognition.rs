//! Lens Mode: asks Claude which brand a camera photo shows and which listed
//! company is behind it. The image only goes to Anthropic for this request;
//! it is never stored (RNF05).

use loco_rs::prelude::*;
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};

use super::managed_agents::AnthropicSettings;

const MODEL: &str = "claude-opus-5-5";
const MESSAGES_URL: &str = "https://api.anthropic.com/v1/messages";
/// Re-runs the request on a fallback model if a safety classifier declines it.
const FALLBACK_BETA: &str = "server-side-fallback-2026-07-01";

const SYSTEM_PROMPT: &str = "\
Identificás marcas de productos en fotos tomadas con la cámara de un celular para una app de inversión.
Mirá el producto o logo más prominente de la foto y decí qué marca es y qué empresa que cotiza en bolsa
es la dueña (por ejemplo Sprite → The Coca-Cola Company, KO). Usá el ticker de la bolsa de EE.UU. cuando
exista, porque en Argentina esas acciones se operan como CEDEARs.
- Si la empresa dueña no cotiza en bolsa, devolvé is_public en false y ticker en null.
- Si no se ve ninguna marca o no estás seguro, devolvé found en false: es mejor no sugerir nada que
  sugerir una empresa equivocada.
- explanation: una frase corta en español rioplatense para el usuario.";

/// What Claude saw in the photo.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct RecognizedBrand {
    pub found: bool,
    pub brand: Option<String>,
    pub company: Option<String>,
    pub ticker: Option<String>,
    pub is_public: bool,
    /// "high", "medium" or "low".
    pub confidence: String,
    pub explanation: String,
}

fn output_schema() -> Value {
    let nullable_string = json!({ "type": ["string", "null"] });
    json!({
        "type": "object",
        "properties": {
            "found": { "type": "boolean" },
            "brand": nullable_string,
            "company": nullable_string,
            "ticker": nullable_string,
            "is_public": { "type": "boolean" },
            "confidence": { "type": "string", "enum": ["high", "medium", "low"] },
            "explanation": { "type": "string" }
        },
        "required": ["found", "brand", "company", "ticker", "is_public", "confidence", "explanation"],
        "additionalProperties": false
    })
}

/// Media types the Messages API accepts for images.
pub const SUPPORTED_MEDIA_TYPES: [&str; 4] = ["image/jpeg", "image/png", "image/webp", "image/gif"];

/// # Errors
/// When the API call fails, the model declines, or the answer doesn't match the schema.
pub async fn recognize(
    settings: &AnthropicSettings,
    image_base64: &str,
    media_type: &str,
) -> Result<RecognizedBrand> {
    let body = json!({
        "model": MODEL,
        "max_tokens": 4096,
        "fallbacks": "default",
        // A single-label classification: low effort keeps it fast.
        "output_config": {
            "effort": "low",
            "format": { "type": "json_schema", "schema": output_schema() }
        },
        "system": SYSTEM_PROMPT,
        "messages": [{
            "role": "user",
            "content": [
                { "type": "image", "source": { "type": "base64", "media_type": media_type, "data": image_base64 } },
                { "type": "text", "text": "¿Qué marca es y qué empresa que cotiza está detrás?" }
            ]
        }]
    });

    let res = reqwest::Client::new()
        .post(MESSAGES_URL)
        .header("x-api-key", &settings.api_key)
        .header("anthropic-version", "2023-06-01")
        .header("anthropic-beta", FALLBACK_BETA)
        .json(&body)
        .send()
        .await
        .map_err(Error::wrap)?;
    let status = res.status();
    let response: Value = res.json().await.map_err(Error::wrap)?;
    if !status.is_success() {
        return Err(Error::string(&format!("anthropic {status}: {response}")));
    }
    parse_response(&response)
}

fn parse_response(response: &Value) -> Result<RecognizedBrand> {
    if response["stop_reason"] == "refusal" {
        return Err(Error::string("no se pudo analizar la imagen"));
    }
    let text = response["content"]
        .as_array()
        .into_iter()
        .flatten()
        .filter(|block| block["type"] == "text")
        .filter_map(|block| block["text"].as_str())
        .collect::<String>();
    serde_json::from_str(&text)
        .map_err(|e| Error::string(&format!("unexpected recognition output ({e}): {text}")))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn reads_the_structured_answer_and_skips_thinking_blocks() {
        let response = json!({
            "stop_reason": "end_turn",
            "content": [
                { "type": "thinking", "thinking": "" },
                { "type": "text", "text": "{\"found\":true,\"brand\":\"Sprite\",\"company\":\"The Coca-Cola Company\",\"ticker\":\"KO\",\"is_public\":true,\"confidence\":\"high\",\"explanation\":\"Es una lata de Sprite.\"}" }
            ]
        });
        let brand = parse_response(&response).unwrap();
        assert_eq!(brand.ticker.as_deref(), Some("KO"));
        assert!(brand.found && brand.is_public);
    }

    #[test]
    fn a_refusal_is_an_error() {
        let response = json!({ "stop_reason": "refusal", "content": [] });
        assert!(parse_response(&response).is_err());
    }
}
