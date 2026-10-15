# Fixoom Admin

Fresh Next.js 16 admin application for the Fixoom customer and provider apps.

## Identity

- Administrator login: mubassirnasar@gmail.com
- Official app email: fixoomservices@gmail.com
- Supabase project: mzkrxovluljvsqzbowwb
- Display timezone: Asia/Colombo

The confirmed administrator account has been provisioned in the live database. Sign in using that account's existing Supabase Auth password. This application does not store or ship an administrator password.

## Local development

Use Node.js 22 or 24. Run `npm ci`, copy `.env.example` to `.env.local`, fill in the public Supabase URL and key, and run `npm run dev`. Never use a service-role key in a NEXT_PUBLIC variable. `.env.local` is ignored by Git.

## Implemented

- Supabase password sign-in, verified identity, active-admin access checks, sign-out
- Live overview counts and recent activity
- Provider approval, rejection, suspension, and audit history
- Customer account list and booking details/cancellation
- Service category enable/pause controls
- Responsive layouts and loading, empty, error and confirmation states
- Teal and lime visual identity shared with the Android apps, with a responsive sidebar, keyboard focus, reduced motion, and password visibility control
- Database pagination and search across providers, customers, bookings, and activity
- Provider/customer profile dialogs, booking details, and administrator activity details
- Current-page CSV exports with quoted values and spreadsheet formula protection
- Booking status distribution and active service catalog summaries using actual records

Record lists use 20-row database pages with exact counts. Name/phone search is available for accounts; booking search covers reference, address, and problem description; activity search covers action names. Filters apply across records, rather than just the current page. CSV exports contain only the current page. Overview totals are exact; the booking pipeline is explicitly limited to the latest 100 bookings. Refresh reloads from Supabase; returning to the browser window also refreshes. Realtime subscriptions are future work.

Navigation sections use URL fragments, so refreshing or using browser history can restore the selected workspace section.

## GitHub and Vercel

The Android GitHub repository is `mbr0775/fixoom_services`. Admin source is prepared under `platform/admin-web`, with database migrations and tests in `platform/database`. Set the Vercel project root directory to `platform/admin-web`, framework to Next.js, Node.js to 22.x or 24.x, and configure the two variables in `.env.example`. Use an authorized Vercel team; do not deploy another project's directory.

## Checks

- `npm run build`
- `npm run lint`
- `npm audit --omit=dev`
- Database access and workflow test: `../database/tests/platform_access.sql`
- Browser UI verification: `node scripts/verify-admin.cjs` with a running local server and Playwright available. Set `FIXOOM_PLAYWRIGHT_PATH` to a Playwright module directory if it is not installed locally. Optional `FIXOOM_QA_URL` defaults to `http://localhost:3010`; `FIXOOM_QA_OUTPUT` defaults to ignored `artifacts/admin-qa`.

The browser script intercepts Supabase requests with synthetic fixtures. It checks access denial, login errors, password visibility, pagination, search, record details, confirmation before writes, write failures, CSV formula safety, catalog controls, activity, and all seven sections at 1440, 1024, 390, and 360 px. It does not create production users or bypass authentication in the application. Dashboard screenshots produced by this script are design evidence using test data, not live business metrics. Real administrator password sign-in must still be exercised by the account owner.

The database test creates synthetic identities inside a transaction and rolls everything back. It verifies provider registration, rejection of self-assigned admin roles, admin review, row isolation, booking workflow, and anonymous access.

## Delivery boundary

The Android apps are installed on the Honor phone. Their existing customer booking UI still uses its in-memory BookingStore and sample providers. Provider dashboard job/profile actions still require wiring to the new shared database APIs. This admin application and database foundation do not establish payment processing, messaging, push notifications, or a completed production marketplace.

## Remaining release checks

The Supabase advisor reports leaked-password protection disabled. Review [Supabase password protection](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection) before production release. No schema/RLS warnings were reported after the foundation migration.

The production dependency audit is clean. The development lint toolchain still reports the upstream braces/fast-glob advisory; a forced downgrade of Next.js ESLint configuration was not applied. See [the upstream advisory](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm). These tools are not production dependencies.

## October 15, 2026 admin delivery

Production build, TypeScript, lint, production dependency audit, and the browser fixture checks passed. Live anonymous API checks confirmed that the active service catalog is readable and profiles, bookings, and audit records are denied. The existing database contains one active administrator and six service categories.

The Vercel connector still returns 403 for `team_VSN9rXJSYJNd8d0PUVCKuybJ` (`mubassir-nasars-projects`). No hosted Fixoom admin deployment is claimed. Run the local production preview with `npm run start -- --port 3010` after building.
