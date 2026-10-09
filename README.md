# LensVest

## Backend (`backend/`) — [Loco](https://loco.rs) (Rust, SQLite)

```sh
cd backend && cargo loco start   # http://127.0.0.1:5150/_ping
cargo test
```

- Escucha en `127.0.0.1` (IPv4) para que el emulador llegue por `10.0.2.2`.
- Mails en dev: `mailer.stub: true` (no hace falta SMTP).
- Berry MCP: config en `config/development.yaml` → `settings.berry`. El `client_id` es un cliente OAuth público
  registrado por Dynamic Client Registration con redirect `lensvest://berry/callback`.

### Asistente (Claude Managed Agents)

- Copiar `backend/.env.example` a `backend/.env` y poner `ANTHROPIC_API_KEY` (key de un workspace de la API, no la de Claude Code).
- Una sola vez: `cargo loco task ai_setup` crea el environment y el agente; los IDs van en `settings.anthropic`
  (`config/development.yaml`). Volver a correrlo publica una nueva versión del agente (p. ej. al cambiar el prompt).
- Cada usuario tiene **su propio vault** con la credencial OAuth de Berry; el backend no guarda tokens de Berry
  y Anthropic los renueva. Cada sesión del agente usa solo el vault de su usuario.
- `place_order` y `cancel_order` requieren confirmación (`always_ask`): la app muestra la orden y el usuario
  confirma o rechaza. Cada sesión tiene un tope de gasto (`session_budget_cents`).

Endpoints: `POST /api/auth/register`, `POST /api/auth/login`, y con `Authorization: Bearer <jwt>`:
`POST /api/berry/connect`, `POST /api/berry/callback`, `GET /api/berry/status`,
`POST /api/assistant/messages` (`{text}`), `POST /api/assistant/confirm` (`{approve}`).

## App (`app/`) — Kotlin + Jetpack Compose (MVVM / Clean Architecture, Hilt)

Abrir la carpeta `app/` en Android Studio y Run. CLI: `cd app && ./gradlew installDebug`.

```
ar.edu.uade.lensvest/
├── data/          local (SessionStore), remote (api, dto), repository (*Impl)
├── domain/        model, repository (interfaces), usecase
├── mapper/        DTO → dominio, errores → mensaje
├── presentation/  di (Hilt), navigation, ui (screens), viewmodel
└── ui/theme/
```

- El build debug apunta a `http://10.0.2.2:5150/` (`BuildConfig.BASE_URL`).
- **Android 17 (API 37)**: `10.0.2.2` cuenta como red local. El build debug pide el permiso
  "dispositivos cercanos" (`ACCESS_LOCAL_NETWORK`) al abrir la app: aceptarlo, si no, no llega al backend.
