# Local Google Backup — Verification Report

Date: 2026-07-26  
Branch: `cursor/local-google-backup-57b1`  
Plan: `docs/plans/2026-07-25-001-feat-local-google-backup-plan.md`

## Automated suites

| Suite | Result |
| --- | --- |
| Android `:app:testDebugUnitTest` | **135 passed**, 0 failed (includes ~55 backup/sync-related) |
| `@money/shared` Vitest | **55 passed** |
| `money-web` Vitest | **22 passed** |
| `:app:assembleDebug` | **SUCCESS** (`app-debug.apk`) |

## Verification contract coverage

| Requirement | Evidence |
| --- | --- |
| Existing Android JVM tests remain green | Full suite 135/135 |
| Bundle codec round-trip / corruption / password / version / size | `BackupBundleCodecTest`, `PasswordBackupCryptoTest` |
| Room export/import + FK + rollback | `BackupRepositoryIntegrationTest` |
| Import failure restores pre-import state | rollback cases in integration tests |
| Fake Drive transport (list/upload/download/delete, HTTP, auth) | `GoogleDriveRestClientTest`, `GoogleDriveAuthorizationManagerTest` |
| UI states: disconnected, connected, backing up, restore preview, success, reconnect-required, failure | expanded `BackupSettingsViewModelTest` (8 cases) |
| Reconciler after success only | `PostRestoreReconcilerTest` |
| Worker / automatic backup | `AutomaticBackupWorkerTest` |
| Local reset safety | `LocalDataResetterTest` + ViewModel delete confirmation |
| Shared/web sync schema alignment | shared + web Vitest green after tombstone/schema v3 work |
| Debug APK | `assembleDebug` succeeds |

## Static UI wiring review (no emulator on this VM)

- Route `AppRoutes.BACKUP_SETTINGS` registered in `MoneyNavHost`
- Phone drawer and tablet settings both expose `onOpenBackupSettings`
- `BackupSettingsScreen` covers local export/import, Google connect/backup/restore/delete, automatic backup, and local data reset
- RU/EN backup strings present for titles, errors, and success messages

## Remaining manual / publisher steps (not blocked for merge)

- Physical phone/tablet walkthrough with a real Google Cloud OAuth client
- Fill Google Cloud Console values listed in `docs/BACKUP_RESTORE.md` and `docs/GOOGLE_PLAY_RELEASE.md`
- Publish privacy policy URL and complete Play Console Data Safety form
