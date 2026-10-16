-- Durable recipient inbox and consent-based service discovery / active-job location.
create table public.notifications (
 id uuid primary key default gen_random_uuid(),
 recipient_id uuid not null references public.profiles(id) on delete cascade,
 actor_id uuid references public.profiles(id) on delete set null,
 booking_id uuid not null references public.bookings(id) on delete cascade,
 kind text not null, title text not null, body text not null,
 created_at timestamptz not null default now(),read_at timestamptz
);
create index notifications_recipient_created_idx on public.notifications(recipient_id,created_at desc);
create index notifications_booking_idx on public.notifications(booking_id);
create index notifications_actor_idx on public.notifications(actor_id);
alter table public.notifications enable row level security;
revoke all on public.notifications from public,anon,authenticated;
grant select on public.notifications to authenticated;
create policy notifications_own_read on public.notifications for select to authenticated using (
 recipient_id=(select auth.uid()) and private.can_access_booking(booking_id));
create function private.notify_booking_change() returns trigger language plpgsql security definer set search_path='' as $$
declare booking public.bookings; headline text; event_kind text;
begin
 if tg_table_name='booking_messages' then
   select * into booking from public.bookings where id=new.booking_id;
   insert into public.notifications(recipient_id,actor_id,booking_id,kind,title,body)
   values(case when new.sender_id=booking.customer_id then booking.provider_id else booking.customer_id end,new.sender_id,booking.id,'message','New booking message','Open your booking to read the message.');
 else
   booking:=new;
   if tg_op='UPDATE' and new.status is not distinct from old.status and new.direct_payment_received is not distinct from old.direct_payment_received then return new;end if;
   event_kind:=case when tg_op='UPDATE' and new.direct_payment_received and not old.direct_payment_received then 'direct_payment_received' else new.status end;
   headline:=case event_kind when 'requested' then 'New service request' when 'accepted' then 'Booking accepted' when 'in_progress' then 'Service started' when 'completed' then 'Service completed' when 'cancelled' then 'Booking cancelled' when 'rejected' then 'Booking declined' when 'direct_payment_received' then 'Payment receipt recorded' else 'Booking updated' end;
   insert into public.notifications(recipient_id,actor_id,booking_id,kind,title,body)
   select recipient,auth.uid(),booking.id,event_kind,headline,booking.reference||' / '||booking.category_name
   from unnest(array[booking.customer_id,booking.provider_id]) recipient;
 end if;
 return new;
end;$$;
revoke all on function private.notify_booking_change() from public,anon,authenticated;
create trigger booking_notifications after insert or update on public.bookings for each row execute function private.notify_booking_change();
create trigger message_notifications after insert on public.booking_messages for each row execute function private.notify_booking_change();
create function private.mark_notification_read(p_id uuid) returns void language plpgsql security definer set search_path='' as $$
begin
 if auth.uid() is null then raise exception 'Sign in required' using errcode='42501';end if;
 update public.notifications set read_at=coalesce(read_at,now()) where recipient_id=auth.uid() and (p_id is null or id=p_id) and private.can_access_booking(booking_id);
end;$$;
revoke all on function private.mark_notification_read(uuid) from public,anon,authenticated;
grant execute on function private.mark_notification_read(uuid) to authenticated;
create function public.mark_notification_read(p_id uuid default null) returns void language sql security invoker set search_path='' as $$select private.mark_notification_read(p_id);$$;
revoke all on function public.mark_notification_read(uuid) from public,anon,authenticated;
grant execute on function public.mark_notification_read(uuid) to authenticated;

-- Exact business base coordinates are owner/admin-only. Discovery returns distance, never coordinates.
create table public.provider_service_locations (
 provider_id uuid primary key references public.provider_profiles(id) on delete cascade,
 latitude double precision not null check(latitude between -90 and 90),longitude double precision not null check(longitude between -180 and 180),
 radius_km integer not null default 25 check(radius_km between 1 and 100),updated_at timestamptz not null default now()
);
alter table public.provider_service_locations enable row level security;
revoke all on public.provider_service_locations from public,anon,authenticated;
grant select on public.provider_service_locations to authenticated;
create policy service_location_owner_admin on public.provider_service_locations for select to authenticated using (provider_id=(select auth.uid()) or (select private.has_role('admin')));
create function private.set_service_location(p_latitude double precision,p_longitude double precision,p_radius_km integer) returns void language plpgsql security definer set search_path='' as $$
begin
 perform id from public.profiles where id=auth.uid() and role='provider' and account_status in ('pending','active') for update;
 if not found then raise exception 'Provider access required' using errcode='42501';end if;
 if p_latitude is null and p_longitude is null then delete from public.provider_service_locations where provider_id=auth.uid();return;end if;
 if p_latitude is null or p_longitude is null or p_radius_km is null or not(p_latitude between -90 and 90 and p_longitude between -180 and 180 and p_radius_km between 1 and 100) then raise exception 'Invalid service location';end if;
 insert into public.provider_service_locations(provider_id,latitude,longitude,radius_km) values(auth.uid(),p_latitude,p_longitude,p_radius_km)
 on conflict(provider_id) do update set latitude=excluded.latitude,longitude=excluded.longitude,radius_km=excluded.radius_km,updated_at=now();
end;$$;
revoke all on function private.set_service_location(double precision,double precision,integer) from public,anon,authenticated;
grant execute on function private.set_service_location(double precision,double precision,integer) to authenticated;
create function public.set_service_location(p_latitude double precision,p_longitude double precision,p_radius_km integer default 25) returns void language sql security invoker set search_path='' as $$select private.set_service_location(p_latitude,p_longitude,p_radius_km);$$;
revoke all on function public.set_service_location(double precision,double precision,integer) from public,anon,authenticated;
grant execute on function public.set_service_location(double precision,double precision,integer) to authenticated;
create function private.nearby_providers(p_latitude double precision,p_longitude double precision,p_radius_km integer) returns jsonb language plpgsql stable security definer set search_path='' as $$
declare result jsonb;
begin
 if p_latitude is null or p_longitude is null or p_radius_km is null or not(p_latitude between -90 and 90 and p_longitude between -180 and 180 and p_radius_km between 1 and 100) then raise exception 'Invalid discovery location';end if;
 select coalesce(jsonb_agg(rowdata order by distance_km),'[]') into result from (
 select to_jsonb(pp)||jsonb_build_object('distance_km',round(d.km::numeric,1),'provider_skills',
 (select coalesce(jsonb_agg(to_jsonb(s)),'[]') from public.provider_skills s join public.service_categories c on c.slug=s.category_slug where s.provider_id=pp.id and s.status='approved' and c.is_active)) rowdata,d.km distance_km
 from public.provider_profiles pp join public.provider_service_locations l on l.provider_id=pp.id
 cross join lateral (select 6371*acos(least(1.0,greatest(-1.0,sin(radians(p_latitude))*sin(radians(l.latitude))+cos(radians(p_latitude))*cos(radians(l.latitude))*cos(radians(p_longitude-l.longitude)))))) d(km)
 where pp.is_available and trim(pp.display_name)<>'' and trim(pp.service_area)<>'' and private.provider_is_active(pp.id) and private.provider_has_approved_skill(pp.id,null) and d.km<=least(p_radius_km,l.radius_km) order by d.km,pp.id limit 500
 ) results;
 return result;
end;$$;
revoke all on function private.nearby_providers(double precision,double precision,integer) from public,anon,authenticated;
grant execute on function private.nearby_providers(double precision,double precision,integer) to anon,authenticated;
create function public.nearby_providers(p_latitude double precision,p_longitude double precision,p_radius_km integer default 50) returns jsonb language sql security invoker set search_path='' as $$select private.nearby_providers(p_latitude,p_longitude,p_radius_km);$$;
revoke all on function public.nearby_providers(double precision,double precision,integer) from public,anon,authenticated;
grant execute on function public.nearby_providers(double precision,double precision,integer) to anon,authenticated;

-- Voluntary live job position. Stale positions expire after five minutes and are cleared on closure.
create table public.booking_locations (
 booking_id uuid primary key references public.bookings(id) on delete cascade,
 provider_id uuid not null references public.provider_profiles(id) on delete cascade,
 latitude double precision not null check(latitude between -90 and 90),longitude double precision not null check(longitude between -180 and 180),
 accuracy_m double precision not null check(accuracy_m between 0 and 10000),updated_at timestamptz not null default now()
);
create index booking_locations_provider_idx on public.booking_locations(provider_id);
alter table public.booking_locations enable row level security;
revoke all on public.booking_locations from public,anon,authenticated;
grant select on public.booking_locations to authenticated;
create policy active_booking_location_read on public.booking_locations for select to authenticated using (
 private.can_access_booking(booking_id) and updated_at>now()-interval '5 minutes' and exists(select 1 from public.bookings b where b.id=booking_id and b.status in ('accepted','in_progress')));
create function private.share_booking_location(p_booking_id uuid,p_latitude double precision,p_longitude double precision,p_accuracy_m double precision) returns void language plpgsql security definer set search_path='' as $$
begin
 perform id from public.bookings where id=p_booking_id and provider_id=auth.uid() and status in ('accepted','in_progress') and private.has_role('provider') for update;
 if not found then raise exception 'Active assigned provider required' using errcode='42501';end if;
 if p_latitude is null and p_longitude is null then delete from public.booking_locations where booking_id=p_booking_id;return;end if;
 if p_latitude is null or p_longitude is null or p_accuracy_m is null or not(p_latitude between -90 and 90 and p_longitude between -180 and 180 and p_accuracy_m between 0 and 10000) then raise exception 'Invalid job location';end if;
 insert into public.booking_locations(booking_id,provider_id,latitude,longitude,accuracy_m) values(p_booking_id,auth.uid(),p_latitude,p_longitude,p_accuracy_m)
 on conflict(booking_id) do update set latitude=excluded.latitude,longitude=excluded.longitude,accuracy_m=excluded.accuracy_m,updated_at=now();
end;$$;
revoke all on function private.share_booking_location(uuid,double precision,double precision,double precision) from public,anon,authenticated;
grant execute on function private.share_booking_location(uuid,double precision,double precision,double precision) to authenticated;
create function public.share_booking_location(p_booking_id uuid,p_latitude double precision,p_longitude double precision,p_accuracy_m double precision default 0) returns void language sql security invoker set search_path='' as $$select private.share_booking_location(p_booking_id,p_latitude,p_longitude,p_accuracy_m);$$;
revoke all on function public.share_booking_location(uuid,double precision,double precision,double precision) from public,anon,authenticated;
grant execute on function public.share_booking_location(uuid,double precision,double precision,double precision) to authenticated;
create function private.clear_closed_job_location() returns trigger language plpgsql security definer set search_path='' as $$begin
 if new.status not in ('accepted','in_progress') then delete from public.booking_locations where booking_id=new.id;end if;return new;
end;$$;
revoke all on function private.clear_closed_job_location() from public,anon,authenticated;
create trigger clear_closed_job_location after update of status on public.bookings for each row execute function private.clear_closed_job_location();
-- Postgres Changes obey the recipient/participant RLS policies above.
alter publication supabase_realtime add table public.notifications,public.booking_locations;
