-- Transactional database tests: synthetic users and bookings are rolled back.
begin;
select set_config('fixoom.test.customer',gen_random_uuid()::text,true);
select set_config('fixoom.test.stranger',gen_random_uuid()::text,true);
select set_config('fixoom.test.provider',gen_random_uuid()::text,true);
select set_config('fixoom.test.attacker',gen_random_uuid()::text,true);
select set_config('fixoom.test.admin',(select id::text from public.profiles where role='admin' and account_status='active' order by created_at limit 1),true);
insert into auth.users (id,email,raw_user_meta_data) values
  (current_setting('fixoom.test.customer')::uuid,'fixoom-test-customer@example.invalid','{"role":"customer","full_name":"Test customer"}'),
  (current_setting('fixoom.test.stranger')::uuid,'fixoom-test-stranger@example.invalid','{"role":"customer","full_name":"Test stranger"}'),
  (current_setting('fixoom.test.provider')::uuid,'fixoom-test-provider@example.invalid','{"role":"provider","full_name":"Test provider"}'),
  (current_setting('fixoom.test.attacker')::uuid,'fixoom-test-attacker@example.invalid','{"role":"admin","full_name":"Test attacker"}');
do $$ begin
  if not exists(select 1 from public.profiles where id=current_setting('fixoom.test.provider')::uuid and role='provider' and account_status='pending') then raise exception 'Provider registration did not create a pending provider'; end if;
  if not exists(select 1 from public.profiles where id=current_setting('fixoom.test.attacker')::uuid and role='customer') then raise exception 'Self-assigned admin role was accepted'; end if;
end $$;
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.customer'),true);
do $$ begin
  if (select count(*) from public.profiles) <> 1 then raise exception 'Customer can read other profiles'; end if;
  begin
    perform public.review_provider(current_setting('fixoom.test.provider')::uuid,'active');
    raise exception 'Customer was able to approve a provider';
  exception when insufficient_privilege then null; end;
  begin
    update public.profiles set role='admin' where id=auth.uid();
    raise exception 'Customer was able to promote their account';
  exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.admin'),true);
select public.review_provider(current_setting('fixoom.test.provider')::uuid,'active');
do $$ begin
  if not exists(select 1 from public.profiles where id=current_setting('fixoom.test.provider')::uuid and account_status='active') then raise exception 'Admin review did not approve provider'; end if;
  if not exists(select 1 from public.admin_audit_log where subject_id=current_setting('fixoom.test.provider')::uuid) then raise exception 'Provider review was not audited'; end if;
end $$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.provider'),true);
select public.request_provider_skill('plumbing','Permission suite plumbing service');
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.admin'),true);
select public.review_provider_skill(current_setting('fixoom.test.provider')::uuid,'plumbing','approved');
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.provider'),true);
select public.save_provider_profile('Test provider','plumbing','Test area','Permission suite service',true);
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.customer'),true);
select set_config('fixoom.test.booking',(public.submit_booking(current_setting('fixoom.test.provider')::uuid,now()+interval '1 day','Test service address','Test leaking kitchen tap')).id::text,true);
do $$ begin
  if not exists(select 1 from public.bookings where id=current_setting('fixoom.test.booking')::uuid and status='requested') then raise exception 'Customer booking not visible'; end if;
  begin
    perform public.change_booking_status(current_setting('fixoom.test.booking')::uuid,'completed');
    raise exception 'Customer was able to complete a requested job';
  exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.stranger'),true);
do $$ begin
  if exists(select 1 from public.bookings where id=current_setting('fixoom.test.booking')::uuid) then raise exception 'Unrelated customer can read private booking'; end if;
  begin
    perform public.change_booking_status(current_setting('fixoom.test.booking')::uuid,'cancelled');
    raise exception 'Unrelated customer can cancel private booking';
  exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.provider'),true);
select public.change_booking_status(current_setting('fixoom.test.booking')::uuid,'accepted');
select public.change_booking_status(current_setting('fixoom.test.booking')::uuid,'in_progress');
select public.change_booking_status(current_setting('fixoom.test.booking')::uuid,'completed');
do $$ begin
  if not exists(select 1 from public.bookings where id=current_setting('fixoom.test.booking')::uuid and status='completed') then raise exception 'Provider lifecycle did not complete'; end if;
  begin
    perform public.change_booking_status(current_setting('fixoom.test.booking')::uuid,'accepted');
    raise exception 'A completed booking was reopened';
  exception when raise_exception then
    if sqlerrm <> 'This booking is already closed' then raise; end if;
  end;
end $$;
set local role anon;
select set_config('request.jwt.claim.sub','',true);
do $$ begin
  if (select count(*) from public.service_categories) <> 6 then raise exception 'Public service categories inaccessible'; end if;
  if not exists(select 1 from public.provider_profiles where id=current_setting('fixoom.test.provider')::uuid) then raise exception 'Approved provider directory inaccessible'; end if;
  begin
    perform id from public.bookings limit 1;
    raise exception 'Anonymous user can read bookings';
  exception when insufficient_privilege then null; end;
end $$;
rollback;
select 'PASS: registration, admin review, isolation, booking lifecycle, and anonymous access. All synthetic data rolled back.' as result;
