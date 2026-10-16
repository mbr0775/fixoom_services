-- Real policies and functions; every synthetic row is rolled back.
begin;
select set_config('fixoom.test.customer',gen_random_uuid()::text,true);
select set_config('fixoom.test.stranger',gen_random_uuid()::text,true);
select set_config('fixoom.test.provider',gen_random_uuid()::text,true);
select set_config('fixoom.test.key',gen_random_uuid()::text,true);
select set_config('fixoom.test.admin',(select id::text from public.profiles where role='admin' and account_status='active' order by created_at limit 1),true);
select set_config('fixoom.test.time',(now()+interval '2 days')::text,true);
insert into auth.users(id,email,raw_user_meta_data) values
  (current_setting('fixoom.test.customer')::uuid,'fixoom-workflow-customer@example.invalid','{"role":"customer","full_name":"Workflow customer"}'),
  (current_setting('fixoom.test.stranger')::uuid,'fixoom-workflow-stranger@example.invalid','{"role":"customer","full_name":"Unrelated customer"}'),
  (current_setting('fixoom.test.provider')::uuid,'fixoom-workflow-provider@example.invalid','{"role":"provider","full_name":"Workflow professional"}');
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.provider'),true);
select public.save_provider_profile('Workflow professional','plumbing','Colombo','Test service profile',true);
do $$ begin
  if not exists(select 1 from public.provider_profiles where id=auth.uid() and not is_available) then raise exception 'Pending provider became available';end if;
end $$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.admin'),true);
select public.review_provider(current_setting('fixoom.test.provider')::uuid,'active');
select public.review_provider_skill(current_setting('fixoom.test.provider')::uuid,'plumbing','approved');
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.provider'),true);
select public.save_provider_profile('Workflow professional','plumbing','Colombo','Test service profile',true);
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.customer'),true);
do $$ begin
  begin perform public.review_customer(current_setting('fixoom.test.stranger')::uuid,'suspended');raise exception 'Customer moderated another account';exception when insufficient_privilege then null;end;
end $$;
select set_config('fixoom.test.booking',(public.create_service_booking(current_setting('fixoom.test.key')::uuid,current_setting('fixoom.test.provider')::uuid,current_setting('fixoom.test.time')::timestamptz,'Test service address','Repair the test tap')).id::text,true);
select public.create_service_booking(current_setting('fixoom.test.key')::uuid,current_setting('fixoom.test.provider')::uuid,current_setting('fixoom.test.time')::timestamptz,'Test service address','Repair the test tap');
do $$ begin
  if (select count(*) from public.bookings where client_request_id=current_setting('fixoom.test.key')::uuid)<>1 then raise exception 'Duplicate booking created';end if;
  if not exists(select 1 from public.bookings where id=current_setting('fixoom.test.booking')::uuid and provider_name='Workflow professional' and customer_name='Workflow customer' and category_name='Plumbing') then raise exception 'Booking identity snapshot missing';end if;
  begin
    perform public.create_service_booking(current_setting('fixoom.test.key')::uuid,current_setting('fixoom.test.provider')::uuid,current_setting('fixoom.test.time')::timestamptz,'Different service address','Repair the test tap');
    raise exception 'Idempotency key accepted different payload';
  exception when raise_exception then if sqlerrm<>'This request key was used for a different booking' then raise;end if;end;
end $$;
select public.send_booking_message(current_setting('fixoom.test.booking')::uuid,'Customer test message');
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.stranger'),true);
do $$ begin
  if exists(select 1 from public.booking_messages where booking_id=current_setting('fixoom.test.booking')::uuid) or exists(select 1 from public.booking_events where booking_id=current_setting('fixoom.test.booking')::uuid) then raise exception 'Private conversation or history leaked';end if;
  begin perform public.send_booking_message(current_setting('fixoom.test.booking')::uuid,'Unauthorized message');raise exception 'Stranger sent a message';exception when insufficient_privilege then null;end;
end $$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.provider'),true);
select public.send_booking_message(current_setting('fixoom.test.booking')::uuid,'Provider test reply');
select public.change_booking_status(current_setting('fixoom.test.booking')::uuid,'accepted');
select public.change_booking_status(current_setting('fixoom.test.booking')::uuid,'in_progress');
select public.complete_service_booking(current_setting('fixoom.test.booking')::uuid,2500.50);
select public.confirm_direct_payment(current_setting('fixoom.test.booking')::uuid);
select public.confirm_direct_payment(current_setting('fixoom.test.booking')::uuid);
do $$ begin
  if (select count(*) from public.booking_events where booking_id=current_setting('fixoom.test.booking')::uuid and kind='direct_payment_received')<>1 then raise exception 'Duplicate receipt event';end if;
end $$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.customer'),true);
do $$ begin
  if not exists(select 1 from public.bookings where id=current_setting('fixoom.test.booking')::uuid and status='completed' and final_amount=2500.50 and direct_payment_received and completed_at is not null) then raise exception 'Customer cannot see completed service receipt';end if;
  if (select count(*) from public.booking_messages where booking_id=current_setting('fixoom.test.booking')::uuid)<>2 then raise exception 'Conversation not shared';end if;
  if (select count(*) from public.booking_events where booking_id=current_setting('fixoom.test.booking')::uuid)<>5 then raise exception 'Booking timeline incomplete';end if;
  begin perform public.confirm_direct_payment(current_setting('fixoom.test.booking')::uuid);raise exception 'Customer confirmed provider receipt';exception when insufficient_privilege then null;end;
  begin perform public.save_provider_profile('Attack','plumbing','Colombo','',true);raise exception 'Customer edited provider service';exception when insufficient_privilege then null;end;
end $$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.admin'),true);
do $$ begin
  if (select count(*) from public.booking_messages where booking_id=current_setting('fixoom.test.booking')::uuid)<>2 then raise exception 'Admin cannot inspect conversation';end if;
  if not exists(select 1 from public.bookings where id=current_setting('fixoom.test.booking')::uuid and final_amount=2500.50) then raise exception 'Admin cannot see final amount';end if;
  begin perform public.send_booking_message(current_setting('fixoom.test.booking')::uuid,'Impersonation');raise exception 'Admin could impersonate participant';exception when insufficient_privilege then null;end;
end $$;
select public.review_provider(current_setting('fixoom.test.provider')::uuid,'suspended');
select public.review_customer(current_setting('fixoom.test.customer')::uuid,'suspended');
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.customer'),true);
do $$ begin
  if exists(select 1 from public.booking_messages where booking_id=current_setting('fixoom.test.booking')::uuid) then raise exception 'Suspended customer reads private messages';end if;
end $$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.provider'),true);
do $$ begin
  if exists(select 1 from public.booking_messages where booking_id=current_setting('fixoom.test.booking')::uuid) then raise exception 'Suspended provider still reads messages';end if;
  begin perform public.send_booking_message(current_setting('fixoom.test.booking')::uuid,'Suspended message');raise exception 'Suspended provider sent message';exception when insufficient_privilege then null;end;
end $$;
set local role anon;
select set_config('request.jwt.claim.sub','',true);
do $$ begin
  begin perform id from public.booking_messages limit 1;raise exception 'Anonymous messages exposed';exception when insufficient_privilege then null;end;
end $$;
rollback;
select 'PASS: pending profile, admin approval, directory availability, idempotent booking, shared conversation, provider lifecycle, direct receipt, admin visibility, and access isolation. All test data rolled back.' as result;
