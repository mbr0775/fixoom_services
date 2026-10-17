-- Isolated account/approval fixtures. All records, audits and side effects roll back.
begin;
select set_config('fixoom.signup.provider',gen_random_uuid()::text,true);
select set_config('fixoom.signup.legacy',gen_random_uuid()::text,true);
select set_config('fixoom.signup.invalid',gen_random_uuid()::text,true);
select set_config('fixoom.signup.customer',gen_random_uuid()::text,true);
select set_config('fixoom.signup.admin',(select id::text from public.profiles where role='admin' and account_status='active' limit 1),true);
insert into auth.users(id,email,raw_user_meta_data) values
 (current_setting('fixoom.signup.provider')::uuid,'signup-offerings@example.invalid','{"role":"provider","full_name":"Signup fixture","account_status":"active","approved_categories":["plumbing"],"skill_categories":["plumbing","carpentry"],"skill_descriptions":{"plumbing":"  Pipe fitting and plumbing repairs with practical experience.  ","carpentry":"Furniture repairs, shelves and carpentry installation.","painting":"Unselected offering must not be inserted."}}'),
 (current_setting('fixoom.signup.legacy')::uuid,'signup-legacy@example.invalid','{"role":"provider","full_name":"Legacy fixture","skill_categories":["plumbing"]}'),
 (current_setting('fixoom.signup.invalid')::uuid,'signup-invalid@example.invalid',jsonb_build_object('role','provider','full_name','Invalid fixture','skill_categories',jsonb_build_array('plumbing','carpentry','painting'),'skill_descriptions',jsonb_build_object('plumbing','23','carpentry',jsonb_build_array('malformed'),'painting',repeat('x',2001)))),
 (current_setting('fixoom.signup.customer')::uuid,'signup-customer@example.invalid','{"role":"customer","full_name":"Customer fixture","skill_categories":["plumbing"],"skill_descriptions":{"plumbing":"Customer cannot become a provider through skill metadata."}}');
do $$begin
 if (select count(*) from public.provider_skills where provider_id=current_setting('fixoom.signup.provider')::uuid and status='pending' and length(description)>=10)<>2 then raise exception 'Registration descriptions missing or requests self-approved';end if;
 if (select description from public.provider_skills where provider_id=current_setting('fixoom.signup.provider')::uuid and category_slug='plumbing')<>'Pipe fitting and plumbing repairs with practical experience.' then raise exception 'Description not normalized';end if;
 if exists(select 1 from public.provider_skills where provider_id=current_setting('fixoom.signup.provider')::uuid and category_slug='painting') then raise exception 'Unselected service inserted';end if;
 if exists(select 1 from public.profiles where id=current_setting('fixoom.signup.provider')::uuid and account_status<>'pending') then raise exception 'Metadata bypassed account approval';end if;
 if not exists(select 1 from public.provider_skills where provider_id=current_setting('fixoom.signup.legacy')::uuid and description='' and status='pending') then raise exception 'Legacy signup no longer works';end if;
 if exists(select 1 from public.provider_skills where provider_id=current_setting('fixoom.signup.invalid')::uuid and description<>'') then raise exception 'Malformed descriptions were trusted';end if;
 if exists(select 1 from public.provider_skills where provider_id=current_setting('fixoom.signup.customer')::uuid) then raise exception 'Customer metadata created provider skills';end if;
end$$;
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('fixoom.signup.provider'),true);
select public.save_provider_profile('Signup fixture','','Colombo','Permission fixture introduction',true);
do $$begin
 begin perform public.review_provider_skill(auth.uid(),'plumbing','approved');raise exception 'Provider self-approved';exception when insufficient_privilege then null;end;
end$$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.signup.admin'),true);
-- Service review is allowed before account approval. Visibility still needs both.
select public.review_provider_skill(current_setting('fixoom.signup.provider')::uuid,'plumbing','approved');
select public.review_provider_skill(current_setting('fixoom.signup.legacy')::uuid,'plumbing','approved');
do $$begin
 if not exists(select 1 from public.provider_skills where provider_id=current_setting('fixoom.signup.legacy')::uuid and status='approved' and description='') then raise exception 'Admin cannot review a legacy selection or description was fabricated';end if;
end$$;
update public.service_categories set is_active=false where slug='carpentry';
do $$begin
 begin perform public.review_provider_skill(current_setting('fixoom.signup.provider')::uuid,'carpentry','approved');raise exception 'Inactive category approved';exception when raise_exception then if sqlerrm<>'Enable this service category before approval' then raise;end if;end;
end$$;
update public.service_categories set is_active=true where slug='carpentry';
select set_config('request.jwt.claim.sub',current_setting('fixoom.signup.customer'),true);
do $$begin
 if exists(select 1 from public.provider_profiles where id=current_setting('fixoom.signup.provider')::uuid) then raise exception 'Pending account became public after skill approval';end if;
end$$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.signup.admin'),true);
select public.review_provider(current_setting('fixoom.signup.provider')::uuid,'active');
select set_config('request.jwt.claim.sub',current_setting('fixoom.signup.provider'),true);
select public.save_provider_profile('Signup fixture','plumbing','Colombo','Permission fixture introduction',true);
select set_config('request.jwt.claim.sub',current_setting('fixoom.signup.customer'),true);
do $$begin
 if not exists(select 1 from public.provider_profiles where id=current_setting('fixoom.signup.provider')::uuid and approved_categories @> array['plumbing']) then raise exception 'Reviewed signup offering not discoverable';end if;
 if exists(select 1 from public.provider_skills where provider_id=current_setting('fixoom.signup.provider')::uuid and category_slug='carpentry') then raise exception 'Unapproved second service leaked';end if;
end$$;
rollback;
select 'PASS: registration descriptions, legacy compatibility, malformed metadata, admin-only review, separate account/service approval and customer discovery; all fixtures rolled back.' as result;
