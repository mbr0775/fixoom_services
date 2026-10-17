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

The provider's existing native 3D/parallax visual treatment is preserved; see `provider/REDESIGN.md`. Current navigation uses the connected service screens with real shared backend records. Latest mobile snapshots are limited to 200 bookings, 100 conversation messages and 500 directory entries. FCM booking alerts, private media attachments and complaints are implemented as described below. Rescheduling, refunds, customer password recovery, published legal policies and store release preparation remain release work. This is a tested development workflow, not a claim of full public-release readiness.

## Profiles, booking evidence and private complaints

Customer: Profile → Account details → Edit profile updates your name, phone and cropped profile photo. Provider: Profile → Edit profile includes account contact editing, a professional profile, service coverage and the existing crop/adjust photo editor. Email remains the verified sign-in identity; updating a phone does not verify ownership (SMS verification is deferred).

Both apps: open a saved booking to add photos/videos or Report an issue privately. Customer Profile → Help and provider Profile → Help & complaints also support general account complaints. Submit a complaint first, then attach evidence. Read saved administrator replies by opening the complaint and refreshing. Complaint replies currently use this saved inbox, not a separate background push alert.

Booking evidence is visible only to booking participants and active administrators. Complaint evidence stays private to its reporter and administrators, even when linked to a booking. Files are stored in private buckets on the same Supabase project. Five attachments per person/context; MP4 videos up to 60 seconds and 25 MB; images are prepared without cropping the evidence frame (up to 15 MB input, reduced JPEG upload). Published evidence is immutable. Uploads require an explicit tap, use fresh immutable keys and support retry after interrupted responses. Failed/unpublished uploads may leave private objects for later cleanup; there is no automatic evidence retention/deletion job in this version.

Admin: [Complaints](https://fixoom-admin.vercel.app/#complaints) provides paginated private reports, protected evidence viewing, status updates and replies. Booking details include participant attachments. No periodic admin reload is used. Shared backend migration `20261017050126_user_profiles_and_service_evidence` and rollback tests are versioned under `platform/database`.

## Repositories

- Android and shared database: [mbr0775/fixoom_services](https://github.com/mbr0775/fixoom_services), branch `main`.
- Active admin: [mbr0775/fixoom_admin](https://github.com/mbr0775/fixoom_admin), branch `main`.

Historical admin snapshots on `codex/fixoom-admin-platform` are not the active admin application.

## Sign-up reliability update — October 15, 2026

Registration clears an older local auth session before requesting the new account and resolves a profile only for the resulting matching identity. Confirmation-only responses open email verification. Sign-in also recognizes an unconfirmed email; a missing private profile clears the unusable session and presents recovery instructions instead of the SDK's “List is empty” error. SDK/server exception details are not displayed directly.

Eight isolated HTTP regression tests cover confirmation-only registration, stale sessions, confirmed provider registration, missing profiles, unconfirmed sign-in, failed registration and wrong-role denial. Run them with `:core:testDebugUnitTest`. They use only test HTTP responses and never create production accounts. No database schema or approval rules were changed by this fix.

## Contact, approval and email return update

Local Sri Lankan contacts such as `0771234567` are now normalized to E.164 in the shared client and database. The applied contact migration repairs the original phone constraint and fills missing contacts from the original signup metadata without overwriting saved profile contacts. Accounts with different emails may share a contact number.

Provider approval checks show progress, a clear still-pending result, or a retryable error. Approval opens the workspace; the button never approves its own account. Confirmation links now explicitly target `fixoom-provider://auth/confirmed` or `fixoom-customer://auth/confirmed`, and both apps return to their sign-in screen. Add those exact URLs plus the existing recovery URL `fixoom-provider://auth` to the same Supabase project's Auth redirect allowlist. The user confirmed this setup on October 15, 2026. Older links that still land on the admin origin use its app handoff page; mobile sessions are never imported into the admin.

Verification includes 12 repository auth tests, 2 contact normalization tests, 3 provider approval ViewModel tests, four transactional SQL suites and browser callback/role checks. Phone routing is checked by opening the registered confirmation URI; this does not substitute for inbox delivery verification or send email on behalf of a user. Admin refresh is now manual, with real Auth email shown through protected profile contacts.

## Customer provider directory update

Home, Explore, category lists and provider details use only the shared Supabase directory. Explore no longer recommends hardcoded professionals or claims ratings, distance, experience or estimates that the backend does not supply. Cards open the returned provider UUID; category counts use the loaded real directory. Approved, available providers need an active category, display name and service area to appear. An empty directory stays empty; connection failures show an error and retry instead of sample records. Email confirmation remains enabled; SMS/WhatsApp verification is deferred and saved phone numbers are not verified.

Three isolated SDK tests cover empty results, returned identity/profile completeness and backend failure without sample fallback. `RealProviderDiscoveryTest` exercises Explore with isolated screen inputs, including directory updates, exact provider ID navigation and error recovery. It neither signs into a user's account nor writes fixtures to production.

Validation for this update: 44 core/customer/provider unit tests passed, both debug APKs built and installed on the Honor NIC-LX2, and both `RealProviderDiscoveryTest` instrumentation checks passed using a manually installed test APK (without clearing the app data). The live guest directory returned zero eligible providers at verification time; no production accounts were added or approved for this check.

## Provider profile photos

In the Provider app, open Profile → Add photo (or Change photo), choose an image in Android Photo Picker, review it and tap Save photo. Remove photo clears the published image. Uploads stay in the shared Supabase project in the private `provider-photos` bucket. The photo appears on the provider dashboard, real customer provider cards/details and admin review. Pending providers can upload a photo for review; public visibility follows approval and a complete service profile. Image preparation, SDK request ordering and rollback-only Storage permission tests cover the feature.

Photo-update validation: both debug apps built and were installed with app data preserved. All 48 unit tests, six provider screen checks, five rollback-only SQL permission suites and the admin browser suite passed. The real provider profile opens Android Photo Picker. No personal photo was chosen or uploaded by the agent; the user selects and saves their own image.

Profile photo controls now use a tappable avatar and an Edit photo action sheet. Choose gallery or camera, position/zoom/rotate the circular preview, tap Use photo, then Save photo. View photo opens the existing image; Remove photo asks for confirmation. Cancel and Discard leave the saved photo unchanged.

The provider My profile screen now has a portrait header with scroll-driven 3D parallax, expandable read-only detail sections, a saved-data essentials indicator and animated navigation into editing. Motion respects system animation settings and battery saver; all profile data still comes from the shared backend.


## Multi-skill provider approvals

Providers select multiple service skills at signup and submit an experience/offering description for each in Profile → Skills & services. The administrator must approve the account and each offering separately. Admin → Providers now includes a paginated Skills & offerings queue, with approve/reject/suspend decisions and audit history. Customers discover/book only approved active services, selecting the category and reserved duration; the backend prevents overlapping accepted work across the provider's skills. Existing published services are preserved and no demo professionals are created.

See [the connected workflow](docs/CONNECTED_WORKFLOW.md#multiple-skills-and-separate-service-approvals--october-16-2026) for usage, checks and deployment limits. All apps continue to share Supabase project `mzkrxovluljvsqzbowwb`.

## Connected booking notifications and location

Both Android apps now include recipient inboxes and Firebase phone-alert registration, real nearby discovery from opt-in service bases, and voluntary location sharing during active jobs. Firebase is used only for push; Supabase remains the shared database/auth service. See [the connected workflow](docs/CONNECTED_WORKFLOW.md#booking-alerts-nearby-search-and-optional-live-tracking--october-16-2026) for phone setup, private credential handling, build configuration and the exact verification limits.
