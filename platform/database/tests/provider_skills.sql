-- Synthetic identities, writes and schema preflights are confined to rollback transactions.
begin;
select set_config('fixoom.skill.provider',gen_random_uuid()::text,true);
select set_config('fixoom.skill.customer',gen_random_uuid()::text,true);
select set_config('fixoom.skill.admin',(select id::text from public.profiles where role='admin' and account_status='active' limit 1),true);
select set_config('fixoom.skill.time',(now()+interval '3 days')::text,true);
insert into auth.users(id,email,raw_user_meta_data) values
(current_setting('fixoom.skill.provider')::uuid,'skill-provider@example.invalid','{"role":"provider","full_name":"Skill fixture","account_status":"active","skill_categories":["plumbing","carpentry"]}'),
(current_setting('fixoom.skill.customer')::uuid,'skill-customer@example.invalid','{"role":"customer","full_name":"Customer fixture"}');
do $$begin
 if not exists(select 1 from public.profiles where id=current_setting('fixoom.skill.provider')::uuid and account_status='pending') or (select count(*) from public.provider_skills where provider_id=current_setting('fixoom.skill.provider')::uuid and status='pending')<>2 then raise exception 'Signup bypassed approval or lost selected skills';end if;
end$$;
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('fixoom.skill.provider'),true);
select public.request_provider_skill('plumbing','Real permission-test plumbing description');
select public.request_provider_skill('carpentry','Real permission-test carpentry description');
select public.save_provider_profile('Skill fixture','','Colombo','Permission test bio',true);
do $$begin
 begin update public.provider_skills set status='approved' where provider_id=auth.uid();raise exception 'Provider self-approved';exception when insufficient_privilege then null;end;
 begin update public.provider_profiles set approved_categories=array['plumbing'] where id=auth.uid();raise exception 'Provider forged skills';exception when insufficient_privilege then null;end;
 begin perform public.review_provider_skill(auth.uid(),'plumbing','approved');raise exception 'Provider reviewed skill';exception when insufficient_privilege then null;end;
end$$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.skill.admin'),true);
select public.review_provider(current_setting('fixoom.skill.provider')::uuid,'active');
select set_config('request.jwt.claim.sub',current_setting('fixoom.skill.customer'),true);
do $$begin
 if exists(select 1 from public.provider_profiles where id=current_setting('fixoom.skill.provider')::uuid) or exists(select 1 from public.provider_skills where provider_id=current_setting('fixoom.skill.provider')::uuid) then raise exception 'Pending service became public with account approval';end if;
 begin perform public.request_provider_skill('painting','Unauthorized offering');raise exception 'Customer submitted provider skill';exception when insufficient_privilege then null;end;
end$$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.skill.provider'),true);
select public.save_provider_profile('Skill fixture','plumbing','Colombo','Permission test bio',true);
select set_config('request.jwt.claim.sub',current_setting('fixoom.skill.admin'),true);
select public.review_provider_skill(current_setting('fixoom.skill.provider')::uuid,'plumbing','approved');
select set_config('request.jwt.claim.sub',current_setting('fixoom.skill.provider'),true);
select public.save_provider_profile('Skill fixture','plumbing','Colombo','Permission test bio',true);
select set_config('request.jwt.claim.sub',current_setting('fixoom.skill.customer'),true);
do $$begin
 if (select count(*) from public.provider_skills where provider_id=current_setting('fixoom.skill.provider')::uuid)<>1 then raise exception 'Pending skill leaked';end if;
 begin perform public.create_service_booking_for_skill(gen_random_uuid(),current_setting('fixoom.skill.provider')::uuid,'carpentry',current_setting('fixoom.skill.time')::timestamptz,'Test service address','Test unapproved booking',60);raise exception 'Pending skill booked';exception when raise_exception then if sqlerrm<>'This provider is not available for this service' then raise;end if;end;
end$$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.skill.admin'),true);
select public.review_provider_skill(current_setting('fixoom.skill.provider')::uuid,'carpentry','approved');
select set_config('request.jwt.claim.sub',current_setting('fixoom.skill.customer'),true);
select set_config('fixoom.skill.key',gen_random_uuid()::text,true);
select set_config('fixoom.skill.booking',(public.create_service_booking_for_skill(current_setting('fixoom.skill.key')::uuid,current_setting('fixoom.skill.provider')::uuid,'plumbing',current_setting('fixoom.skill.time')::timestamptz,'Test service address','Test plumbing booking',60)).id::text,true);
select set_config('fixoom.skill.overlap',(public.create_service_booking_for_skill(gen_random_uuid(),current_setting('fixoom.skill.provider')::uuid,'carpentry',current_setting('fixoom.skill.time')::timestamptz,'Test service address','Test carpentry booking',60)).id::text,true);
do $$begin
 if not exists(select 1 from public.provider_profiles where id=current_setting('fixoom.skill.provider')::uuid and approved_categories @> array['plumbing','carpentry']) then raise exception 'Approved categories not synchronized';end if;
 begin perform public.create_service_booking_for_skill(current_setting('fixoom.skill.key')::uuid,current_setting('fixoom.skill.provider')::uuid,'carpentry',current_setting('fixoom.skill.time')::timestamptz,'Test service address','Test plumbing booking',60);raise exception 'Idempotency ignored skill';exception when raise_exception then if sqlerrm<>'This request key was used for a different booking' then raise;end if;end;
end$$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.skill.provider'),true);
select public.change_booking_status(current_setting('fixoom.skill.booking')::uuid,'accepted');
do $$begin
 begin perform public.change_booking_status(current_setting('fixoom.skill.overlap')::uuid,'accepted');raise exception 'Overlapping skills accepted';exception when raise_exception then if sqlerrm<>'This provider already has an accepted job during that time' then raise;end if;end;
end$$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.skill.admin'),true);
select public.review_provider_skill(current_setting('fixoom.skill.provider')::uuid,'plumbing','suspended');
select set_config('request.jwt.claim.sub',current_setting('fixoom.skill.provider'),true);
select public.change_booking_status(current_setting('fixoom.skill.booking')::uuid,'in_progress');
select public.complete_service_booking(current_setting('fixoom.skill.booking')::uuid,2500);
select public.withdraw_provider_skill('carpentry');
do $$begin
 if not exists(select 1 from public.provider_profiles where id=auth.uid() and cardinality(approved_categories)=0 and not is_available) then raise exception 'Withdrawn skills remained published';end if;
end$$;
set local role anon;
select set_config('request.jwt.claim.sub','',true);
do $$begin
 if exists(select 1 from public.provider_profiles where id=current_setting('fixoom.skill.provider')::uuid) then raise exception 'Provider without approved skills is public';end if;
 begin perform public.request_provider_skill('painting','Unauthorized request');raise exception 'Anonymous skill submission';exception when insufficient_privilege then null;end;
end$$;
rollback;
select 'PASS: signup requests, account/skill separation, owner/admin authorization, public filtering, explicit service booking, idempotency, shared-calendar overlap, withdrawal and existing-job history; all fixture data rolled back.' as result;
