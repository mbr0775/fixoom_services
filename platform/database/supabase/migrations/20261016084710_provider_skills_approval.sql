-- Multiple real service offerings per account. New requests always require an admin decision.
create table public.provider_skills (
  provider_id uuid not null references public.provider_profiles(id) on delete cascade,
  category_slug text not null references public.service_categories(slug),
  description text not null default '' check(length(description)<=2000),
  status text not null default 'pending' check(status in ('pending','approved','rejected','suspended','withdrawn')),
  requested_at timestamptz not null default now(),
  reviewed_at timestamptz,
  updated_at timestamptz not null default now(),
  primary key(provider_id,category_slug)
);
create index provider_skills_review_idx on public.provider_skills(status,requested_at);
create index provider_skills_category_idx on public.provider_skills(category_slug,status,provider_id);
alter table public.provider_skills enable row level security;
revoke all on public.provider_skills from public,anon,authenticated;
grant select on public.provider_skills to anon,authenticated;
alter table public.provider_profiles add column approved_categories text[] not null default '{}';
-- Preserve the currently published category of existing active providers; no new skill is auto-approved.
insert into public.provider_skills(provider_id,category_slug,description,status,reviewed_at)
select pp.id,pp.category_slug,pp.bio,case when p.account_status='active' then 'approved' else 'pending' end,
  case when p.account_status='active' then now() else null end
from public.provider_profiles pp join public.profiles p on p.id=pp.id
where pp.category_slug is not null;

create function private.provider_has_approved_skill(p_provider_id uuid,p_category_slug text)
returns boolean language sql stable security definer set search_path='' as $$
 select exists(select 1 from public.provider_skills s join public.service_categories c on c.slug=s.category_slug
 where s.provider_id=p_provider_id and s.status='approved' and c.is_active
 and (p_category_slug is null or s.category_slug=p_category_slug));
$$;
revoke all on function private.provider_has_approved_skill(uuid,text) from public,anon,authenticated;
grant execute on function private.provider_has_approved_skill(uuid,text) to anon,authenticated;
create policy skills_public_read on public.provider_skills for select to anon,authenticated using (
 status='approved' and private.provider_is_active(provider_id) and
 exists(select 1 from public.service_categories c where c.slug=category_slug and c.is_active) and
 exists(select 1 from public.provider_profiles pp where pp.id=provider_id and trim(pp.service_area)<>''));
create policy skills_owner_admin_read on public.provider_skills for select to authenticated using (
 provider_id=(select auth.uid()) or (select private.has_role('admin')));
drop policy providers_directory_read on public.provider_profiles;
create policy providers_directory_read on public.provider_profiles for select to anon,authenticated using (
 private.provider_is_active(id) and private.provider_has_approved_skill(id,null) and trim(service_area)<>'');
-- All provider mutations use owner-validating RPCs; direct category edits cannot bypass review.
revoke update(display_name,category_slug,service_area,bio,is_available) on public.provider_profiles from authenticated;

create function private.sync_provider_skills() returns trigger language plpgsql security definer set search_path='' as $$
declare target uuid:=coalesce(new.provider_id,old.provider_id); categories text[];
begin
 perform id from public.provider_profiles where id=target for update;
 select coalesce(array_agg(category_slug order by category_slug),'{}') into categories
 from public.provider_skills where provider_id=target and status='approved';
 update public.provider_profiles set approved_categories=categories,
 category_slug=case when category_slug=any(categories) then category_slug else categories[1] end,
 is_available=case when cardinality(categories)=0 then false else is_available end,updated_at=now() where id=target;
 return null;
end;$$;
revoke all on function private.sync_provider_skills() from public,anon,authenticated;
create trigger sync_provider_skills after insert or update or delete on public.provider_skills
for each row execute function private.sync_provider_skills();
update public.provider_profiles pp set approved_categories=(select coalesce(array_agg(s.category_slug order by s.category_slug),'{}') from public.provider_skills s where s.provider_id=pp.id and s.status='approved');
update public.provider_profiles set category_slug=approved_categories[1],is_available=false where cardinality(approved_categories)=0;

create function private.request_provider_skill(p_category_slug text,p_description text)
returns public.provider_skills language plpgsql security definer set search_path='' as $$
declare result public.provider_skills; account public.profiles;
begin
 select * into account from public.profiles where id=auth.uid() and role='provider' for update;
 if account.id is null or account.account_status not in ('pending','active') then raise exception 'Provider access required' using errcode='42501';end if;
 if p_description is null or length(trim(p_description)) not between 10 and 2000 then raise exception 'Describe your experience and the service you offer (10-2000 characters)';end if;
 if not exists(select 1 from public.service_categories where slug=p_category_slug and is_active) then raise exception 'Choose an active service category';end if;
 select * into result from public.provider_skills where provider_id=account.id and category_slug=p_category_slug for update;
 if found and result.status='approved' then raise exception 'This skill is already approved';end if;
 insert into public.provider_skills(provider_id,category_slug,description,status)
 values(account.id,p_category_slug,trim(p_description),'pending') on conflict(provider_id,category_slug)
 do update set description=excluded.description,status='pending',requested_at=now(),reviewed_at=null,updated_at=now() returning * into result;
 return result;
end;$$;
revoke all on function private.request_provider_skill(text,text) from public,anon,authenticated;
grant execute on function private.request_provider_skill(text,text) to authenticated;
create function public.request_provider_skill(p_category_slug text,p_description text) returns public.provider_skills
language sql security invoker set search_path='' as $$select private.request_provider_skill(p_category_slug,p_description);$$;
revoke all on function public.request_provider_skill(text,text) from public,anon,authenticated;
grant execute on function public.request_provider_skill(text,text) to authenticated;

create function private.review_provider_skill(p_provider_id uuid,p_category_slug text,p_decision text)
returns void language plpgsql security definer set search_path='' as $$
declare previous text;
begin
 if not private.has_role('admin') then raise exception 'Administrator access required' using errcode='42501';end if;
 if p_decision is null or p_decision not in ('approved','rejected','suspended') then raise exception 'Invalid skill decision';end if;
 -- Use the same lock order as provider requests and profile edits.
 perform id from public.profiles where id=p_provider_id and role='provider' and account_status<>'deleted' for update;
 if not found then raise exception 'Provider not found';end if;
 select status into previous from public.provider_skills where provider_id=p_provider_id and category_slug=p_category_slug for update;
 if previous is null or previous='withdrawn' then raise exception 'Requested skill not found';end if;
 if p_decision='approved' and exists(select 1 from public.provider_skills where provider_id=p_provider_id and category_slug=p_category_slug and length(trim(description))<10) then raise exception 'The provider must describe this service before approval';end if;
 if previous=p_decision then return;end if;
 update public.provider_skills set status=p_decision,reviewed_at=now(),updated_at=now() where provider_id=p_provider_id and category_slug=p_category_slug;
 insert into public.admin_audit_log(actor_id,action,subject_id,details) values(auth.uid(),'provider_skill_review',p_provider_id,
 jsonb_build_object('category_slug',p_category_slug,'previous_status',previous,'decision',p_decision));
end;$$;
revoke all on function private.review_provider_skill(uuid,text,text) from public,anon,authenticated;
grant execute on function private.review_provider_skill(uuid,text,text) to authenticated;
create function public.review_provider_skill(p_provider_id uuid,p_category_slug text,p_decision text) returns void
language sql security invoker set search_path='' as $$select private.review_provider_skill(p_provider_id,p_category_slug,p_decision);$$;
revoke all on function public.review_provider_skill(uuid,text,text) from public,anon,authenticated;
grant execute on function public.review_provider_skill(uuid,text,text) to authenticated;

create function private.withdraw_provider_skill(p_category_slug text) returns void language plpgsql security definer set search_path='' as $$
begin
 perform id from public.profiles where id=auth.uid() and role='provider' and account_status in ('pending','active') for update;
 if not found then raise exception 'Provider access required' using errcode='42501';end if;
 update public.provider_skills set status='withdrawn',updated_at=now() where provider_id=auth.uid() and category_slug=p_category_slug;
 if not found then raise exception 'Skill not found';end if;
end;$$;
revoke all on function private.withdraw_provider_skill(text) from public,anon,authenticated;
grant execute on function private.withdraw_provider_skill(text) to authenticated;
create function public.withdraw_provider_skill(p_category_slug text) returns void language sql security invoker set search_path='' as $$select private.withdraw_provider_skill(p_category_slug);$$;
revoke all on function public.withdraw_provider_skill(text) from public,anon,authenticated;
grant execute on function public.withdraw_provider_skill(text) to authenticated;

-- Signup metadata is an untrusted request, never an approval or authorization claim.
create function private.register_initial_skills() returns trigger language plpgsql security definer set search_path='' as $$
declare choices jsonb;
begin
 select raw_user_meta_data->'skill_categories' into choices from auth.users where id=new.id;
 if jsonb_typeof(choices)='array' then
   insert into public.provider_skills(provider_id,category_slug,description)
   select new.id,c.slug,'' from public.service_categories c
   where c.is_active and c.slug in (select jsonb_array_elements_text(choices)) limit 12;
 end if;
 return new;
end;$$;
revoke all on function private.register_initial_skills() from public,anon,authenticated;
create trigger register_initial_skills after insert on public.provider_profiles for each row execute function private.register_initial_skills();

create or replace function private.save_provider_profile(p_display_name text,p_category_slug text,p_service_area text,p_bio text,p_is_available boolean)
returns public.provider_profiles language plpgsql security definer set search_path='' as $$
declare result public.provider_profiles; state public.account_status; selected text:=nullif(trim(p_category_slug),'');
begin
 select account_status into state from public.profiles where id=auth.uid() and role='provider' for update;
 if state is null or state not in ('pending','active') then raise exception 'Provider profile access required' using errcode='42501';end if;
 if p_display_name is null or p_service_area is null or p_bio is null or length(trim(p_display_name)) not between 2 and 100 or length(trim(p_service_area)) not between 2 and 150 or length(p_bio)>2000 then raise exception 'Check your name, service area, and introduction';end if;
 if selected is not null and not private.provider_has_approved_skill(auth.uid(),selected) then
   -- Older app versions can submit a first category, but it remains a pending request.
   if not exists(select 1 from public.service_categories where slug=selected and is_active) then raise exception 'Choose an active service category';end if;
   insert into public.provider_skills(provider_id,category_slug,description) values(auth.uid(),selected,trim(p_bio)) on conflict(provider_id,category_slug) do nothing;
   selected:=null;
 end if;
 update public.provider_profiles set display_name=trim(p_display_name),
 category_slug=coalesce(selected,category_slug),service_area=trim(p_service_area),bio=trim(p_bio),
 is_available=(p_is_available and state='active' and private.provider_has_approved_skill(auth.uid(),null)),updated_at=now()
 where id=auth.uid() returning * into result;
 if not found then raise exception 'Service profile not found';end if;
 return result;
end;$$;

alter table public.bookings add column duration_minutes integer not null default 60 check(duration_minutes between 15 and 480);
create function private.guard_skill_booking() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if tg_op='INSERT' or (new.status='accepted' and old.status<>'accepted') then
   perform id from public.provider_profiles where id=new.provider_id for update;
   if not private.provider_is_active(new.provider_id) or not private.provider_has_approved_skill(new.provider_id,new.category_slug) then
     raise exception 'This service is not approved for this provider';end if;
   if exists(select 1 from public.bookings b where b.provider_id=new.provider_id and b.id<>new.id and b.status in ('accepted','in_progress')
      and tstzrange(b.scheduled_at,b.scheduled_at+make_interval(mins=>b.duration_minutes),'[)') &&
          tstzrange(new.scheduled_at,new.scheduled_at+make_interval(mins=>new.duration_minutes),'[)')) then
     raise exception 'This provider already has an accepted job during that time';end if;
 end if;
 return new;
end;$$;
revoke all on function private.guard_skill_booking() from public,anon,authenticated;
create trigger guard_skill_booking before insert or update of status on public.bookings for each row execute function private.guard_skill_booking();

create function private.create_service_booking_for_skill(p_request_id uuid,p_provider_id uuid,p_category_slug text,p_scheduled_at timestamptz,p_address text,p_description text,p_duration_minutes integer)
returns public.bookings language plpgsql security definer set search_path='' as $$
declare result public.bookings; service public.provider_profiles; category_label text;
begin
 if not private.has_role('customer') then raise exception 'An active customer account is required' using errcode='42501';end if;
 if p_request_id is null then raise exception 'A booking request key is required';end if;
 if p_duration_minutes is null or p_duration_minutes not between 15 and 480 then raise exception 'Choose a valid reserved duration';end if;
 perform pg_advisory_xact_lock(hashtextextended(auth.uid()::text||p_request_id::text,0));
 select * into result from public.bookings where customer_id=auth.uid() and client_request_id=p_request_id;
 if found then
  if result.provider_id is distinct from p_provider_id or result.category_slug is distinct from p_category_slug or result.duration_minutes is distinct from p_duration_minutes or result.scheduled_at is distinct from p_scheduled_at or result.service_address is distinct from trim(p_address) or result.problem_description is distinct from trim(p_description) then raise exception 'This request key was used for a different booking';end if;
  return result;
 end if;
 if p_scheduled_at is null or p_scheduled_at<=now() then raise exception 'Choose a future service time';end if;
 select * into service from public.provider_profiles where id=p_provider_id for update;
 if service.id is null or not service.is_available or trim(service.service_area)='' or not private.provider_is_active(p_provider_id) or not private.provider_has_approved_skill(p_provider_id,p_category_slug) then raise exception 'This provider is not available for this service';end if;
 select name into category_label from public.service_categories where slug=p_category_slug and is_active;
 if category_label is null then raise exception 'Choose an active service category';end if;
 insert into public.bookings(customer_id,provider_id,category_slug,scheduled_at,service_address,problem_description,client_request_id,provider_name,customer_name,category_name,duration_minutes)
 values(auth.uid(),p_provider_id,p_category_slug,p_scheduled_at,trim(p_address),trim(p_description),p_request_id,service.display_name,
 (select full_name from public.profiles where id=auth.uid()),category_label,p_duration_minutes) returning * into result;
 return result;
end;$$;
revoke all on function private.create_service_booking_for_skill(uuid,uuid,text,timestamptz,text,text,integer) from public,anon,authenticated;
grant execute on function private.create_service_booking_for_skill(uuid,uuid,text,timestamptz,text,text,integer) to authenticated;
create function public.create_service_booking_for_skill(p_request_id uuid,p_provider_id uuid,p_category_slug text,p_scheduled_at timestamptz,p_address text,p_description text,p_duration_minutes integer default 60)
returns public.bookings language sql security invoker set search_path='' as $$select private.create_service_booking_for_skill(p_request_id,p_provider_id,p_category_slug,p_scheduled_at,p_address,p_description,p_duration_minutes);$$;
revoke all on function public.create_service_booking_for_skill(uuid,uuid,text,timestamptz,text,text,integer) from public,anon,authenticated;
grant execute on function public.create_service_booking_for_skill(uuid,uuid,text,timestamptz,text,text,integer) to authenticated;
-- Old clients still book their approved primary category through the same protected path.
create or replace function private.create_service_booking(p_request_id uuid,p_provider_id uuid,p_scheduled_at timestamptz,p_address text,p_description text)
returns public.bookings language sql security invoker set search_path='' as $$
 select private.create_service_booking_for_skill(p_request_id,p_provider_id,(select category_slug from public.provider_profiles where id=p_provider_id),p_scheduled_at,p_address,p_description,60);$$;
create or replace function private.submit_booking(provider_id uuid,scheduled_at timestamptz,service_address text,problem_description text)
returns public.bookings language sql security invoker set search_path='' as $$
 select private.create_service_booking(gen_random_uuid(),provider_id,scheduled_at,service_address,problem_description);$$;

create or replace function private.can_read_provider_photo(object_path text)
returns boolean language sql stable security definer set search_path='' as $$
 select private.has_role('admin') or exists(select 1 from public.provider_profiles pp join public.profiles p on p.id=pp.id
 where pp.avatar_path=object_path and p.role='provider' and p.account_status='active'
 and private.provider_has_approved_skill(pp.id,null) and trim(pp.display_name)<>'' and trim(pp.service_area)<>'');$$;
