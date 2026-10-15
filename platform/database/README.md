# Fixoom connected database

These migrations extend the existing Supabase project `mzkrxovluljvsqzbowwb` and its pre-existing `profiles`, `app_role`, `account_status` and profile timestamp trigger. They are **not an initial schema for an empty project**. All four migrations in this directory have been applied to the existing live project. Do not apply them again manually to that project.

The versioned source is `platform/database` on `main` in [fixoom_services](https://github.com/mbr0775/fixoom_services). The local workspace's `Fixoom_Platform/database` is the working source used for this delivery; keep both copies synchronized when making future migrations. The admin repository is separate.

## Migrations

1. Account states: commit added enum values before the foundation uses them.
2. Platform foundation: pending provider registration, service catalog, approved directory, admin review/audit and booking ownership/status rules.
3. Connected workflow: editable professional profiles, idempotent booking requests, identity snapshots, participant-only conversations, event history, final LKR amounts and recorded direct payments.
4. Customer moderation: administrator-only suspend/restore with audit history.

Android and web clients use a public key plus the signed-in user's session. Server-side role, account-state and ownership checks authorize operations; RLS protects participant data. Booking events/messages cannot be inserted directly by clients. Admins can inspect a conversation but cannot send messages impersonating its participants.

Payment is directly between customer and provider. `confirm_direct_payment` records the assigned provider's acknowledgment of receipt after completion; it does not process or verify a financial transfer. The new clients complete jobs through `complete_service_booking`, including the agreed final amount. The original status RPC remains available for compatibility with the foundation contract.

## Verification

Run `tests/platform_access.sql` and `tests/connected_workflow.sql` as a database administrator. Both create synthetic identities inside a transaction and roll back all fixtures. They do not send email or leave test accounts behind.

The original test verifies registration, protection against self-assigned admin roles, administrator review, ownership isolation, booking lifecycle and anonymous access. The connected test adds pending profile setup, availability, idempotency, final amount and receipt handling, participant chat, event history, customer moderation and restricted-account isolation. Both passed against the configured project on October 15, 2026.

The Supabase security advisor reports no schema/RLS warnings for this delivery. Leaked-password protection remains disabled; review [Supabase password protection](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection) before public release.
