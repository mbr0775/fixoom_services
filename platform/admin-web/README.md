# Fixoom Admin

Fresh Next.js 16 admin application for the Fixoom customer and provider apps.

## Identity

- Administrator login: the account designated by the workspace owner
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

Record lists show the most recent 100 rows. Filters and search apply to the loaded rows. Counts use exact database counts. Refresh reloads from Supabase; returning to the browser window also refreshes. Realtime subscriptions and server pagination are future work.

## GitHub and Vercel

The Android GitHub repository is `mbr0775/fixoom_services`. Admin source is prepared under `platform/admin-web`, with database migrations and tests in `platform/database`. Set the Vercel project root directory to `platform/admin-web`, framework to Next.js, Node.js to 22.x or 24.x, and configure the two variables in `.env.example`. Use an authorized Vercel team; do not deploy another project's directory.

## Checks

- `npm run build`
- `npm run lint`
- `npm audit --omit=dev`
- Database access and workflow test: `../database/tests/platform_access.sql`

The database test creates synthetic identities inside a transaction and rolls everything back. It verifies provider registration, rejection of self-assigned admin roles, admin review, row isolation, booking workflow, and anonymous access.

## Delivery boundary

The Android apps are installed on the Honor phone. Their existing customer booking UI still uses its in-memory BookingStore and sample providers. Provider dashboard job/profile actions still require wiring to the new shared database APIs. This admin application and database foundation do not establish payment processing, messaging, push notifications, or a completed production marketplace.

## Remaining release checks

The Supabase advisor reports leaked-password protection disabled. Review [Supabase password protection](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection) before production release. No schema/RLS warnings were reported after the foundation migration.

The production dependency audit is clean. The development lint toolchain still reports the upstream braces/fast-glob advisory; a forced downgrade of Next.js ESLint configuration was not applied. See [the upstream advisory](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm). These tools are not production dependencies.
