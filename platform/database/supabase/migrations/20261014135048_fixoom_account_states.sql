-- Commit enum additions before using their values in the foundation migration.
alter type public.account_status add value if not exists 'pending';
alter type public.account_status add value if not exists 'rejected';
