# Legacy development server

> **Warning:** this Express service is legacy/dev-only. The Android app and web
> dashboard do not use it for production data, backup, or P2P synchronization.
> Do not deploy it as the Money backend or treat its data model as authoritative.

The current products are offline-first:

- Android persists to Room/DataStore and synchronizes directly over the local
  network.
- Web persists to browser `localStorage`.
- Optional Android cloud backup uses Google Drive `appDataFolder` directly.

Run this service only when maintaining or manually exploring the old REST API:

```bash
npm install --prefix server
npm run dev --prefix server
```

It listens on port 3001 by default. It is outside the root npm workspaces and has
its own lockfile.
