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
do $$declare nearby jsonb;begin
 nearby:=public.nearby_providers(6.9271,79.8612,50);
 if not exists(select 1 from jsonb_array_elements(nearby) p where p->>'id'=current_setting('fixoom.flow.provider') and (p->>'distance_km')::numeric=0) then raise exception 'Nearby approved service absent';end if;
 if nearby::text like '%latitude%' or nearby::text like '%longitude%' then raise exception 'Discovery exposes exact coordinates';end if;
 if exists(select 1 from public.provider_service_locations) then raise exception 'Customer reads private service bases';end if;
 if exists(select 1 from jsonb_array_elements(public.nearby_providers(9.6615,80.0255,50)) p where p->>'id'=current_setting('fixoom.flow.provider')) then raise exception 'Far provider included';end if;
 begin perform public.set_service_location(6.9,79.8,25);raise exception 'Customer set provider base';exception when insufficient_privilege then null;end;
end$$;
select set_config('fixoom.flow.booking',(public.create_service_booking_for_skill(gen_random_uuid(),current_setting('fixoom.flow.provider')::uuid,'plumbing',now()+interval '2 days','Test service address','Test booking notification',60)).id::text,true);
do $$begin
 if (select count(*) from public.notifications where booking_id=current_setting('fixoom.flow.booking')::uuid and kind='requested')<>1 then raise exception 'Customer inbox missing or leaked';end if;
 begin update public.notifications set title='Forged';raise exception 'Notification tampering allowed';exception when insufficient_privilege then null;end;
end$$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.provider'),true);
do $$begin
 if (select count(*) from public.notifications where booking_id=current_setting('fixoom.flow.booking')::uuid and kind='requested')<>1 then raise exception 'Provider did not receive request';end if;
end$$;
select public.change_booking_status(current_setting('fixoom.flow.booking')::uuid,'accepted');
select public.send_booking_message(current_setting('fixoom.flow.booking')::uuid,'Fixture reply for the customer');
select public.share_booking_location(current_setting('fixoom.flow.booking')::uuid,6.928,79.86,15);
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.customer'),true);
do $$begin
 if not exists(select 1 from public.notifications where booking_id=current_setting('fixoom.flow.booking')::uuid and kind='message' and read_at is null) then raise exception 'Customer reply notification missing';end if;
 if not exists(select 1 from public.notifications where booking_id=current_setting('fixoom.flow.booking')::uuid and kind='message' and title='New message from Flow professional' and body like '%Plumbing%') then raise exception 'Message notification lacks actual sender/service';end if;
 if exists(select 1 from public.notifications where booking_id=current_setting('fixoom.flow.booking')::uuid and body like '%Fixture reply for the customer%') then raise exception 'Chat content unexpectedly published in alert';end if;
 if not exists(select 1 from public.booking_locations where booking_id=current_setting('fixoom.flow.booking')::uuid) then raise exception 'Assigned customer cannot track active job';end if;
 begin perform public.share_booking_location(current_setting('fixoom.flow.booking')::uuid,6.9,79.8,10);raise exception 'Customer forged provider position';exception when insufficient_privilege then null;end;
end$$;
select public.mark_notification_read(null);
do $$begin if exists(select 1 from public.notifications where booking_id=current_setting('fixoom.flow.booking')::uuid and read_at is null) then raise exception 'Inbox read not persisted';end if;end$$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.stranger'),true);
do $$begin
 if exists(select 1 from public.notifications where booking_id=current_setting('fixoom.flow.booking')::uuid) or exists(select 1 from public.booking_locations where booking_id=current_setting('fixoom.flow.booking')::uuid) then raise exception 'Unrelated user sees private updates';end if;
end$$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.provider'),true);
select public.share_booking_location(current_setting('fixoom.flow.booking')::uuid,null,null,0);
do $$begin if exists(select 1 from public.booking_locations where booking_id=current_setting('fixoom.flow.booking')::uuid) then raise exception 'Stop sharing failed';end if;end$$;
select public.share_booking_location(current_setting('fixoom.flow.booking')::uuid,6.928,79.86,15);
reset role;
update public.booking_locations set updated_at=now()-interval '6 minutes' where booking_id=current_setting('fixoom.flow.booking')::uuid;
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.customer'),true);
do $$begin if exists(select 1 from public.booking_locations where booking_id=current_setting('fixoom.flow.booking')::uuid) then raise exception 'Stale tracking exposed';end if;end$$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.provider'),true);
select public.share_booking_location(current_setting('fixoom.flow.booking')::uuid,6.928,79.86,15);
select public.change_booking_status(current_setting('fixoom.flow.booking')::uuid,'in_progress');
select public.complete_service_booking(current_setting('fixoom.flow.booking')::uuid,2500);
reset role;
do $$begin if exists(select 1 from public.booking_locations where booking_id=current_setting('fixoom.flow.booking')::uuid) then raise exception 'Closed booking retained live location';end if;end$$;
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.customer'),true);
do $$begin if not exists(select 1 from public.notifications where booking_id=current_setting('fixoom.flow.booking')::uuid and kind='completed') then raise exception 'Completion not notified';end if;end$$;
set local role anon;
select set_config('request.jwt.claim.sub','',true);
do $$begin
 begin perform id from public.notifications limit 1;raise exception 'Anonymous inbox access';exception when insufficient_privilege then null;end;
 begin perform booking_id from public.booking_locations limit 1;raise exception 'Anonymous tracking access';exception when insufficient_privilege then null;end;
end$$;
rollback;
select 'PASS: recipient notifications, reply/status persistence, read state, private nearby distances, radius filtering, optional assigned-provider sharing, stranger denial, stale expiry and closed-job cleanup. All fixtures rolled back.' as result;
