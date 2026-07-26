# Google Play release checklist

## 1. Permanent identity

- [ ] Owner chooses the permanent `applicationId`. Current value
  `com.example.money` is placeholder-style and must not be changed by automation.
- [ ] Confirm the chosen ID is available before creating the Play app.
- [ ] If the ID changes, update `namespace` only if desired, deep-link/OAuth
  documentation, every Google Android OAuth client, and any external references.
- [ ] Confirm release version is `versionCode = 3`, `versionName = 1.2.0`. Increase
  `versionCode` for every later Play upload.

Changing an application ID after release is not an update; it creates a different
app and existing users do not migrate automatically.

## 2. Signing and AAB

- [ ] Create and securely escrow an upload keystore; never commit it or its
  passwords.
- [ ] Configure the build environment only:
  - `MONEY_RELEASE_STORE_FILE`
  - `MONEY_RELEASE_STORE_PASSWORD`
  - `MONEY_RELEASE_KEY_ALIAS`
  - `MONEY_RELEASE_KEY_PASSWORD`
- [ ] Enroll in Play App Signing and separately archive the upload-key and Play
  app-signing certificate SHA-1/SHA-256 values.
- [ ] Register the Play **app signing key** SHA-1 with the Android OAuth client;
  the upload-key SHA-1 is not the certificate on Play-installed APKs.
- [ ] Run `./gradlew clean :app:testDebugUnitTest :app:bundleRelease`.
- [ ] Verify `app/build/outputs/bundle/release/app-release.aab`, signing identity,
  version, package, mapping/symbol artifacts, and Play Console upload.
- [ ] Keep code/resource shrinking disabled until a signed candidate has device
  coverage for onboarding, Room, reflection/serialization, WorkManager, Google
  authorization, backup/restore, notifications, calendar, contacts, P2P, TTS,
  widgets, and process recreation. Then enable and repeat the full matrix before
  shipping.

An unsigned local AAB is useful for compilation verification but cannot be
uploaded as a production release.

## 3. Policy and store configuration

- [ ] Replace every owner placeholder in `docs/PRIVACY_POLICY.md`, obtain
  owner/legal approval, and publish it at a stable public HTTPS URL.
- [ ] Put that URL in the Play listing and inside the app if Play policy requires.
- [ ] Complete App access, Ads (“No”), Content rating, Target audience, News,
  Government, Financial features, and any other current Play declarations.
- [ ] Confirm the support email, website, publisher name, and country/legal
  details.

## 4. Data Safety review

Complete the current Play form from observed release behavior, not this checklist
alone. Owner/legal must decide the final answers.

- [ ] Inventory financial records, user profile/psychology fields, contact
  details, optional image, app activity/preferences, Google authorization, and
  random installation/sync identifiers.
- [ ] Explain that core data is local; optional encrypted backup is transmitted
  to the user's Google Drive `appDataFolder`; optional P2P sync sends data directly
  across the user's LAN.
- [ ] Do not claim every transmission is encrypted in transit without resolving
  the current plain `ws://` LAN sync transport.
- [ ] Declare that the app has no ads, analytics, sale of data, or developer
  finance-data server, if still true in the final artifact.
- [ ] Verify whether optional, user-initiated Google Drive/P2P transfers qualify
  for current Play “collection,” “sharing,” optional-data, and ephemeral-processing
  exceptions. Play definitions can change.
- [ ] Document user deletion: local reset is in-app; Google backup deletion is a
  separate explicit action; publisher cannot delete device-only data.
- [ ] Recheck Android OS backup/device-transfer behavior for the optional vision
  image and other non-excluded files.

## 5. Permissions and sensitive access

- [ ] Justify every permission shown by the final manifest:
  `NEARBY_WIFI_DEVICES`, Wi-Fi/network/multicast, notifications,
  `RECEIVE_BOOT_COMPLETED`, calendar read/write, contacts read, and `SEND_SMS`.
- [ ] Remove any permission whose user-visible feature will not ship.
- [ ] Confirm nearby-Wi-Fi use remains non-location and retains
  `neverForLocation`.
- [ ] Confirm contacts/calendar permissions are requested in context and the app
  remains useful when denied.
- [ ] Treat `SEND_SMS` as high-risk: verify current Play eligibility/declaration
  requirements and the app's core-function justification. If approval is not
  available, remove direct SMS and use an `ACTION_SENDTO` user-confirmed flow.
- [ ] Verify notification permission UX and deep links on all supported Android
  versions.

## 6. Google integration

- [ ] Complete every step in `docs/BACKUP_RESTORE.md`.
- [ ] Enable Drive API, publish/configure OAuth consent, and request only
  `drive.appdata`.
- [ ] Register separate package/SHA-1 Android clients for debug, direct release,
  and Play App Signing as applicable.
- [ ] Move the OAuth app from Testing to the required production status and
  complete verification if Google requires it.
- [ ] Test reconnect, revoked consent, offline/retry behavior, wrong password,
  restore rollback, retention of five backups, disconnect-keep, and
  disconnect-delete.

## 7. Test tracks and rollout

- [ ] Internal app sharing: install the generated AAB split APKs on representative
  API 24, current API, phone, and tablet devices.
- [ ] Internal track: test clean install, upgrade from versionCode 2, onboarding,
  background work, process death, reboot, and denied permissions.
- [ ] Closed track: satisfy Google's current tester/duration requirements for the
  account type and collect backup/restore and data-loss feedback.
- [ ] Run Play pre-launch report; triage crashes, ANRs, accessibility, security,
  and compatibility findings.
- [ ] Verify Google authorization using a Play-installed build, not only an
  Android Studio build.
- [ ] Use a staged production rollout with monitoring and a halt/rollback plan.

## 8. Store assets

- [ ] Final app name and short/full descriptions in supported languages.
- [ ] 512×512 high-resolution icon and 1024×500 feature graphic.
- [ ] Current phone screenshots; tablet screenshots if tablet support is claimed.
- [ ] Support contact/website and published privacy URL.
- [ ] Accurate category, tags, localization, accessibility text, and release notes.
- [ ] Ensure screenshots and copy do not expose real financial/contact data or
  promise security, cloud recovery, or automatic timing beyond implemented
  behavior.
