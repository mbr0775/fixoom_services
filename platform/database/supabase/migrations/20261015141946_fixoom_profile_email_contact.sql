-- Profile contacts remain protected by the existing own-account/admin RLS.
alter table public.profiles add column email text;
grant select(email) on public.profiles to authenticated;

create function private.sync_auth_profile_email()
returns trigger language plpgsql security definer set search_path='' as $$
begin
  update public.profiles set email=new.email where id=new.id;
  return new;
end;
$$;
revoke all on function private.sync_auth_profile_email() from public,anon,authenticated;
-- Run after the existing registration trigger has created the profile.
create trigger zz_fixoom_auth_email_sync after insert or update of email on auth.users
  for each row execute function private.sync_auth_profile_email();

update public.profiles p set email=u.email from auth.users u where p.id=u.id;
