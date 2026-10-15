# Fixoom Android apps

This repository contains both native Android applications for Fixoom Services, built with Kotlin and Jetpack Compose.

- `app/`: Fixoom Customer, application ID `com.fixoom.customer`.
- `provider/`: Fixoom Provider, application ID `com.fixoom.provider`.
- `core/`: shared authentication, identity models and backend client code.

The two application IDs allow both apps to be installed on the same phone. They produce separate APKs and have separate launcher icons. A change to `core` can affect both apps, so build and test both after shared changes.

## Repositories

- Android customer and provider apps: [mbr0775/fixoom_services](https://github.com/mbr0775/fixoom_services), branch `main`.
- Next.js admin panel: [mbr0775/fixoom_admin](https://github.com/mbr0775/fixoom_admin), branch `main`.

The mobile apps are built with Gradle/Android Studio. The admin panel is the application hosted on Vercel.

## Local setup

Open this repository's root folder in Android Studio. Use its bundled JDK and install the SDK versions required by the module build files. Android Studio writes the local SDK path to `local.properties`.

Add `SUPABASE_URL` and `SUPABASE_ANON_KEY` to the ignored `local.properties` file using the existing Fixoom project's public client configuration. Both app modules read these values. Never put a service-role key or signing credentials in an Android app or in Git.

## Build and test both apps

On Windows, from this repository root:

```powershell
.\gradlew.bat :app:assembleDebug :provider:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :core:testDebugUnitTest
```

The APKs are generated separately:

- Customer: `app/build/outputs/apk/debug/app-debug.apk`.
- Provider: `provider/build/outputs/apk/debug/provider-debug.apk`.

For local testing on a connected, authorized Android phone:

```powershell
.\gradlew.bat :app:installDebug :provider:installDebug
```

In Android Studio, you can also select the `app` run configuration to run Customer, then select `provider` to run Provider. Installing one does not replace the other.

## Save future changes

Keep customer screens in `app`, provider screens in `provider`, and shared code in `core`. Check changes, build both apps, commit, and push to this repository. Admin-only changes belong in `fixoom_admin`.

Pushing source code to GitHub does not update an app already installed on a phone. Build and install the corresponding APK to test a mobile update. Store distribution requires separately signed release builds for the two application IDs.

## Current delivery scope

Authentication and the provider visual redesign are implemented. Provider motion and layouts have been checked on the Honor test device; see `provider/REDESIGN.md` for the recorded scope. The customer booking journey still uses local sample data, and provider job, schedule, earnings and profile actions still require backend integration. This repository is a development build, not a completed production marketplace.

Shared database foundation migrations and tests are currently preserved under `platform/database` on the `codex/fixoom-admin-platform` branch. Historical admin snapshots on that branch are not the active admin project; use the separate admin repository above.
