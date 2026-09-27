# Privacy Policy — draft

> **OWNER ACTION REQUIRED BEFORE PUBLICATION**
>
> - Publisher/legal name: **[PUBLISHER LEGAL NAME]**
> - Contact email: **[PRIVACY CONTACT EMAIL]**
> - Published policy URL: **[PUBLIC HTTPS PRIVACY POLICY URL]**
> - Effective date: **[YYYY-MM-DD]**
> - Country/legal address if required: **[PUBLISHER ADDRESS]**
>
> Replace every bracketed placeholder and obtain owner/legal approval. This draft
> is not legal advice and must not be published with placeholders.

**Financial Control** (“the App”) is an offline-first personal finance tool. This
policy explains how the Android app and companion web dashboard handle data.

## Data you provide

Depending on the features you use, the App can store:

- financial goals, balances, deposits, expenses, debts, payment history, dates,
  notes, currencies, and forecasts;
- a display name, optional age/gender/emotion, goals, financial-psychology quiz
  answers, fears, motivation, challenges, badges, streaks, and advisor history;
- debtor names and optional phone numbers and email addresses;
- links to contacts selected on the device, linked calendar event identifiers,
  reminder settings, and message-delivery preferences;
- an optional goal/vision image;
- app language, privacy-display settings, widget caches, and other local
  preferences; and
- a random installation identifier and synchronization metadata used for local
  peer-to-peer synchronization.

The web dashboard stores its finance state in that browser's `localStorage`.

## Where data goes

By default, data stays on the device or in the browser. The publisher does not
operate a production finance-data server for this App. The legacy development
server in the source repository is not used by the Android or web product.

The following optional features transmit data only when enabled or invoked:

### Google Drive backup

If you connect Google Drive, the App requests only
`https://www.googleapis.com/auth/drive.appdata`. It can create, list, download,
and delete this App's files in Google's hidden application-data folder. Backup
contents are password-encrypted on the device before upload and sent over HTTPS.
Google processes the account authorization, access token, and stored file under
Google's own terms and privacy policy. The App does not request access to normal
Drive documents.

### Local-network peer synchronization

If peer sync is used, finance snapshots and synchronization metadata are sent
directly between selected/discovered devices on the same local network. The
current WebSocket transport is not end-to-end encrypted or authenticated. Use it
only on a trusted private LAN; other users or devices with network access may be
able to observe or attempt to connect to the service.

### Android system integrations

The App can write debt reminders to the system calendar, show notifications,
open communication apps, and—only when the direct-SMS option is enabled—send an
SMS through Android. Contact selection reads the chosen contact details into the
App. These actions are handled by Android and the selected calendar, carrier, or
communication provider under their policies.

## Permissions

- **Internet / network / Wi-Fi / multicast / nearby Wi-Fi devices:** Google Drive
  backup and local-network peer discovery/sync. Nearby Wi-Fi is declared with
  `neverForLocation`; the App does not use it to derive location.
- **Notifications and boot completed:** debt, autopilot, weekly, voice, and
  messaging reminders and restoring their schedules after reboot.
- **Read/write calendar:** create, update, and remove debt reminder events.
- **Read contacts:** let you select a contact and copy relevant contact details.
- **Send SMS:** optional direct sending when explicitly enabled; sharing through
  another app does not require this permission.

You can deny permissions in Android settings. The related feature may stop
working, but core offline finance tracking remains available.

## Backups, security, and recovery

Portable backup files use password-based AES-256-GCM encryption. The publisher
does not know or recover the password. For automatic backup, the password is
wrapped with Android Keystore and stored only on that device. Do not share backup
files or passwords.

Android's own backup/device-transfer service may also process app files that are
not excluded by the installed backup rules, subject to the device and Google/OS
backup settings.

## Deletion and retention

The App keeps local data until you edit it, delete it, clear app storage, or
uninstall the App, subject to Android backup/restore behavior.

Use **Backup & restore → Delete all local data** to clear Room finance/sync data,
local user/widget/backup preferences, the goal image, saved backup credential,
sync installation ID, scheduled work, notifications, and linked calendar events
where Android still grants access. It also stops active P2P sync for the current
app process. This action deliberately leaves Google Drive backups untouched.
Delete individual remote files, or choose the separate delete-remote option when
disconnecting Google Drive, to remove those files. Google may retain deleted
content according to its policies.

Browser data can be removed through the web dashboard's controls or by clearing
the site's browser storage.

For privacy requests concerning the publisher, contact
**[PRIVACY CONTACT EMAIL]**. Because the publisher does not receive a copy of
offline local data, the publisher generally cannot locate or delete data held
only on your device, browser, Google Drive account, calendar, contacts, or peers.

## No sale, advertising, or analytics

The App does not sell personal data, display third-party advertising, or include
analytics, behavioral profiling, or crash-reporting SDKs. The local
psychology/advisor features use deterministic on-device logic, not a remote LLM.

## Children and legal requirements

The intended age range and any child-directed use are **[OWNER MUST DEFINE]**.
The publisher may disclose information only when legally required and only to
the extent the publisher actually possesses it.

## Changes

Material changes will be posted at **[PUBLIC HTTPS PRIVACY POLICY URL]** with an
updated effective date. Contact **[PRIVACY CONTACT EMAIL]** with questions.
