-- Busy intervals only: never disclose another customer's booking identity or details.
create function private.provider_busy_times(p_provider_id uuid,p_day date)
returns table(starts_at timestamptz,ends_at timestamptz)
language plpgsql stable security definer set search_path='' as $$
declare day_start timestamptz;day_end timestamptz;
begin
 if not (private.has_role('customer') or private.has_role('admin') or (private.has_role('provider') and auth.uid()=p_provider_id)) then
  raise exception 'Active booking account required' using errcode='42501';end if;
 if p_day is null or p_provider_id is null then raise exception 'Choose a provider and service date';end if;
 if not private.provider_is_active(p_provider_id) or not private.provider_has_approved_skill(p_provider_id,null) then
  raise exception 'This provider is not available for this service';end if;
 day_start:=p_day::timestamp at time zone 'Asia/Colombo';
 day_end:=(p_day+1)::timestamp at time zone 'Asia/Colombo';
 return query select b.scheduled_at,b.scheduled_at+make_interval(mins=>b.duration_minutes)
 from public.bookings b where b.provider_id=p_provider_id and b.status in ('accepted','in_progress')
 and b.scheduled_at<day_end and b.scheduled_at+make_interval(mins=>b.duration_minutes)>day_start
 order by b.scheduled_at;
end;$$;
revoke all on function private.provider_busy_times(uuid,date) from public,anon,authenticated;
grant execute on function private.provider_busy_times(uuid,date) to authenticated;
create function public.provider_busy_times(p_provider_id uuid,p_day date)
returns table(starts_at timestamptz,ends_at timestamptz)
language sql security invoker set search_path='' as $$select * from private.provider_busy_times(p_provider_id,p_day);$$;
revoke all on function public.provider_busy_times(uuid,date) from public,anon,authenticated;
grant execute on function public.provider_busy_times(uuid,date) to authenticated;
