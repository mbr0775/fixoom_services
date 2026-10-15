-- One durable workflow shared by Customer, Provider, and Admin.
alter table public.bookings
  add column client_request_id uuid,
  add column provider_name text not null default '',
  add column customer_name text not null default '',
  add column category_name text not null default '',
  add column final_amount numeric(12,2) check (final_amount >= 0 and final_amount <= 10000000),
  add column currency text not null default 'LKR' check (currency = 'LKR'),
  add column direct_payment_received boolean not null default false,
  add column completed_at timestamptz;
create unique index bookings_customer_request_key on public.bookings(customer_id,client_request_id) where client_request_id is not null;

create function private.can_access_booking(p_booking_id uuid)
returns boolean language sql stable security definer set search_path='' as $$
  select exists(select 1 from public.bookings b where b.id=p_booking_id and
    ((b.customer_id=auth.uid() and private.has_role('customer')) or
     (b.provider_id=auth.uid() and private.has_role('provider')) or private.has_role('admin')));
$$;
revoke all on function private.can_access_booking(uuid) from public,anon;
grant execute on function private.can_access_booking(uuid) to authenticated;

create table public.booking_events (
  id uuid primary key default gen_random_uuid(),
  booking_id uuid not null references public.bookings(id) on delete cascade,
  actor_id uuid references public.profiles(id),
  kind text not null,
  created_at timestamptz not null default now()
);
create index booking_events_booking_created_idx on public.booking_events(booking_id,created_at);
create index booking_events_actor_idx on public.booking_events(actor_id);
alter table public.booking_events enable row level security;
revoke all on public.booking_events from anon,authenticated;
grant select on public.booking_events to authenticated;
create policy booking_events_participants_read on public.booking_events for select to authenticated
  using ((select private.can_access_booking(booking_id)));

create function private.record_booking_event() returns trigger
language plpgsql security definer set search_path='' as $$
begin
  if tg_op='INSERT' or new.status is distinct from old.status then
    insert into public.booking_events(booking_id,actor_id,kind) values(new.id,auth.uid(),new.status);
  end if;
  if tg_op='UPDATE' and new.direct_payment_received and not old.direct_payment_received then
    insert into public.booking_events(booking_id,actor_id,kind) values(new.id,auth.uid(),'direct_payment_received');
  end if;
  return new;
end;
$$;
revoke all on function private.record_booking_event() from public,anon,authenticated;
create trigger booking_event_history after insert or update on public.bookings
  for each row execute function private.record_booking_event();

create table public.booking_messages (
  id uuid primary key default gen_random_uuid(),
  booking_id uuid not null references public.bookings(id) on delete cascade,
  sender_id uuid not null references public.profiles(id),
  body text not null check(length(trim(body)) between 1 and 2000),
  created_at timestamptz not null default now()
);
create index booking_messages_booking_created_idx on public.booking_messages(booking_id,created_at);
create index booking_messages_sender_idx on public.booking_messages(sender_id);
alter table public.booking_messages enable row level security;
revoke all on public.booking_messages from anon,authenticated;
grant select on public.booking_messages to authenticated;
create policy booking_messages_participants_read on public.booking_messages for select to authenticated
  using ((select private.can_access_booking(booking_id)));

create function private.save_provider_profile(p_display_name text,p_category_slug text,p_service_area text,p_bio text,p_is_available boolean)
returns public.provider_profiles language plpgsql security definer set search_path='' as $$
declare result public.provider_profiles; state public.account_status;
begin
  select account_status into state from public.profiles where id=auth.uid() and role='provider' for update;
  if state is null or state not in ('pending','active') then raise exception 'Provider profile access required' using errcode='42501'; end if;
  if length(trim(p_display_name)) not between 2 and 100 or length(trim(p_service_area)) not between 2 and 150 or length(p_bio)>2000 then raise exception 'Check your name, service area, and introduction'; end if;
  if not exists(select 1 from public.service_categories where slug=p_category_slug and is_active) then raise exception 'Choose an active service category'; end if;
  update public.provider_profiles set display_name=trim(p_display_name),category_slug=p_category_slug,
    service_area=trim(p_service_area),bio=trim(p_bio),is_available=(p_is_available and state='active'),updated_at=now()
    where id=auth.uid() returning * into result;
  if not found then raise exception 'Service profile not found'; end if;
  return result;
end;
$$;
revoke all on function private.save_provider_profile(text,text,text,text,boolean) from public,anon;
grant execute on function private.save_provider_profile(text,text,text,text,boolean) to authenticated;
create function public.save_provider_profile(p_display_name text,p_category_slug text,p_service_area text,p_bio text,p_is_available boolean)
returns public.provider_profiles language sql security invoker set search_path='' as $$
  select private.save_provider_profile(p_display_name,p_category_slug,p_service_area,p_bio,p_is_available);
$$;
revoke all on function public.save_provider_profile(text,text,text,text,boolean) from public,anon;
grant execute on function public.save_provider_profile(text,text,text,text,boolean) to authenticated;

create function private.create_service_booking(p_request_id uuid,p_provider_id uuid,p_scheduled_at timestamptz,p_address text,p_description text)
returns public.bookings language plpgsql security definer set search_path='' as $$
declare result public.bookings; service public.provider_profiles; category_label text;
begin
  if not private.has_role('customer') then raise exception 'An active customer account is required' using errcode='42501'; end if;
  if p_request_id is null then raise exception 'A booking request key is required'; end if;
  perform pg_advisory_xact_lock(hashtextextended(auth.uid()::text||p_request_id::text,0));
  select * into result from public.bookings where customer_id=auth.uid() and client_request_id=p_request_id;
  if found then
    if result.provider_id<>p_provider_id or result.scheduled_at<>p_scheduled_at or result.service_address<>trim(p_address) or result.problem_description<>trim(p_description) then raise exception 'This request key was used for a different booking'; end if;
    return result;
  end if;
  if p_scheduled_at<=now() then raise exception 'Choose a future service time'; end if;
  select * into service from public.provider_profiles where id=p_provider_id and is_available and private.provider_is_active(id) and service_area<>'';
  select name into category_label from public.service_categories where slug=service.category_slug and is_active;
  if category_label is null then raise exception 'This provider is not available for bookings'; end if;
  insert into public.bookings(customer_id,provider_id,category_slug,scheduled_at,service_address,problem_description,client_request_id,provider_name,customer_name,category_name)
    values(auth.uid(),p_provider_id,service.category_slug,p_scheduled_at,trim(p_address),trim(p_description),p_request_id,service.display_name,
      (select full_name from public.profiles where id=auth.uid()),category_label) returning * into result;
  return result;
end;
$$;
revoke all on function private.create_service_booking(uuid,uuid,timestamptz,text,text) from public,anon;
grant execute on function private.create_service_booking(uuid,uuid,timestamptz,text,text) to authenticated;
create function public.create_service_booking(p_request_id uuid,p_provider_id uuid,p_scheduled_at timestamptz,p_address text,p_description text)
returns public.bookings language sql security invoker set search_path='' as $$
  select private.create_service_booking(p_request_id,p_provider_id,p_scheduled_at,p_address,p_description);
$$;
revoke all on function public.create_service_booking(uuid,uuid,timestamptz,text,text) from public,anon;
grant execute on function public.create_service_booking(uuid,uuid,timestamptz,text,text) to authenticated;

create function private.complete_service_booking(p_booking_id uuid,p_final_amount numeric)
returns public.bookings language plpgsql security definer set search_path='' as $$
declare result public.bookings;
begin
  if not private.has_role('provider') then raise exception 'Active provider access required' using errcode='42501'; end if;
  select * into result from public.bookings where id=p_booking_id and provider_id=auth.uid() for update;
  if not found or result.status<>'in_progress' then raise exception 'Only your in-progress job can be completed' using errcode='42501'; end if;
  if p_final_amount is null or p_final_amount<0 or p_final_amount>10000000 then raise exception 'Enter a valid final amount'; end if;
  update public.bookings set status='completed',final_amount=round(p_final_amount,2),completed_at=now(),updated_at=now() where id=p_booking_id returning * into result;
  return result;
end;
$$;
revoke all on function private.complete_service_booking(uuid,numeric) from public,anon;
grant execute on function private.complete_service_booking(uuid,numeric) to authenticated;
create function public.complete_service_booking(p_booking_id uuid,p_final_amount numeric)
returns public.bookings language sql security invoker set search_path='' as $$select private.complete_service_booking(p_booking_id,p_final_amount);$$;
revoke all on function public.complete_service_booking(uuid,numeric) from public,anon;
grant execute on function public.complete_service_booking(uuid,numeric) to authenticated;

create function private.confirm_direct_payment(p_booking_id uuid)
returns public.bookings language plpgsql security definer set search_path='' as $$
declare result public.bookings;
begin
  if not private.has_role('provider') then raise exception 'Active provider access required' using errcode='42501'; end if;
  select * into result from public.bookings where id=p_booking_id and provider_id=auth.uid() for update;
  if not found or result.status<>'completed' or result.final_amount is null then raise exception 'A completed job with a final amount is required' using errcode='42501'; end if;
  update public.bookings set direct_payment_received=true,updated_at=now() where id=p_booking_id returning * into result;
  return result;
end;
$$;
revoke all on function private.confirm_direct_payment(uuid) from public,anon;
grant execute on function private.confirm_direct_payment(uuid) to authenticated;
create function public.confirm_direct_payment(p_booking_id uuid)
returns public.bookings language sql security invoker set search_path='' as $$select private.confirm_direct_payment(p_booking_id);$$;
revoke all on function public.confirm_direct_payment(uuid) from public,anon;
grant execute on function public.confirm_direct_payment(uuid) to authenticated;

create function private.send_booking_message(p_booking_id uuid,p_body text)
returns public.booking_messages language plpgsql security definer set search_path='' as $$
declare result public.booking_messages;
begin
  if not private.can_access_booking(p_booking_id) or private.has_role('admin') then raise exception 'Booking participant access required' using errcode='42501'; end if;
  insert into public.booking_messages(booking_id,sender_id,body) values(p_booking_id,auth.uid(),trim(p_body)) returning * into result;
  return result;
end;
$$;
revoke all on function private.send_booking_message(uuid,text) from public,anon;
grant execute on function private.send_booking_message(uuid,text) to authenticated;
create function public.send_booking_message(p_booking_id uuid,p_body text)
returns public.booking_messages language sql security invoker set search_path='' as $$select private.send_booking_message(p_booking_id,p_body);$$;
revoke all on function public.send_booking_message(uuid,text) from public,anon;
grant execute on function public.send_booking_message(uuid,text) to authenticated;
