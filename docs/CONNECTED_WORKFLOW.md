# Fixoom connected workflow — October 15, 2026

The customer Android app, provider Android app and Next.js admin panel now use the same Supabase project, `mzkrxovluljvsqzbowwb`. This addendum supersedes September baseline statements that describe booking as local sample data or the admin as planned. The master PDF remains the historical baseline.

## Use the three applications together

1. Open **Fixoom Customer** on the Honor phone. Go to Profile, then account/sign-in. Create a customer account with an email you control and verify the email if prompted.
2. Open **Fixoom Provider**. Register with a different email you control. Customer, provider and administrator roles use separate identities; an administrator account is not a mobile customer/provider login.
3. While approval is pending, complete the provider's professional name, active service category, service area and introduction. Save the profile.
4. Open the admin panel, sign in as **mubassirnasar@gmail.com** using its existing Supabase password, inspect the provider profile and approve it. The official support address is **fixoomservices@gmail.com**.
5. In Provider, check approval or reopen the app, then enable availability. Only approved, available providers with an active category and completed service area appear in customer discovery.
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

The connected development workflow is verified. Public release still requires published Terms/Privacy, signed releases and store setup, customer password recovery and any desired background push, media attachments, rescheduling or refund features. An online payment gateway is outside the chosen direct-payment workflow.

Vercel hosting has not been verified: the connector cannot access the previously specified team, and searching the currently accessible projects for Fixoom returns none. No production admin URL is claimed. Connect an authorized Vercel team/project to `mbr0775/fixoom_admin` to publish it with the configured environment variables.

The Supabase security advisor has no schema/RLS warnings for this delivery. Leaked-password protection is disabled; review [Supabase password protection](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection) before public release.

### Real provider discovery

Home, Explore and provider booking routes read the same Supabase directory. Explore cards open the actual provider ID, with no invented ratings, distance or example professionals. The provider must be approved, available, and have a display name, active category and service area. If none qualify, customers see an empty state. Do not create a demo provider to fill it. SMS/WhatsApp phone verification is deferred; entering a contact number alone does not verify ownership.

### Provider profile photo

Provider Profile → Add/Change photo opens Android Photo Picker. Review the centered preview, then Save photo; Discard leaves the saved photo unchanged. Remove photo clears it. The private shared Storage bucket accepts JPEG uploads up to 1 MB; mobile preparation resizes selected images and strips EXIF/location metadata. The photo is shown on provider home, customer directory/details and admin review. Pending photos remain owner/admin-only; public read requires an approved provider with a complete active service profile. Do not select an arbitrary personal image on behalf of a real user during testing.
