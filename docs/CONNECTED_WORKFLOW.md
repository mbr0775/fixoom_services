# Fixoom connected workflow — October 15, 2026

The customer Android app, provider Android app and Next.js admin panel now use the same Supabase project, `mzkrxovluljvsqzbowwb`. This addendum supersedes September baseline statements that describe booking as local sample data or the admin as planned. The master PDF remains the historical baseline.

## Use the three applications together

1. Open **Fixoom Customer** on the Honor phone. Go to Profile, then account/sign-in. Create a customer account with an email you control and verify the email if prompted.
2. Open **Fixoom Provider**. Register with a different email you control. Customer, provider and administrator roles use separate identities; an administrator account is not a mobile customer/provider login.
3. While approval is pending, complete the provider's professional name, service area and introduction. In Skills & services, add a description for each selected skill.
4. Open the admin panel, sign in as **mubassirnasar@gmail.com** using its existing Supabase password, inspect and approve the provider account, then review each offering in Skills & offerings. The official support address is **fixoomservices@gmail.com**.
5. In Provider, check approval or reopen the app, then enable availability. Only approved, available providers with at least one approved active skill and completed service area appear in customer discovery.
6. In Customer, open the provider and request service with a future date/time, address and problem description. The request is saved to the backend; retries using the same request key do not create duplicate bookings.
7. In Provider, open Requests, accept the booking, start the job and complete it with the agreed final amount in LKR. Customers pay the provider directly after service. Mark payment received only after receiving the money.
8. Open the same booking in Customer to see its status, history, final amount, receipt acknowledgment and conversation. Participants can exchange messages from booking details. Admin can inspect the same booking and conversation, manage permitted cancellations and review the audit trail.

Newly registered providers require administrator approval. No demo provider or test booking remains in the live database after verification, so an empty directory is expected until an approved provider enables availability.

Mobile screens refresh every 12 seconds while visible; manual refresh is also available. Admin data reloads on explicit refresh, navigation/search/filter/page changes or successful administrator actions, with no interval or window-focus reload. This does not deliver background push notifications. The phone and browser need internet access. Display times use Asia/Colombo. Latest mobile snapshots cover up to 200 bookings, 100 messages per conversation and 500 directory entries; receipt totals explicitly cover that loaded booking range.

## What was connected

- Customer authentication/account editing, live category/provider discovery and UUID-based booking navigation.
- Provider professional profile, approval state, availability, requests, job lifecycle, schedule, activity and recorded direct receipts.
- Participant-only messages and booking event history, readable by the authorized administrator.
- Administrator approvals, provider and customer suspensions/restoration, category controls, cancellation and audit history.
- Completion amount and provider acknowledgment of direct payment, shown consistently in all three interfaces. These records do not transfer funds or independently prove payment.

The provider's existing Compose 3D/parallax dashboard is preserved. Connected screens share theme, components and repositories through `core` and `marketplace-ui`. Historical prototype screens remain in source but current navigation opens the connected workflow.

## Verification completed

Eight opt-in tests passed on the Honor NIC-LX2, Android 15:

1. Pending provider saves a professional profile without bypassing approval.
2. Approved provider enables availability.
3. Customer discovers the provider, requests a real service and sends a message.
4. Provider accepts, starts, completes for LKR 2,500.50, records receipt and replies.
5. Customer reopens the completed booking and sees receipt/history/reply.
6. Customer signs in through the shared account form and sees the account identity.
7. Actual provider MainActivity opens recorded receipts from the branded home.
8. Actual customer MainActivity opens the backend provider and booking form.

The real admin web UI approved the temporary provider and inspected the mobile-created completion, receipt and messages. Separate browser fixture checks cover login errors, role denial, pagination/search, confirmation and error states, CSV safety, dropdown keyboard behavior and all seven sections at 1440, 1024, 390 and 360 pixels. The final booking dialog position is checked in that browser suite.

Android debug assemblies, app/core unit checks, admin production build and lint passed. Both transactional SQL suites passed against the configured project, including role/ownership isolation, idempotency, status changes, conversation access, moderation and direct-payment event handling. Those SQL suites roll back their fixtures.

Temporary live verification accounts, profiles, bookings and messages were removed after the device/browser tests. The original active administrator was retained. Test-only APKs were removed; both main Android apps remain installed. No test password or administrator password is included in either repository.

Local evidence is under `Documentation/connected-workflow`. Screenshots labelled as fixtures show synthetic verification data, not real customers or business results.

## Source and operation

- Android/shared database: [fixoom_services](https://github.com/mbr0775/fixoom_services), branch `main`.
- Active Next.js admin: [fixoom_admin](https://github.com/mbr0775/fixoom_admin), branch `main`.
- The admin application is at the repository root; Vercel Root Directory is `./`. Configure the two public client environment variables from `.env.example` for the same Supabase project. Never expose a service-role key.
- Local admin preview: `npm run build`, then `npm run start -- --port 3010`; open `http://localhost:3010`.
- Build/install both phone apps from the Android repository using `:app:assembleDebug`, `:provider:assembleDebug`, `:app:installDebug`, `:provider:installDebug`. A GitHub push alone does not update installed APKs.
- Applied migrations and SQL tests are versioned in mobile `platform/database`; the local `Fixoom_Platform/database` working copy mirrors them. These extend the existing project baseline, not an empty database.

## Remaining public-release work

The connected development workflow is verified. Public release still requires published Terms/Privacy, signed releases and store setup, customer password recovery and any desired media attachments, rescheduling or refund features. Background push is implemented below; real opted-in delivery still needs device confirmation. An online payment gateway is outside the chosen direct-payment workflow.

Vercel hosting has not been verified: the connector cannot access the previously specified team, and searching the currently accessible projects for Fixoom returns none. No production admin URL is claimed. Connect an authorized Vercel team/project to `mbr0775/fixoom_admin` to publish it with the configured environment variables.

The Supabase security advisor has no schema/RLS warnings for this delivery. Leaked-password protection is disabled; review [Supabase password protection](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection) before public release.

### Real provider discovery

Home, Explore and provider booking routes read the same Supabase directory. Explore cards open the actual provider ID, with no invented ratings, distance or example professionals. The provider must be approved, available, and have a display name, active category and service area. If none qualify, customers see an empty state. Do not create a demo provider to fill it. SMS/WhatsApp phone verification is deferred; entering a contact number alone does not verify ownership.

### Provider profile photo

Provider Profile → Add/Change photo opens Android Photo Picker. Review the centered preview, then Save photo; Discard leaves the saved photo unchanged. Remove photo clears it. The private shared Storage bucket accepts JPEG uploads up to 1 MB; mobile preparation resizes selected images and strips EXIF/location metadata. The photo is shown on provider home, customer directory/details and admin review. Pending photos remain owner/admin-only; public read requires an approved provider with a complete active service profile. Do not select an arbitrary personal image on behalf of a real user during testing.

Photo controls refinement: tap the provider avatar or Edit photo, choose gallery/camera, adjust crop/zoom/rotation, tap Use photo and then Save photo. View and confirmed removal are in the same action sheet. Camera output is shared only through the provider app's dedicated private cache FileProvider.

Every gallery/camera selection must pass through Crop & adjust before a Save photo preview is available. Adjust crop reopens the original selected image before upload, so reframing does not repeatedly crop a compressed preview. Cancel keeps the last confirmed preview; Discard leaves the published photo unchanged.

Provider Profile now opens a read-only My profile screen with the saved image, service details, availability and account contacts. The toolbar edit icon opens Edit profile, including photo crop/adjust controls. A successful profile save and refresh returns to the updated overview; failed saves stay in the editor. Back from editing returns to the profile view without submitting form changes. Pending providers can still check approval from the overview.

The profile overview now uses a layered portrait header with scroll-driven 3D parallax, animated detail sections and an edit-screen transition. Personal details, Service details and About your work expand to show saved data; editing remains a separate screen. Profile essentials counts saved photo, complete contact fields, service name/category/area and introduction text, without implying phone verification or administrator approval. Motion follows system animation settings, lifecycle and battery saver.


## Multiple skills and separate service approvals — October 16, 2026

Use one provider account for all genuine skills. Registration offers the active service categories from the shared database; selected categories start as pending requests. After email confirmation/sign-in, open **Profile → Skills & services**, choose a category and describe the experience/service offered. A request needs 10–2,000 characters. Signup selections need this description completed before an administrator can approve them.

In Admin → Providers, **Skills & offerings** is a separate, paginated review queue. Approve/reject each request, or suspend an approved offering. Account approval alone never publishes an unreviewed skill. Approving a skill alone does not activate a pending/suspended account. Each service decision is audited and appears in Activity. No timer or focus refresh was added.

Once the account and at least one active service are approved, complete the service area and enable availability. Customers discover the same provider in every approved category. The booking form asks for the service and reserved time. All skills use the provider's shared schedule: overlapping accepted/in-progress reservations are blocked by the backend. Old clients use the approved primary service with a 60-minute reservation. Customers still pay providers directly after service.

Profile editing can choose a primary service only from approved categories. Removing an approved skill hides it from new bookings; adding it again requires review. Existing accepted work can still progress and remains in history. This version requires removing/resubmitting to change an approved offering's description. General profile introduction and offering descriptions are separate.

The provider app keeps its light theme and extends the existing depth design with raised icon headers, decorative layers, scroll parallax and short screen transitions across skills, jobs/schedule, booking conversations/details, receipts and authentication screens. Motion follows system settings, lifecycle and battery saver. Text/inputs remain stable for reading and editing.

Validation: both debug APKs and all 50 unit tests passed. All 21 provider UI checks and both customer directory checks passed on Honor NIC-LX2 (including multi-select registration, offering validation/removal confirmation and discovery by an approved secondary skill). All six SQL policy suites passed with rolled-back fixtures. Admin build/TypeScript/lint and browser checks passed, including skill-review confirmation, failed-write recovery, no idle/focus refresh and responsive widths of 1440/1024/390/360. Updated customer and provider APKs were installed with accounts retained. No fictional provider or operational demo data was added.

The migration is applied to shared project `mzkrxovluljvsqzbowwb` and mirrored in both local database folders. Vercel project inspection still returns 403 for the previously specified team, so this update's hosted admin deployment is not verified. Publishing source to GitHub does not itself prove a Vercel deployment succeeded.


## Booking alerts, nearby search and optional live tracking — October 16, 2026

Bookings and participant replies now create a durable, recipient-only Supabase notification inbox. Realtime updates refresh the open Android apps; a 30-second foreground recovery refresh handles missed connections. Notification taps open the protected booking. A customer cannot read another customer's job, messages or position. The admin retains explicit refresh; no interval/focus reload was added.

Firebase project `fixoom-services-live-077-519b8` (display name `fixoom-services-live-0775`) supplies Android push only. Customer package `com.fixoom.customer` and provider package `com.fixoom.provider` are registered. Supabase `mzkrxovluljvsqzbowwb` remains the only business database and authentication service. The Google account used for this Firebase setup is `fizoomservices@gmail.com`; this does not change the official Fixoom contact or Supabase account.

The deployed `dispatch-booking-alerts` Edge Function authenticates a server-only dispatch key, claims recipient/device jobs, signs an FCM HTTP v1 OAuth assertion and records actual delivery acceptance or retry/failure. A dedicated FCM-only service account is encrypted in Supabase Vault as `fixoom_fcm_service_account`; `fixoom_push_dispatch_key` is also encrypted. Neither credential is stored in client code or Git. The private outbox retries network errors with backoff and retires invalid tokens. Post-commit dispatch plus the `fixoom-booking-push-retry` cron job work without a running mobile app. Success means Google accepted a send, not proof a phone displayed it. Notifications contain generic text; opening the job still requires its authorized session.

On each phone, sign in to the matching app and open Notifications → Allow phone notifications. Device registrations are tied to the authenticated role and installation; sign-out unregisters/deletes the token best-effort, account changes replace the old registration, and foreground message handling verifies the recipient. OS permission, channel settings, connectivity and device battery restrictions can affect display. The sender's live authenticated configuration check returned HTTP 200 with no queued jobs; actual background device delivery remains unverified until a real opted-in booking/reply is sent and observed.

For nearby discovery, the provider opens Profile → Edit profile → Where you offer services, chooses a radius, and explicitly saves the current location as the real service base. Exact base coordinates remain provider/admin-only. Customers use GPS in the Home location picker to discover active, available, approved providers within 50 km who cover that point. Results show approximate straight-line distance and require a saved provider base. Manual location selection clears stored GPS discovery coordinates and returns the approved directory; it does not pretend to calculate proximity. No eligible providers means an empty state.

For an accepted/in-progress booking, the assigned provider may tap Share live location for this job. It requests foreground location access and starts a visible location foreground service with a Stop action. Fresh positions publish approximately every 15 seconds while available, including when the app is backgrounded. The customer opens the reported point in the phone's map app. This is a position/status workflow, with no invented ETA or road route. Sharing is voluntary, stops on closure/session/access failure, and can be stopped manually. Failed cleanup/process death can leave the last point until its five-minute server expiry. Only the participants and authorized administrator can read active, fresh points. The admin uses Refresh booking to inspect them.

The three migrations and rollback-only tests are in `platform/database`. All eight SQL suites passed after deployment, as did four isolated Deno sender tests. Both Firebase-configured debug APKs built, all 50 unit tests passed, and the updated apps were installed on Honor without clearing accounts. All 11 targeted provider and 12 customer device UI checks passed, along with admin production build/lint and browser checks at four widths. Android UI and browser evidence is local under `Documentation/connected-workflow`; fixtures stay isolated and are not operational data. Security advisor reports deliberate deny-by-default RLS on the two private push tables and the existing disabled leaked-password protection warning. Performance advisor reports existing overlapping policies and unused-index monitoring items.

### Firebase configuration on another build machine

The local module `google-services.json` files are ignored. Download them from the two registered Firebase Android apps, or use an authorized Firebase CLI:

```powershell
firebase apps:sdkconfig ANDROID 1:1050059272045:android:2ab301b20c3a70d610b464 --project fixoom-services-live-077-519b8 --out app/google-services.json
firebase apps:sdkconfig ANDROID 1:1050059272045:android:d5da4b11a8430a0310b464 --project fixoom-services-live-077-519b8 --out provider/google-services.json
```

Debug builds without those files show an honest unconfigured-phone-alert state. Release builds require the matching configuration. Never copy a service-account credential into either module. The current live sender credentials are already encrypted in Vault; rotate them through the authorized server administration workflow when needed.
