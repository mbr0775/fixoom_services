create schema if not exists private;
revoke all on schema private from public, anon;
grant usage on schema private to authenticated;

-- Look up the caller in a protected table, never user-editable JWT metadata.
create function private.has_role(required_role public.app_role)
returns boolean language sql stable security definer set search_path = '' as $$
  select auth.uid() is not null and exists (
    select 1 from public.profiles where id = (select auth.uid())
      and role = required_role and account_status = 'active'
  );
$$;
revoke all on function private.has_role(public.app_role) from public, anon;
grant execute on function private.has_role(public.app_role) to authenticated;

create policy profiles_admin_read on public.profiles for select to authenticated
  using ((select private.has_role('admin')));

create table public.service_categories (
  slug text primary key,
  name text not null check (length(name) between 2 and 60),
  description text not null default '',
  is_active boolean not null default true,
  sort_order integer not null default 0
);
alter table public.service_categories enable row level security;
revoke all on public.service_categories from anon, authenticated;
grant select on public.service_categories to anon, authenticated;
grant update (name, description, is_active, sort_order) on public.service_categories to authenticated;
create policy categories_public_read on public.service_categories for select to anon, authenticated
  using (is_active);
create policy categories_admin_read on public.service_categories for select to authenticated
  using ((select private.has_role('admin')));
create policy categories_admin_update on public.service_categories for update to authenticated
  using ((select private.has_role('admin'))) with check ((select private.has_role('admin')));
insert into public.service_categories (slug, name, description, sort_order) values
  ('plumbing', 'Plumbing', 'Repairs, installations and water systems', 1),
  ('electrical', 'Electrical', 'Wiring, lighting and electrical repairs', 2),
  ('carpentry', 'Carpentry', 'Furniture, fittings and woodwork', 3),
  ('painting', 'Painting', 'Interior and exterior painting', 4),
  ('masonry', 'Masonry', 'Building repairs and masonry work', 5),
  ('cleaning', 'Cleaning', 'Home and commercial cleaning', 6);

create table public.provider_profiles (
  id uuid primary key references public.profiles(id) on delete cascade,
  display_name text not null default '' check (length(display_name) <= 100),
  category_slug text references public.service_categories(slug),
  service_area text not null default '' check (length(service_area) <= 150),
  bio text not null default '' check (length(bio) <= 2000),
  is_available boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index provider_profiles_category_idx on public.provider_profiles(category_slug);
alter table public.provider_profiles enable row level security;
revoke all on public.provider_profiles from anon, authenticated;
grant select on public.provider_profiles to anon, authenticated;
grant update (display_name, category_slug, service_area, bio, is_available) on public.provider_profiles to authenticated;

create function private.provider_is_active(provider_id uuid)
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (select 1 from public.profiles
    where id = provider_id and role = 'provider' and account_status = 'active');
$$;
revoke all on function private.provider_is_active(uuid) from public;
grant execute on function private.provider_is_active(uuid) to anon, authenticated;
create policy providers_directory_read on public.provider_profiles for select to anon, authenticated
  using (private.provider_is_active(id) and category_slug is not null and service_area <> '');
create policy providers_own_read on public.provider_profiles for select to authenticated
  using (id = (select auth.uid()) or (select private.has_role('admin')));
create policy providers_own_update on public.provider_profiles for update to authenticated
  using (id = (select auth.uid()) and (select private.has_role('provider')))
  with check (id = (select auth.uid()) and (select private.has_role('provider')));

-- Registration can choose customer or pending provider. It cannot grant admin access.
create function private.handle_new_auth_user()
returns trigger language plpgsql security definer set search_path = '' as $$
declare signup_role public.app_role;
begin
  signup_role := case when new.raw_user_meta_data ->> 'role' = 'provider'
    then 'provider'::public.app_role else 'customer'::public.app_role end;
  insert into public.profiles (id, role, account_status, full_name, phone)
  values (new.id, signup_role,
    case when signup_role = 'provider' then 'pending'::public.account_status
      else 'active'::public.account_status end,
    left(coalesce(trim(new.raw_user_meta_data ->> 'full_name'), ''), 100),
    case when new.raw_user_meta_data ->> 'phone' ~ '^\+[1-9][0-9]{7,14}$'
      then new.raw_user_meta_data ->> 'phone' else null end);
  if signup_role = 'provider' then
    insert into public.provider_profiles (id, display_name)
    values (new.id, left(coalesce(trim(new.raw_user_meta_data ->> 'full_name'), ''), 100));
  end if;
  return new;
end;
$$;
revoke all on function private.handle_new_auth_user() from public, anon, authenticated;
drop trigger on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
  for each row execute function private.handle_new_auth_user();
-- Retain the old function for migration compatibility, but remove client execution.
revoke all on function public.handle_new_auth_user() from public, anon, authenticated;

-- Backfill profiles for existing confirmed or unconfirmed Auth users without changing roles.
insert into public.profiles (id, full_name)
select id, left(coalesce(trim(raw_user_meta_data ->> 'full_name'), ''), 100)
from auth.users on conflict (id) do nothing;
insert into public.provider_profiles (id, display_name)
select id, full_name from public.profiles where role = 'provider' on conflict (id) do nothing;

create table public.admin_audit_log (
  id uuid primary key default gen_random_uuid(),
  actor_id uuid references public.profiles(id),
  action text not null,
  subject_id uuid not null,
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);
create index admin_audit_actor_idx on public.admin_audit_log(actor_id);
alter table public.admin_audit_log enable row level security;
revoke all on public.admin_audit_log from anon, authenticated;
grant select on public.admin_audit_log to authenticated;
create policy audit_admin_read on public.admin_audit_log for select to authenticated
  using ((select private.has_role('admin')));

create function private.review_provider(provider_id uuid, decision text)
returns void language plpgsql security definer set search_path = '' as $$
declare previous_status public.account_status;
begin
  if not private.has_role('admin') then raise exception 'Administrator access required' using errcode = '42501'; end if;
  if decision not in ('active', 'rejected', 'suspended') then raise exception 'Invalid review decision'; end if;
  select account_status into previous_status from public.profiles
    where id = provider_id and role = 'provider' for update;
  if not found then raise exception 'Provider not found'; end if;
  if previous_status = 'deleted' then raise exception 'Deleted providers cannot be reviewed'; end if;
  update public.profiles set account_status = decision::public.account_status where id = provider_id;
  insert into public.admin_audit_log (actor_id, action, subject_id, details)
  values (auth.uid(), 'provider_review', provider_id,
    jsonb_build_object('previous_status', previous_status, 'decision', decision));
end;
$$;
revoke all on function private.review_provider(uuid,text) from public, anon;
grant execute on function private.review_provider(uuid,text) to authenticated;
create function public.review_provider(provider_id uuid, decision text)
returns void language sql security invoker set search_path = '' as $$
  select private.review_provider(provider_id, decision);
$$;
revoke all on function public.review_provider(uuid,text) from public, anon;
grant execute on function public.review_provider(uuid,text) to authenticated;

create table public.bookings (
  id uuid primary key default gen_random_uuid(),
  reference text not null unique default ('FX-' || upper(substr(gen_random_uuid()::text, 1, 8))),
  customer_id uuid not null references public.profiles(id),
  provider_id uuid not null references public.provider_profiles(id),
  category_slug text not null references public.service_categories(slug),
  scheduled_at timestamptz not null,
  service_address text not null check (length(trim(service_address)) between 5 and 500),
  problem_description text not null check (length(trim(problem_description)) between 5 and 4000),
  status text not null default 'requested' check (status in ('requested','accepted','in_progress','completed','cancelled','rejected')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (customer_id <> provider_id)
);
create index bookings_customer_created_idx on public.bookings(customer_id, created_at desc);
create index bookings_provider_schedule_idx on public.bookings(provider_id, scheduled_at);
create index bookings_category_idx on public.bookings(category_slug);
alter table public.bookings enable row level security;
revoke all on public.bookings from anon, authenticated;
grant select on public.bookings to authenticated;
create policy bookings_authorized_read on public.bookings for select to authenticated
  using ((customer_id = (select auth.uid()) and (select private.has_role('customer')))
    or (provider_id = (select auth.uid()) and (select private.has_role('provider')))
    or (select private.has_role('admin')));

create function private.submit_booking(provider_id uuid, scheduled_at timestamptz, service_address text, problem_description text)
returns public.bookings language plpgsql security definer set search_path = '' as $$
declare result public.bookings; provider_category text;
begin
  if not private.has_role('customer') then raise exception 'An active customer account is required' using errcode='42501'; end if;
  if scheduled_at <= now() then raise exception 'Choose a future service time'; end if;
  select pp.category_slug into provider_category from public.provider_profiles pp
  join public.service_categories c on c.slug = pp.category_slug and c.is_active
  where pp.id = provider_id and pp.is_available and pp.service_area <> '' and private.provider_is_active(pp.id);
  if provider_category is null then raise exception 'This provider is not available for bookings'; end if;
  insert into public.bookings (customer_id, provider_id, category_slug, scheduled_at, service_address, problem_description)
  values (auth.uid(), provider_id, provider_category, scheduled_at, trim(service_address), trim(problem_description)) returning * into result;
  return result;
end;
$$;
revoke all on function private.submit_booking(uuid,timestamptz,text,text) from public, anon;
grant execute on function private.submit_booking(uuid,timestamptz,text,text) to authenticated;
create function public.submit_booking(provider_id uuid, scheduled_at timestamptz, service_address text, problem_description text)
returns public.bookings language sql security invoker set search_path = '' as $$
  select private.submit_booking(provider_id, scheduled_at, service_address, problem_description);
$$;
revoke all on function public.submit_booking(uuid,timestamptz,text,text) from public, anon;
grant execute on function public.submit_booking(uuid,timestamptz,text,text) to authenticated;

create function private.change_booking_status(booking_id uuid, next_status text)
returns public.bookings language plpgsql security definer set search_path = '' as $$
declare result public.bookings; allowed boolean := false;
begin
  if auth.uid() is null then raise exception 'Sign in required' using errcode='42501'; end if;
  select * into result from public.bookings where id = booking_id for update;
  if not found then raise exception 'Booking not found'; end if;
  if result.status in ('completed','cancelled','rejected') then raise exception 'This booking is already closed'; end if;
  if private.has_role('admin') then
    allowed := next_status = 'cancelled';
  elsif result.customer_id = auth.uid() and private.has_role('customer') then
    allowed := next_status = 'cancelled' and result.status in ('requested','accepted');
  elsif result.provider_id = auth.uid() and private.has_role('provider') then
    allowed := (result.status = 'requested' and next_status in ('accepted','rejected'))
      or (result.status = 'accepted' and next_status = 'in_progress')
      or (result.status = 'in_progress' and next_status = 'completed');
  end if;
  if not allowed then raise exception 'This status change is not allowed' using errcode='42501'; end if;
  update public.bookings set status = next_status, updated_at = now() where id = booking_id returning * into result;
  if private.has_role('admin') then
    insert into public.admin_audit_log (actor_id,action,subject_id,details)
    values (auth.uid(),'booking_cancelled',booking_id,jsonb_build_object('status',next_status));
  end if;
  return result;
end;
$$;
revoke all on function private.change_booking_status(uuid,text) from public, anon;
grant execute on function private.change_booking_status(uuid,text) to authenticated;
create function public.change_booking_status(booking_id uuid, next_status text)
returns public.bookings language sql security invoker set search_path = '' as $$
  select private.change_booking_status(booking_id, next_status);
$$;
revoke all on function public.change_booking_status(uuid,text) from public, anon;
grant execute on function public.change_booking_status(uuid,text) to authenticated;
