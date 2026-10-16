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
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.provider'),true);
select set_config('fixoom.push.device',gen_random_uuid()::text,true);
select public.register_push_device(current_setting('fixoom.push.device')::uuid,'isolated-fixture-token-never-used-for-fcm','provider');
do $$begin
 begin perform public.push_server_config();raise exception 'User read server credentials';exception when insufficient_privilege then null;end;
 begin perform public.claim_booking_push(50);raise exception 'User claimed privileged jobs';exception when insufficient_privilege then null;end;
 begin perform public.register_push_device(gen_random_uuid(),'isolated-other-token-never-used','customer');raise exception 'Wrong-role device accepted';exception when insufficient_privilege then null;end;
end$$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.customer'),true);
select set_config('fixoom.flow.booking',(public.create_service_booking_for_skill(gen_random_uuid(),current_setting('fixoom.flow.provider')::uuid,'plumbing',now()+interval '2 days','Test service address','Test push authorization',60)).id::text,true);
reset role;
do $$begin
 if (select count(*) from private.push_outbox o join public.notifications n on n.id=o.notification_id where n.booking_id=current_setting('fixoom.flow.booking')::uuid)<>1 then raise exception 'Recipient device push missing or sender notified';end if;
end$$;
set local role service_role;
select set_config('fixoom.push.dispatch',(select dispatch_id::text from public.claim_booking_push(100) where booking_id=current_setting('fixoom.flow.booking')::uuid),true);
select public.finish_booking_push(current_setting('fixoom.push.dispatch')::uuid,false,'isolated_transient_failure',false);
reset role;
do $$begin
 if not exists(select 1 from private.push_outbox where id=current_setting('fixoom.push.dispatch')::uuid and state='pending' and attempts=1 and next_attempt_at>now()) then raise exception 'Push retry state incorrect';end if;
 update private.push_outbox set next_attempt_at=now() where id=current_setting('fixoom.push.dispatch')::uuid;
end$$;
set local role service_role;
select public.claim_booking_push(100);
select public.finish_booking_push(current_setting('fixoom.push.dispatch')::uuid,true,null,false);
reset role;
do $$begin if not exists(select 1 from private.push_outbox where id=current_setting('fixoom.push.dispatch')::uuid and state='sent' and sent_at is not null) then raise exception 'Delivery result not persisted';end if;end$$;
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.customer'),true);
select public.unregister_push_device(current_setting('fixoom.push.device')::uuid);
reset role;
do $$begin if not exists(select 1 from private.push_devices where installation_id=current_setting('fixoom.push.device')::uuid) then raise exception 'Customer removed provider device';end if;end$$;
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('fixoom.flow.provider'),true);
select public.unregister_push_device(current_setting('fixoom.push.device')::uuid);
reset role;
do $$begin if exists(select 1 from private.push_devices where installation_id=current_setting('fixoom.push.device')::uuid) then raise exception 'Sign-out device removal failed';end if;end$$;
rollback;
select 'PASS: role-bound device registration, privileged queue/config denial, recipient-only outbox, claim/retry/delivery state and owned sign-out cleanup. No real push sent; all fixtures rolled back.' as result;
