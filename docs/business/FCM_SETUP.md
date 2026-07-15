# FCM Setup (Phase 4 — optional remote push)

The app ships with **local notifications** by default. Remote FCM push is optional.

## Current behavior

- `BusinessPushDelivery` routes push to `BusinessNotificationHelper` (local).
- `FcmTokenStore` saves token when FCM is configured.
- `BusinessFcmMessagingService` is a stub until Firebase is added.

## Enable FCM (future)

1. Create Firebase project and add `google-services.json` to `app/`.
2. Add to `app/build.gradle.kts`:

```kotlin
plugins { id("com.google.gms.google-services") }
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-messaging")
}
```

3. Register `BusinessFcmMessagingService` in `AndroidManifest.xml`.
4. On token refresh, call `FcmTokenStore.saveToken(context, token)`.
5. `BusinessPushDelivery.sendRemote()` will use the token for server-side sends.

Until then, all push notifications remain **on-device local alerts**.
