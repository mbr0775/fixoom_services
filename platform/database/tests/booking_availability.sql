-- Real policies/functions. All synthetic identities, locations and notifications roll back.
begin;
select set_config('fixoom.flow.provider',gen_random_uuid()::text,true);
select set_config('fixoom.flow.customer',gen_random_uuid()::text,true);
select set_config('fixoom.flow.stranger',gen_random_uuid()::text,true);
select set_config('fixoom.flow.admin',(select id::text from public.profiles where role='admin' and account_status='active' limit 1),true);
insert into auth.users(id,email,raw_user_meta_data) values
(current_setting('fixoom.flow.provider')::uuid,'fixoom-flow-provider@example.invalid','{"role":"provider","full_name":"Flow professional"}'),
(current_setting('fixoom.flow.customer')::uuid,'fixoom-flow-customer@example.invalid','{"role":"customer","full_name":"Flow customer"}'),
(current_setting('fixoom.flow.stranger')::uuid,'fixoom-flow-stranger@example.invalid','{"role":"customer","full_name":"Other customer"}');
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.provider'),true);
select public.request_provider_skill('plumbing','Permission fixture plumbing offering');
select public.save_provider_profile('Flow professional','','Colombo','Fixture service description',false);
select public.set_service_location(6.9271,79.8612,25);
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.admin'),true);
select public.review_provider(current_setting('fixoom.flow.provider')::uuid,'active');
select public.review_provider_skill(current_setting('fixoom.flow.provider')::uuid,'plumbing','approved');
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.provider'),true);
select public.save_provider_profile('Flow professional','plumbing','Colombo','Fixture service description',true);
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.customer'),true);
select set_config('fixoom.availability.start',((((now() at time zone 'Asia/Colombo')::date+3)::timestamp+interval '23 hours 30 minutes') at time zone 'Asia/Colombo')::text,true);
select set_config('fixoom.flow.booking',(public.create_service_booking_for_skill(gen_random_uuid(),current_setting('fixoom.flow.provider')::uuid,'plumbing',current_setting('fixoom.availability.start')::timestamptz,'Test address','Availability fixture',60)).id::text,true);
do $$begin
 if exists(select 1 from public.provider_busy_times(current_setting('fixoom.flow.provider')::uuid,(current_setting('fixoom.availability.start')::timestamptz at time zone 'Asia/Colombo')::date)) then raise exception 'Unaccepted request blocks calendar';end if;
end$$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.provider'),true);
select public.change_booking_status(current_setting('fixoom.flow.booking')::uuid,'accepted');
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.stranger'),true);
do $$declare day date; slots jsonb;begin
 day:=(current_setting('fixoom.availability.start')::timestamptz at time zone 'Asia/Colombo')::date;
 if (select count(*) from public.provider_busy_times(current_setting('fixoom.flow.provider')::uuid,day))<>1 or
    (select count(*) from public.provider_busy_times(current_setting('fixoom.flow.provider')::uuid,day+1))<>1 then raise exception 'Reservation across midnight is missing';end if;
 select jsonb_agg(to_jsonb(t)) into slots from public.provider_busy_times(current_setting('fixoom.flow.provider')::uuid,day) t;
 if slots::text like '%customer%' or slots::text like '%booking_id%' or slots::text like '%address%' then raise exception 'Availability exposes private booking data';end if;
 if exists(select 1 from public.bookings where id=current_setting('fixoom.flow.booking')::uuid) then raise exception 'Availability bypassed booking RLS';end if;
end$$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.customer'),true);
do $$begin
 begin perform public.create_service_booking_for_skill(gen_random_uuid(),current_setting('fixoom.flow.provider')::uuid,'plumbing',current_setting('fixoom.availability.start')::timestamptz,'Test address','Overlapping fixture',60);raise exception 'Conflict accepted';exception when raise_exception then if sqlerrm<>'This provider already has an accepted job during that time' then raise;end if;end;
end$$;
select set_config('fixoom.availability.next',(public.create_service_booking_for_skill(gen_random_uuid(),current_setting('fixoom.flow.provider')::uuid,'plumbing',current_setting('fixoom.availability.start')::timestamptz+interval '1 hour','Test address','Adjacent fixture',60)).id::text,true);
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.provider'),true);
select public.change_booking_status(current_setting('fixoom.availability.next')::uuid,'accepted');
select public.change_booking_status(current_setting('fixoom.flow.booking')::uuid,'in_progress');
select public.complete_service_booking(current_setting('fixoom.flow.booking')::uuid,100);
do $$begin
 if not exists(select 1 from public.bookings where id=current_setting('fixoom.flow.booking')::uuid and status='completed') then raise exception 'Provider completion history missing';end if;
 if exists(select 1 from public.provider_busy_times(current_setting('fixoom.flow.provider')::uuid,(current_setting('fixoom.availability.start')::timestamptz at time zone 'Asia/Colombo')::date)) then raise exception 'Closed job still reserves time';end if;
end$$;
reset role;
set local role anon;
do $$begin
 begin perform public.provider_busy_times(current_setting('fixoom.flow.provider')::uuid,current_date);raise exception 'Anonymous schedule access';exception when insufficient_privilege then null;end;
end$$;
rollback;
select 'PASS: requested/accepted/completed availability, cross-midnight intervals, anonymous denial, private booking isolation, adjacent booking and provider completion history. All fixtures rolled back.' as result;
