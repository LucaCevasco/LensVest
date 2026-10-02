# LensVest

- `app/` — Android (Kotlin + Jetpack Compose). Abrir la carpeta `app/` en Android Studio y darle Run.
  CLI: `cd app && ./gradlew installDebug` (requiere JDK 17+; sirve el JBR de Android Studio).
- `backend/` — [Loco](https://loco.rs) (Rust, SQLite).
  `cd backend && cargo loco start` → http://localhost:5150/_ping
  (desde el emulador Android: `http://10.0.2.2:5150`)
