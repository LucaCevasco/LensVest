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

Endpoints: `POST /api/auth/register`, `POST /api/auth/login`, y con `Authorization: Bearer <jwt>`:
`POST /api/berry/connect`, `POST /api/berry/callback`, `GET /api/berry/status`.

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
