# Fixoom connected database

These migrations extend the existing Supabase project `mzkrxovluljvsqzbowwb` and its pre-existing `profiles`, `app_role`, `account_status` and profile timestamp trigger. They are **not an initial schema for an empty project**. All eight migrations in this directory have been applied to the existing live project. Do not apply them again manually to that project.

The versioned source is `platform/database` on `main` in [fixoom_services](https://github.com/mbr0775/fixoom_services). The local workspace's `Fixoom_Platform/database` is the working source used for this delivery; keep both copies synchronized when making future migrations. The admin repository is separate.

## Migrations

1. Account states: commit added enum values before the foundation uses them.
2. Platform foundation: pending provider registration, service catalog, approved directory, admin review/audit and booking ownership/status rules.
3. Connected workflow: editable professional profiles, idempotent booking requests, identity snapshots, participant-only conversations, event history, final LKR amounts and recorded direct payments.
4. Customer moderation: administrator-only suspend/restore with audit history.
5. Contact phone normalization: accept Sri Lankan local and international formats, store E.164, fix the leading-plus constraint, and recover missing profile contacts from original signup metadata. Contact numbers can be shared between email accounts; email/session identity and role checks remain unchanged.
6. Profile email contacts: mirror authoritative Auth email into the private profile on registration and email changes, including existing accounts. Own-account/admin RLS protects this contact field; clients cannot overwrite it directly, and the public provider directory does not expose it.

7. Provider profile photos: a private JPEG-only bucket (1 MB), ownership checks, approved-directory read access, and a validated photo RPC synchronize private/profile directory paths. Clients cannot set arbitrary photo URLs or overwrite immutable photo objects.

Android and web clients use a public key plus the signed-in user's session. Server-side role, account-state and ownership checks authorize operations; RLS protects participant data. Booking events/messages cannot be inserted directly by clients. Admins can inspect a conversation but cannot send messages impersonating its participants.

Payment is directly between customer and provider. `confirm_direct_payment` records the assigned provider's acknowledgment of receipt after completion; it does not process or verify a financial transfer. The new clients complete jobs through `complete_service_booking`, including the agreed final amount. The original status RPC remains available for compatibility with the foundation contract.

## Verification

Run `tests/platform_access.sql` and `tests/connected_workflow.sql` as a database administrator. Both create synthetic identities inside a transaction and roll back all fixtures. They do not send email or leave test accounts behind.

`tests/contact_phone.sql` additionally checks local/international normalization, signup persistence, shared contacts and authenticated edit validation. Its fixtures also roll back. All three suites passed after the contact repair.

`tests/profile_email.sql` verifies email synchronization, administrator visibility, own-account access, anonymous denial and rejection of direct client email edits. All four SQL suites use rollback-only fixtures.

The original test verifies registration, protection against self-assigned admin roles, administrator review, ownership isolation, booking lifecycle and anonymous access. The connected test adds pending profile setup, availability, idempotency, final amount and receipt handling, participant chat, event history, customer moderation and restricted-account isolation. Both passed against the configured project on October 15, 2026.

The Supabase security advisor reports no schema/RLS warnings for this delivery. Leaked-password protection remains disabled; review [Supabase password protection](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection) before public release.

`tests/provider_photos.sql` checks provider-owned uploads and publication, pending-photo privacy, customer/admin access, denial of arbitrary URLs and overwrites, suspension and removal. It uses rollback-only metadata fixtures (no files or users remain). All five SQL suites passed after the photo migration. The Storage API stores actual images; SQL tests validate authorization only.


## Multiple provider skills — October 16, 2026

Migration `20261016084710_provider_skills_approval.sql` is applied to the same shared project. `provider_skills` stores one offering per provider/category, with a description and independent pending/approved/rejected/suspended/withdrawn status. Signup selections create pending requests only. Active accounts must still receive administrator approval for every new offering. Existing active providers retain their previously published primary service; additional services are never auto-approved.

Owner-validating RPCs handle requests/withdrawal; `review_provider_skill` is admin-only and audited. Clients cannot directly write approval/category fields. The public directory, skill descriptions, photos and booking creation require an active account and approved active category. Availability also requires a completed service area. Private contact email stays in protected profiles. Removing a skill preserves existing job history.

Customers book an explicit approved skill using `create_service_booking_for_skill`, with a reserved duration of 15–480 minutes (the current app offers common durations). The provider has one shared schedule across all skills. Backend row locks serialize creation/acceptance; overlapping accepted/in-progress reservations are rejected. Requested jobs can overlap until one is accepted. Old clients are routed through the same approval checks, with a 60-minute default duration. Durations reserve time; changing an accepted booking's duration is not yet supported.

All six SQL suites passed against the live schema using rollback-only fixtures: account access, connected lifecycle, contact phone, private email, photos and the new `tests/provider_skills.sql`. The new suite covers signup metadata, self-approval denial, account/skill separation, explicit category booking, idempotency, overlap rejection, withdrawal and completion of an existing job after skill suspension. No synthetic users/bookings remain. Security advisor: no schema/RLS warnings; existing disabled leaked-password protection remains. Performance advisor reports permissive-policy overlap and currently unused indexes; these are monitoring items, not evidence of failed access isolation.
