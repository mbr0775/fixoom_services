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

Use one provider account for all genuine skills. New registration collects 10–2,000 characters of experience/offering details for each selected service (up to 12), and stores every selection as a pending request. After email confirmation/sign-in, descriptions can be updated from **Profile → Skills & services**. Older requests with sparse details remain reviewable by the administrator, with a clear warning in the confirmation dialog; approving them never invents a description. An inactive category must be enabled before it can be approved.

In Admin → Providers, **Skills & offerings** is a separate, paginated review queue. Approve/reject each request, or suspend an approved offering. Account approval alone never publishes an unreviewed skill. Approving a skill alone does not activate a pending/suspended account. Each service decision is audited and appears in Activity. No timer or focus refresh was added.

Once the account and at least one active service are approved, complete the service area and enable availability. Customers discover the same provider in every approved category. The booking form asks for the service and reserved time. All skills use the provider's shared schedule: overlapping accepted/in-progress reservations are blocked by the backend. Old clients use the approved primary service with a 60-minute reservation. Customers still pay providers directly after service.

Profile editing can choose a primary service only from approved categories. Removing an approved skill hides it from new bookings; adding it again requires review. Existing accepted work can still progress and remains in history. This version requires removing/resubmitting to change an approved offering's description. General profile introduction and offering descriptions are separate.

The provider app keeps its light theme and extends the existing depth design with raised icon headers, decorative layers, scroll parallax and short screen transitions across skills, jobs/schedule, booking conversations/details, receipts and authentication screens. Motion follows system settings, lifecycle and battery saver. Text/inputs remain stable for reading and editing.

Validation: both debug APKs and all 50 unit tests passed. All 21 provider UI checks and both customer directory checks passed on Honor NIC-LX2 (including multi-select registration, offering validation/removal confirmation and discovery by an approved secondary skill). All six SQL policy suites passed with rolled-back fixtures. Admin build/TypeScript/lint and browser checks passed, including skill-review confirmation, failed-write recovery, no idle/focus refresh and responsive widths of 1440/1024/390/360. Updated customer and provider APKs were installed with accounts retained. No fictional provider or operational demo data was added.

The migration is applied to shared project `mzkrxovluljvsqzbowwb` and mirrored in both local database folders. Vercel project inspection still returns 403 for the previously specified team, so this update's hosted admin deployment is not verified. Publishing source to GitHub does not itself prove a Vercel deployment succeeded.


## Booking alerts, nearby search and optional live tracking — October 16, 2026

Bookings and participant replies now create a durable, recipient-only Supabase notification inbox. Realtime updates refresh the open Android apps; a 30-second foreground recovery refresh handles missed connections. Notification taps open the protected booking. A customer cannot read another customer's job, messages or position. The admin retains explicit refresh; no interval/focus reload was added.

Firebase project `fixoom-services-live-077-519b8` (display name `fixoom-services-live-0775`) supplies Android push only. Customer package `com.fixoom.customer` and provider package `com.fixoom.provider` are registered. Supabase `mzkrxovluljvsqzbowwb` remains the only business database and authentication service. The Google account used for this Firebase setup is `fizoomservices@gmail.com`; this does not change the official Fixoom contact or Supabase account.

The deployed `dispatch-booking-alerts` Edge Function authenticates a server-only dispatch key, claims recipient/device jobs, signs an FCM HTTP v1 OAuth assertion and records actual delivery acceptance or retry/failure. A dedicated FCM-only service account is encrypted in Supabase Vault as `fixoom_fcm_service_account`; `fixoom_push_dispatch_key` is also encrypted. Neither credential is stored in client code or Git. The private outbox retries network errors with backoff and retires invalid tokens. Post-commit dispatch plus the `fixoom-booking-push-retry` cron job work without a running mobile app. Success means Google accepted a send, not proof a phone displayed it. Notifications now carry the real inbox event title and booking context (reference, service and relevant participant name); opening the job still requires its authorized session. Addresses, phone numbers, email addresses and chat text are excluded from push content.

On each phone, sign in to the matching app and open Notifications → Allow phone notifications. Device registrations are tied to the authenticated role and installation; sign-out unregisters/deletes the token best-effort, account changes replace the old registration, and foreground message handling verifies the recipient. OS permission, channel settings, connectivity and device battery restrictions can affect display. The sender's live authenticated configuration check returned HTTP 200 with no queued jobs; provider background delivery was subsequently confirmed with a clearly labelled direct FCM test: Google accepted the send and Android posted the notification while the provider app was backgrounded. This test created no booking or inbox record. A real unread provider booking alert was subsequently replayed through the deployed outbox/Edge sender: it reached sent state without error, and Android held the exact saved event title and booking body. Both customer and provider devices are now registered. A new customer-side reply notification has not yet been separately observed on the phone.

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

### Phone notification setup verification

Both Honor app packages now have Android notification permission enabled. The provider registered one enabled device in the private backend registry. A direct FCM alert labelled “Fixoom notification test” was accepted by Google (HTTP 200) and its matching provider notification record was observed in Android. This verifies provider transport/display posting, not the complete booking-trigger/outbox flow or that the user read the alert. Both customer and provider registrations are now present. A real provider booking alert was later resent through the actual outbox/Edge pipeline, with its exact saved event title/body confirmed in Android; this is separate from the earlier direct FCM setup test. No personal service-base coordinates were selected or published by the agent.

### Detailed booking notifications

The sender now uses `claim_booking_push_v2` to load each claimed job's actual saved inbox title/body/kind. Real requests show New service request with the customer, service and booking reference. Accept/start/complete/decline/payment updates identify their actual event and booking. New message alerts identify the real sender and service/reference, while the conversation text stays inside the authenticated app. Foreground/realtime and background FCM alerts use this same durable notification source. Client request parameters cannot supply or replace the recipient, title or body. The legacy claim RPC is retained for compatibility and both claim APIs remain service-role-only.

Android alerts have a monochrome Fixoom F icon and expandable body. Sound/vibration/high priority are requested subject to the phone's channel, silent mode and notification/privacy settings. A brief top banner is the phone's heads-up presentation; the notification remains in the notification shade until opened/dismissed. No arbitrary custom overlay or full-screen interruption is used for ordinary bookings.

Detailed-alert validation: both debug APKs built and were installed preserving accounts/permissions; all 50 unit tests, four Deno sender tests and the three affected rollback SQL suites passed. The latest real unread provider request was deliberately resent once (no new business record or fictional booking); the deployed queue/sender recorded a successful second attempt, and the matching Android notification contained the exact saved event title and booking body. The notification drawer was opened on the Honor for user inspection. Existing private-table deny-by-default RLS notices and the disabled leaked-password-protection advisory are unchanged.

## Booking conflicts and provider history

The booking form now reads confirmed busy intervals for the selected Sri Lanka date through `provider_busy_times`. It shows actual reservations, prevents submission of an overlapping choice, and offers Use next unreserved time. This moves the selected time only when tapped; it does not submit a booking. The availability endpoint returns timestamps only to active booking accounts, with no other customer's identity, address or booking reference. Requested jobs do not reserve time. Adjacent reservations are allowed, including intervals crossing midnight; the existing atomic server conflict check remains authoritative if another request is accepted during booking. Failed checks have a Retry action and never present an unchecked date as free.

Known backend failures now have safe, specific explanations: a busy provider, unavailable service, expired session or an already-used request key. Raw SDK responses and database details remain private. Editing the form clears old action feedback. Retrying unchanged submitted details retains the same idempotency key.

The provider dashboard adds History in bottom navigation and Request history under work actions. It opens All saved jobs by default; filters include Awaiting reply, Active, Completed, Cancelled and Declined. New requests explains that accepted jobs move to Active jobs and remain in history. Existing records are retained and details still open their saved conversation/timeline. The current list remains bounded to the latest 200 records and states that limit honestly.

Verification: both APKs built, all 54 unit tests and seven targeted provider device checks passed. A new rollback-only SQL suite verifies requested/accepted/completed availability, anonymous denial, cross-midnight reservations, private booking isolation, adjacent booking and preserved completion history. The real provider History screen displayed the existing accepted job on Honor. No additional operational booking was created during QA.

On the real Honor customer app, the existing 09:00 reservation produced the conflict panel. Tapping Use next unreserved time cleared the conflict and enabled Send booking request for the adjacent time. The form was inspected without submitting another operational booking.

## October 17: logout and booking refresh recovery

Provider logout previously cleared the transient authentication result while the request was in progress. Dashboard interpreted that as a missing session, started session restoration and cancelled logout. Logout now retains its current route until completion, and Dashboard only initiates restoration when no auth request or terminal result owns navigation. The shared repository attempts notification cleanup and current-session server revocation with bounded waits, and always removes this phone's persisted session, including on network failure or cancellation. This signs out this installation; it does not promise successful remote revocation when offline. In-flight push registration is cancelled before unregistering the device, registration is suppressed while signing out, and existing app notifications are cleared. The customer's shared marketplace state clears private cached data immediately after successful local logout, even when the following public reload fails; snapshot responses started under another account are discarded. Logout feedback says Signed out rather than Changes saved.

Booking details previously combined booking, history, messages and live-location reads into one failing batch. Details now publish the saved booking as soon as its read succeeds. History, conversation and location have independent bounded reads and honest loading/error states; successful sections remain visible, failed sections offer refresh, and a failed location read never displays an old coordinate as current or claims the provider stopped sharing. Primary failures label retained details as previously loaded, and an inaccessible/deleted booking clears its details. A refresh requested during a read is coalesced into a follow-up request instead of being dropped; switching bookings cancels and invalidates the old request.

Validation: both debug APKs and instrumentation APKs compile. All 62 unit tests and 11 targeted Honor UI checks pass, including auth failure/idempotency, provider route preservation, customer cached-state removal after a failed reload, independent section recovery, queued refresh, access loss, switching bookings, location error/loading states, availability and dashboard navigation. Both updated apps were installed preserving account data. The real customer's existing request opened successfully on the updated app without the refresh error. Read-only REST calls returned HTTP 200 for its booking, events, messages and location endpoints; authenticated rollback-only database reads also verified customer and assigned-provider access. The screenshot's requested booking exists; no operational booking, message, status or location was created/modified to test these fixes. Local evidence: `Documentation/connected-workflow/oct17-verified-build.log` and `oct17-device-tests.log`. On the follow-up phone check, the provider was signed out, its stored session was absent, and a cold restart returned to the signed-out welcome screen; the customer session remained present. The earlier manual UI check had been deferred while the phone was in use.

## Signup service review — October 17, 2026

Provider registration now collects a separate 10–2,000-character experience/offering description for every selected service, with up to 12 selections. The shared Auth repository validates these drafts before signup and sends only selected descriptions. The signup trigger stores normalized, bounded descriptions as pending requests; legacy clients remain supported, malformed descriptions do not become approved facts, and metadata never grants account or service approval.

For older requests such as an empty description or a short years-of-experience value, admin approval is an explicit administrator decision rather than a disabled-button dead end. The admin shows missing-details guidance and a warning in the confirmation dialog. The existing role gate, profile lock order and audit entry remain in place. Approval never fabricates service experience or approves the account. Inactive categories cannot be newly approved. Customer visibility still requires an active account, an approved active skill, complete profile/area and enabled availability.

Both live migrations, `20261017042841_signup_skill_descriptions` and `20261017043528_admin_skill_review_readiness`, and the new `signup_skill_descriptions.sql` rollback suite are mirrored in the local platform database directory. No existing provider request or status was changed to test approval. The isolated suite covers fresh descriptions, old signup compatibility, malformed metadata, admin review of a sparse legacy request, provider self-approval denial, inactive categories and customer visibility before/after the separate approvals.

Verification: both debug APKs built and were installed on Honor NIC-LX2; all 63 unit tests pass. All 33 distinct targeted device checks passed across the broader run and the corrected three-check registration/skills retry, covering multi-service signup, description validation, profiles/photos, booking/tracking/availability, navigation, motion and video. The initial registration test caught a missing keyboard Done action; the field and test now pass. All 10 SQL suites ran against the shared database with every fixture rolled back; all four push-sender tests pass. The admin production build/TypeScript, full ESLint and complete isolated browser suite pass at four responsive widths. Test evidence is in `Documentation/connected-workflow/approval-*` and admin `artifacts/approval-qa`; browser images are fixtures, never production data. The real empty signup form was inspected without creating an account. Vercel team inspection still returns 403, so a GitHub push must not be described as a verified hosted deployment. The existing leaked-password-protection advisory remains ([guidance](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection)); private push-table no-policy notices are intentional deny-by-default controls.
