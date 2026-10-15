# Fixoom Android and shared backend

- This repository contains Customer (`app`, `com.fixoom.customer`), Provider (`provider`, `com.fixoom.provider`), shared backend code (`core`) and connected Compose screens (`marketplace-ui`).
- The admin panel lives in the separate `mbr0775/fixoom_admin` repository.
- Customer, Provider and Admin **must use one shared Supabase database**. The user reaffirmed this on 2026-10-15; retain it in future work.
- Required Supabase project: `mzkrxovluljvsqzbowwb`, URL `https://mzkrxovluljvsqzbowwb.supabase.co`.
- User-designated Supabase account / official email: `fixoomservices@gmail.com`. Administrator login: `mubassirnasar@gmail.com`.
- Both mobile apps read the same ignored root `local.properties`. The admin local and Vercel environment variables must use this same project. Separate authenticated sessions and role permissions are expected; separate production databases are not.
- Do not create or switch to another Supabase project unless the user explicitly changes this requirement. Shared migrations/tests belong in `platform/database`; these extend the existing project baseline.
- Never commit real keys/passwords or include service-role credentials in a client. The public project URL is safe in configuration examples.
- Build both apps after changes to shared modules. See `docs/CONNECTED_WORKFLOW.md` for setup, verification and release limitations.

## Production requirement — no demo behavior

The user reaffirmed on 2026-10-15 that Customer, Provider and Admin are for real production use. No user-facing demo mode, fictional providers/reviews/ratings/bookings/receipts/statistics, simulated writes, authentication bypass or in-memory replacement for backend persistence is permitted. Use real shared Supabase data and real device/user actions; show honest empty/error states when data or operations are unavailable. Isolated test fixtures belong only in test tooling and must never be rendered as operational data or seeded into the production database. Report unfinished integrations and release prerequisites accurately; do not claim production readiness before verification.

## Standing GitHub publishing instruction

The user authorized on 2026-10-15: after every completed change and its appropriate checks, inspect the diff, commit the task's changes and push to `mbr0775/fixoom_services` without asking again. Use the current intended branch (`main` unless explicitly changed). Admin changes belong in `mbr0775/fixoom_admin`. Preserve unrelated work, exclude credentials/build outputs, never force-push, and verify/report push success or failure. Include updated shared migrations/tests in `platform/database` when changing the backend.
