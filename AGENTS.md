# AGENTS.md

Project **Money / Финансовый Контроль** — an offline-first personal finance app.
See `ARCHITECTURE.md` and `docs/AI_CONTEXT.md` for the full product/architecture overview,
and the root/`web`/`server` `package.json` scripts + `./gradlew` tasks for standard commands.

## Cursor Cloud specific instructions

### Services / surfaces
- **Web dashboard** (`web/`, React 19 + Vite 7 + Tailwind 4) — the product surface that is fully
  runnable/testable on this Linux VM. Dev: `npm run dev --workspace=money-web` (Vite on port **5173**).
  It is offline-first (browser `localStorage`), so no backend is required to use it end-to-end.
- **`@money/shared`** (`packages/shared/`) — TypeScript logic consumed by `web` via a Vite alias +
  npm workspace. It is a build dependency, not a running process.
- **Legacy server** (`server/`, Express) — **optional / not used by the web app** (web uses
  localStorage, not `/api`). It lives outside the root npm workspaces and has its own lockfile.
  Run only if needed: `npm --prefix server run dev` (port **3001**).
- **Android app** (`app/`, Kotlin/Compose) — the primary product, but E2E requires an emulator that
  is **not provisioned** on this headless VM. JVM unit tests (Robolectric) do run without an emulator
  (see below).

### Non-obvious gotchas
- **npm optional native-binary bug (important):** a plain `npm install` in this repo does NOT reliably
  install the platform binaries for `lightningcss` and `@tailwindcss/oxide` (Tailwind v4). Without them,
  `npm run test:web`, `npm run build --workspace=money-web`, and Vite all crash with
  `Cannot find module '../lightningcss.linux-x64-gnu.node'` / `@tailwindcss/oxide` MODULE_NOT_FOUND.
  The startup update script fixes this by explicitly installing the matching-version linux-x64-gnu
  binaries. If you ever wipe `node_modules` and reinstall manually, re-run:
  `npm install --no-save "@tailwindcss/oxide-linux-x64-gnu@$(node -p "require('./web/node_modules/@tailwindcss/oxide/package.json').version")" "lightningcss-linux-x64-gnu@$(node -p "require('./web/node_modules/lightningcss/package.json').version")"`
- **Dev vs build:** `npm run dev` runs Vite only (no type-check), so development mode works. The
  production `npm run build --workspace=money-web` runs `tsc --noEmit` first and currently **fails on
  pre-existing type errors** (e.g. `Dashboard.tsx` references `currentCents`/`targetCents` not on
  `FinancialItem`, plus unused-var errors in `packages/shared`). These are pre-existing source issues,
  not environment problems.
- **Pre-existing failing test:** `web/src/lib/goalProgress.test.ts > calculates daily goal from
  deadline` fails under UTC (expects 5000, gets 4546) due to a timezone/day-count off-by-one in the
  code. Unrelated to setup. All other suites pass (shared 44/44, web 18/19).

### Android SDK (for `./gradlew` unit tests)
- The Android SDK is installed at `~/android-sdk` (platform `android-36`, `build-tools;36.0.0`,
  `platform-tools`) and is preserved in the VM snapshot. `local.properties` (gitignored) must contain
  `sdk.dir=/home/ubuntu/android-sdk`; recreate it if missing.
- Run the primary app's JVM unit tests (no emulator needed):
  `ANDROID_HOME=$HOME/android-sdk ./gradlew :app:testDebugUnitTest` (56 tests pass). The first run
  downloads Gradle 9.4.1 + dependencies (~2 min); JDK 21 is present and matches the daemon toolchain.
- There is no ESLint/Prettier config; there is no separate "lint" step for the Node code beyond `tsc`.
