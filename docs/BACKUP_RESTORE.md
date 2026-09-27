# Backup and restore

The Android app supports password-encrypted local files and optional Google Drive
backups. The Google integration uses only the hidden `appDataFolder`; it does not
request access to the user's normal Drive files.

## Google Cloud setup

The production publisher must complete these steps in the Google Cloud project
used for the Play release:

1. Open [Google Cloud Console](https://console.cloud.google.com/), select or create
   the production project, and record its project ID.
2. Open **APIs & Services → Library**, find **Google Drive API**, and select
   **Enable**.
3. Open **Google Auth Platform**:
   - **Branding:** set the production app name, support email, logo, home page,
     privacy-policy URL, and authorized domains.
   - **Audience:** choose Internal only for a Google Workspace organization that
     owns every user; otherwise choose External. While status is Testing, add every
     tester under **Test users**.
   - **Data Access:** add only
     `https://www.googleapis.com/auth/drive.appdata`. Do not add broad `drive` or
     `drive.file` scopes.
   - Add the publisher contact email. Submit for Google verification if the
     production audience or scope review requires it.
4. Open **Google Auth Platform → Clients → Create client → Android**. Enter the
   exact application ID and signing-certificate SHA-1:
   - Current development ID: `com.example.money`.
   - Debug SHA-1: run `./gradlew signingReport` and use the `debug` variant value.
   - Locally signed release SHA-1: run
     `keytool -list -v -keystore /path/to/release.jks -alias <alias>`.
   - Play-distributed release SHA-1: in Play Console open **Setup → App integrity
     → App signing key certificate**. Use the SHA-1 of the **app signing key**,
     not the upload key.
5. Create a separate Android OAuth client for every package/SHA-1 combination
   used by debug, direct release, and Play App Signing. A mismatched package or
   certificate causes authorization failure.
6. Install a build signed by each registered certificate and verify connect,
   upload, list, download/preview, restore, delete-one-backup, and disconnect.

No web OAuth client secret belongs in the APK. Google Play services resolves the
Android client from the installed package name and signing certificate.

> Release blocker: `com.example.money` is still a placeholder-style application
> ID. The owner must decide the permanent ID before Play app creation and before
> production OAuth clients are registered. Changing it later creates a different
> Play app and requires new OAuth clients.

## Encryption and password recovery

- A backup is encrypted before it is written or uploaded.
- The envelope uses PBKDF2-HMAC-SHA256 (210,000 iterations, random 16-byte salt)
  and AES-256-GCM (random 12-byte IV). The authenticated header includes the
  format marker and envelope version.
- The backup password is not uploaded, included in the backup, or recoverable by
  the publisher. A forgotten password means that backup cannot be restored.
- For automatic backup only, the password is wrapped by a non-exportable Android
  Keystore AES-GCM key and stored on that device. It is cleared when automatic
  backup is disabled, Google backup is disconnected, or all local data is
  deleted. It does not move to a replacement device.
- Store the password in a password manager and test a restore before depending on
  a backup.

## Automatic backup limitations

- WorkManager requests a run approximately once per day; Android may defer or
  skip it because of Doze, battery policy, force-stop, OEM restrictions, or lack
  of suitable network.
- The worker requires battery-not-low. “Wi-Fi only” is implemented as an
  unmetered-network constraint; Android decides whether a network is unmetered.
- Google consent that needs user interaction cannot be completed in background.
  The app marks reconnection as required and waits for the user.
- Retry is limited to network errors, HTTP 429, and server errors. Credential,
  authorization, malformed-data, and other permanent errors require user action.
- After a successful upload, the app keeps the newest five app backups and
  deletes older app backups from `appDataFolder`.
- Uninstalling, clearing app storage, changing devices, or invalidating the
  Keystore removes the saved automatic-backup credential. Existing remote files
  remain, but their password is still required.

## Restore semantics

1. The app reads the complete encrypted file, decrypts it, verifies its format,
   record counts, references, limits, and content hash, and shows a preview.
2. Confirmed restore runs under `SyncWriteGuard`.
3. Room business tables are replaced in one Room transaction. Local numeric IDs
   are regenerated and relationships are rebuilt from portable sync IDs.
   `sync_queue` and sync tombstones are cleared.
4. Portable preferences and the optional vision image are replaced. If apply
   fails, the app attempts to restore the exact prior Room state, preferences,
   sync state, and image.
5. Reminder, messaging, autopilot, weekly check-in, voice, widget, and calendar
   reconciliation runs afterward. A reconciliation failure is returned as a
   warning; it does not undo an otherwise successful restore.

Restore replaces local data; it does not merge with it and does not modify the
source Google Drive backup.

## Included data

- Savings goals, debts, debt payments, transfers, expenses, and luck-diary entries.
- Portable user, privacy, gamification, autopilot, weekly check-in, advisor,
  goal-psychology, calendar-behavior, personal-debt notification, voice,
  language, currency, onboarding, and welcome preferences.
- Phone number and email stored on debt records.
- The current vision image, when valid and within the size limit.

## Deliberately excluded fields

- Room primary keys; relationships use portable sync IDs instead.
- Contact lookup key, contact URI, and contact photo URI.
- Calendar event ID and calendar sync-failure flag.
- `sync_queue`, tombstones, device ID, and other P2P transport metadata.
- Widget caches, notification/calendar permission-request flags, delivery and
  last-open timestamps, calendar ID, and local file paths.
- Backup status, automatic-backup schedule state, wrapped password/Keystore key,
  Google access token, and Google authorization state.
- Notifications, WorkManager jobs, and OS permission grants.

## Delete behavior

**Delete all local data** clears local Room data, local preferences, the vision
image, sync device ID, wrapped backup credential, schedules, notifications, and
linked calendar events where permission permits. Active P2P service is stopped so
a peer cannot immediately repopulate the cleared database. It intentionally does
not delete Google Drive backups. Remote deletion is a separate explicit choice in
Google backup settings.
