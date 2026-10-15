# Fixoom Android apps

Two native Kotlin / Jetpack Compose apps share one Supabase backend with the [Next.js admin panel](https://github.com/mbr0775/fixoom_admin).

- `app/`: Fixoom Customer, application ID `com.fixoom.customer`.
- `provider/`: Fixoom Provider, application ID `com.fixoom.provider`.
- `core/`: authentication, backend client, typed models and repositories.
- `marketplace-ui/`: shared connected booking, profile, conversation and receipt screens.
- `platform/database/`: applied database migrations and transactional permission tests.

Both apps can be installed together. They have separate launcher icons and authenticated sessions. Changes to shared modules require building both apps.

## Connected workflow

Customers register/sign in, browse active categories and available approved professionals, request a service, follow its status, exchange booking messages and view completion/payment records. Providers submit their professional profile for administrator approval, enable availability, accept or decline requests, start jobs, enter the agreed final LKR amount and record payment received. The admin panel manages approvals, suspensions, categories, cancellations and audit history, and reads the same booking conversations and payment records.

Customers **pay the provider directly after service**. Recording payment received is bookkeeping; the apps do not transfer money. Screens refresh every 12 seconds while visible and offer manual refresh. This is foreground polling, not background push notification delivery.

See [the connected workflow guide](docs/CONNECTED_WORKFLOW.md) for the account setup and complete test journey. Administrator login is `mubassirnasar@gmail.com`; official support is `fixoomservices@gmail.com`. Use separate customer and provider accounts when testing both roles.

## Local setup

Open this repository root in Android Studio, use its bundled JDK and install the SDK versions required by the module build files. Configure these public client values in the ignored `local.properties`:

```properties
sdk.dir=YOUR_ANDROID_SDK_PATH
SUPABASE_URL=https://mzkrxovluljvsqzbowwb.supabase.co
SUPABASE_ANON_KEY=YOUR_PUBLIC_CLIENT_KEY
```

Customer, Provider and Admin must always use this same Supabase project (`mzkrxovluljvsqzbowwb`), including the admin's Vercel environment. Use its public client key. Never embed a service-role key or signing credentials. The migrations extend the existing Fixoom project and are not a complete bootstrap for an empty database; read `platform/database/README.md` first.

## Build and install

From this repository root on Windows:

```powershell
.\gradlew.bat :app:assembleDebug :provider:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :core:testDebugUnitTest
.\gradlew.bat :app:installDebug :provider:installDebug
```

Separate APKs are generated at `app/build/outputs/apk/debug/app-debug.apk` and `provider/build/outputs/apk/debug/provider-debug.apk`. Android Studio run configurations `app` and `provider` also install them separately. Pushing source to GitHub does not update an installed app: build and install its APK after changes. Public distribution requires independently signed release builds for both application IDs.

## Verification and release scope

The connected workflow passed eight opt-in instrumentation checks on the Honor NIC-LX2 (Android 15), real admin approval and receipt inspection through the web UI, and transactional database permission tests. Temporary verification accounts and records were removed afterward. Fixture instrumentation requires explicit temporary test credentials and skips without them; never pass a real user's credentials to those tests. Do not use `connectedDebugAndroidTest` against a phone whose app data must be retained; install and invoke instrumentation explicitly on a test device.

The provider's existing native 3D/parallax visual treatment is preserved; see `provider/REDESIGN.md`. Historical prototype screens remain in source, but current navigation uses connected service screens. Latest mobile snapshots are limited to 200 bookings, 100 conversation messages and 500 directory entries. Background push, media attachments, rescheduling, refunds, customer password recovery, published legal policies and store release preparation remain outside this delivery. This is a tested development workflow, not a claim of full public-release readiness.

## Repositories

- Android and shared database: [mbr0775/fixoom_services](https://github.com/mbr0775/fixoom_services), branch `main`.
- Active admin: [mbr0775/fixoom_admin](https://github.com/mbr0775/fixoom_admin), branch `main`.

Historical admin snapshots on `codex/fixoom-admin-platform` are not the active admin application.

## Sign-up reliability update — October 15, 2026

Registration clears an older local auth session before requesting the new account and resolves a profile only for the resulting matching identity. Confirmation-only responses open email verification. Sign-in also recognizes an unconfirmed email; a missing private profile clears the unusable session and presents recovery instructions instead of the SDK's “List is empty” error. SDK/server exception details are not displayed directly.

Eight isolated HTTP regression tests cover confirmation-only registration, stale sessions, confirmed provider registration, missing profiles, unconfirmed sign-in, failed registration and wrong-role denial. Run them with `:core:testDebugUnitTest`. They use only test HTTP responses and never create production accounts. No database schema or approval rules were changed by this fix.
