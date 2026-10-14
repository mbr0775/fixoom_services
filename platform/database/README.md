# Fixoom database foundation

These migrations extend the existing Fixoom Supabase project `mzkrxovluljvsqzbowwb` and its existing `profiles`, `app_role`, `account_status`, and profile timestamp trigger. They are not an initial schema for an empty Supabase project.

The account-state and foundation migrations have been applied to the live project. Keep the enum-state migration separate: Postgres requires added enum values to commit before the foundation can use them.

The foundation implements pending provider registration, public service categories and approved provider directory entries, database-authorized admin review with audit records, and customer/provider booking ownership and state transitions. Frontends use only a public Supabase key and the authenticated user's session.

Run `tests/platform_access.sql` as a database administrator. It creates synthetic identities, exercises the access policies and workflows using the authenticated and anonymous Postgres roles, then rolls all fixtures back. It does not create persistent test accounts or send email.

Android data integration is still in progress. Customer `BookingStore` and sample provider lists have not yet been replaced by these database APIs.
