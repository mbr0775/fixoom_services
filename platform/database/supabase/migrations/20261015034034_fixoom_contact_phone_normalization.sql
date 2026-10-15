-- Preserve submitted contact numbers using the same rules as the mobile apps.
create function private.normalize_contact_phone(input_phone text)
returns text language plpgsql immutable security invoker set search_path='' as $$
declare cleaned text := regexp_replace(trim(coalesce(input_phone,'')), '[[:space:]().-]', '', 'g');
begin
  if cleaned like '00%' then cleaned := '+' || substr(cleaned,3);
  elsif cleaned ~ '^0[1-9][0-9]{8}$' then cleaned := '+94' || substr(cleaned,2);
  elsif cleaned ~ '^94[1-9][0-9]{8}$' then cleaned := '+' || cleaned;
  end if;
  if cleaned ~ '^[+][1-9][0-9]{7,14}$' then return cleaned; end if;
  return null;
end;
$$;
revoke all on function private.normalize_contact_phone(text) from public,anon,authenticated;

-- The original constraint escaped the leading plus twice. Use a character class.
alter table public.profiles drop constraint profiles_phone_e164;
alter table public.profiles add constraint profiles_phone_e164
  check (phone is null or phone ~ '^[+][1-9][0-9]{7,14}$');

-- Contact numbers are not login identities. Different email accounts may share
-- a business/family number, including the customer's and provider's accounts.
drop index public.profiles_phone_unique;
create index profiles_phone_idx on public.profiles(phone) where phone is not null;

create or replace function private.handle_new_auth_user()
returns trigger language plpgsql security definer set search_path='' as $$
declare signup_role public.app_role;
begin
  signup_role := case when new.raw_user_meta_data->>'role'='provider'
    then 'provider'::public.app_role else 'customer'::public.app_role end;
  insert into public.profiles(id,role,account_status,full_name,phone)
  values(new.id,signup_role,
    case when signup_role='provider' then 'pending'::public.account_status else 'active'::public.account_status end,
    left(coalesce(trim(new.raw_user_meta_data->>'full_name'),''),100),
    private.normalize_contact_phone(new.raw_user_meta_data->>'phone'));
  if signup_role='provider' then
    insert into public.provider_profiles(id,display_name)
    values(new.id,left(coalesce(trim(new.raw_user_meta_data->>'full_name'),''),100));
  end if;
  return new;
end;
$$;
revoke all on function private.handle_new_auth_user() from public,anon,authenticated;

-- Recover only missing contacts from the user's original registration metadata.
-- Never replace a phone the user has already saved in their profile.
update public.profiles p
set phone=private.normalize_contact_phone(u.raw_user_meta_data->>'phone')
from auth.users u
where p.id=u.id and (p.phone is null or trim(p.phone)='')
  and private.normalize_contact_phone(u.raw_user_meta_data->>'phone') is not null;
