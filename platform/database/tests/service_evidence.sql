-- Permission and workflow tests only. No real files, users, bookings or complaints persist.
begin;
-- Match Storage API deletion context so the DELETE assertions exercise RLS.
set local storage.allow_delete_query = 'true';
select set_config('evidence.customer',gen_random_uuid()::text,true);
select set_config('evidence.stranger',gen_random_uuid()::text,true);
select set_config('evidence.provider',gen_random_uuid()::text,true);
select set_config('evidence.admin',(select id::text from public.profiles where role='admin' and account_status='active' limit 1),true);
select set_config('evidence.complaint',gen_random_uuid()::text,true);
select set_config('evidence.photo',gen_random_uuid()::text,true);
select set_config('evidence.video',gen_random_uuid()::text,true);
select set_config('evidence.avatar',current_setting('evidence.customer')||'/'||gen_random_uuid()::text||'.jpg',true);
insert into auth.users(id,email,raw_user_meta_data) values
 (current_setting('evidence.customer')::uuid,'evidence-customer@example.invalid','{"role":"customer","full_name":"Evidence customer"}'),
 (current_setting('evidence.stranger')::uuid,'evidence-stranger@example.invalid','{"role":"customer","full_name":"Unrelated customer"}'),
 (current_setting('evidence.provider')::uuid,'evidence-provider@example.invalid','{"role":"provider","full_name":"Evidence professional"}');
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('evidence.provider'),true);
select public.save_provider_profile('Evidence professional','plumbing','Test area','Rollback-only service description',true);
select set_config('request.jwt.claim.sub',current_setting('evidence.admin'),true);
select public.review_provider(current_setting('evidence.provider')::uuid,'active');
select public.review_provider_skill(current_setting('evidence.provider')::uuid,'plumbing','approved');
select set_config('request.jwt.claim.sub',current_setting('evidence.provider'),true);
select public.save_provider_profile('Evidence professional','plumbing','Test area','Rollback-only service description',true);
select set_config('request.jwt.claim.sub',current_setting('evidence.customer'),true);
select set_config('evidence.booking',(public.create_service_booking(gen_random_uuid(),current_setting('evidence.provider')::uuid,now()+interval '3 days','Rollback-only service address','Rollback-only test issue')).id::text,true);
select public.submit_complaint(current_setting('evidence.complaint')::uuid,current_setting('evidence.booking')::uuid,'Test private report','This description is for permission tests only.');
select public.submit_complaint(current_setting('evidence.complaint')::uuid,current_setting('evidence.booking')::uuid,'Test private report','This description is for permission tests only.');
do $$ begin
 if (select count(*) from public.complaints where id=current_setting('evidence.complaint')::uuid)<>1 then raise exception 'Complaint retry duplicated'; end if;
 begin perform public.submit_complaint(current_setting('evidence.complaint')::uuid,null,'Different report','Different payload cannot be silently discarded.'); raise exception 'Different payload reused key'; exception when invalid_parameter_value then null; end;
 begin perform public.review_complaint(current_setting('evidence.complaint')::uuid,'resolved','Unauthorized resolution'); raise exception 'Customer reviewed complaint'; exception when insufficient_privilege then null; end;
 begin perform public.set_customer_photo(current_setting('evidence.avatar')); raise exception 'Missing avatar published'; exception when invalid_parameter_value then null; end;
 begin insert into storage.objects(bucket_id,name,metadata) values('customer-photos',current_setting('evidence.stranger')||'/'||gen_random_uuid()||'.jpg','{"mimetype":"image/jpeg","size":64}'); raise exception 'Wrong avatar folder allowed'; exception when insufficient_privilege then null; end;
end $$;
insert into storage.objects(bucket_id,name,metadata) values('customer-photos',current_setting('evidence.avatar'),'{"mimetype":"image/jpeg","size":64}');
select public.set_customer_photo(current_setting('evidence.avatar'));
select set_config('evidence.booking_path',current_setting('evidence.customer')||'/'||current_setting('evidence.booking')||'/'||current_setting('evidence.photo')||'.jpg',true);
select set_config('evidence.complaint_path',current_setting('evidence.customer')||'/'||current_setting('evidence.complaint')||'/'||current_setting('evidence.video')||'.mp4',true);
insert into storage.objects(bucket_id,name,metadata) values
 ('service-evidence',current_setting('evidence.booking_path'),'{"mimetype":"image/jpeg","size":512}'),
 ('service-evidence',current_setting('evidence.complaint_path'),'{"mimetype":"video/mp4","size":1024}');
select public.publish_service_attachment(current_setting('evidence.photo')::uuid,current_setting('evidence.booking')::uuid,null,current_setting('evidence.booking_path'));
select public.publish_service_attachment(current_setting('evidence.photo')::uuid,current_setting('evidence.booking')::uuid,null,current_setting('evidence.booking_path'));
select public.publish_service_attachment(current_setting('evidence.video')::uuid,null,current_setting('evidence.complaint')::uuid,current_setting('evidence.complaint_path'));
do $$ declare media uuid; path text; meta jsonb; begin
 for meta in select v from (values ('{"mimetype":"image/jpeg","size":26214401}'::jsonb),('{"mimetype":"application/octet-stream","size":64}'::jsonb)) m(v) loop
   media:=gen_random_uuid(); path:=auth.uid()::text||'/'||current_setting('evidence.booking')||'/'||media||'.jpg';
   insert into storage.objects(bucket_id,name,metadata) values('service-evidence',path,meta);
   begin perform public.publish_service_attachment(media,current_setting('evidence.booking')::uuid,null,path); raise exception 'Invalid evidence published'; exception when invalid_parameter_value then null; end;
 end loop;
end $$;
do $$ declare n int; media uuid; path text; begin
 delete from storage.objects where bucket_id='service-evidence' and name=current_setting('evidence.complaint_path');
 if found then raise exception 'Published evidence was deleted'; end if;
 update storage.objects set metadata='{}' where bucket_id='service-evidence' and name=current_setting('evidence.complaint_path');
 if found then raise exception 'Published evidence overwritten'; end if;
 for n in 1..5 loop
   media:=gen_random_uuid(); path:=auth.uid()::text||'/'||current_setting('evidence.complaint')||'/'||media||'.jpg';
   insert into storage.objects(bucket_id,name,metadata) values('service-evidence',path,'{"mimetype":"image/jpeg","size":64}');
   if n<=4 then perform public.publish_service_attachment(media,null,current_setting('evidence.complaint')::uuid,path);
   else
     begin perform public.publish_service_attachment(media,null,current_setting('evidence.complaint')::uuid,path); raise exception 'More than five files published'; exception when invalid_parameter_value then null; end;
   end if;
 end loop;
end $$;
select set_config('request.jwt.claim.sub',current_setting('evidence.stranger'),true);
do $$ begin
 if exists(select 1 from public.complaints where id=current_setting('evidence.complaint')::uuid) then raise exception 'Complaint leaked to stranger'; end if;
 if exists(select 1 from public.service_attachments where booking_id=current_setting('evidence.booking')::uuid or complaint_id=current_setting('evidence.complaint')::uuid) then raise exception 'Evidence metadata leaked'; end if;
 if exists(select 1 from storage.objects where bucket_id in ('service-evidence','customer-photos') and name in (current_setting('evidence.avatar'),current_setting('evidence.booking_path'),current_setting('evidence.complaint_path'))) then raise exception 'Private storage leaked'; end if;
 begin perform public.submit_complaint(gen_random_uuid(),current_setting('evidence.booking')::uuid,'Unauthorized booking','Unrelated user cannot attach another booking'); raise exception 'Unrelated booking accepted'; exception when insufficient_privilege then null; end;
 begin insert into storage.objects(bucket_id,name,metadata) values('service-evidence',auth.uid()::text||'/'||current_setting('evidence.booking')||'/'||gen_random_uuid()||'.jpg','{"mimetype":"image/jpeg","size":64}'); raise exception 'Stranger uploaded to booking'; exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claim.sub',current_setting('evidence.provider'),true);
do $$ begin
 if not exists(select 1 from public.service_attachments where booking_id=current_setting('evidence.booking')::uuid) then raise exception 'Provider cannot read booking evidence'; end if;
 if not exists(select 1 from storage.objects where bucket_id='service-evidence' and name=current_setting('evidence.booking_path')) then raise exception 'Provider cannot download booking evidence'; end if;
 if exists(select 1 from public.complaints where id=current_setting('evidence.complaint')::uuid) or exists(select 1 from storage.objects where bucket_id='service-evidence' and name=current_setting('evidence.complaint_path')) then raise exception 'Private complaint leaked to provider'; end if;
 begin perform public.set_customer_photo(null); raise exception 'Provider changed customer photo'; exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claim.sub',current_setting('evidence.admin'),true);
do $$ begin
 if not exists(select 1 from public.complaints where id=current_setting('evidence.complaint')::uuid) then raise exception 'Admin cannot read report'; end if;
 if not exists(select 1 from storage.objects where bucket_id='service-evidence' and name=current_setting('evidence.complaint_path')) then raise exception 'Admin cannot read evidence'; end if;
 begin perform public.review_complaint(current_setting('evidence.complaint')::uuid,'resolved',''); raise exception 'Empty resolution accepted'; exception when invalid_parameter_value then null; end;
end $$;
select public.review_complaint(current_setting('evidence.complaint')::uuid,'resolved','Reviewed and resolved for this isolated permission test.');
select set_config('request.jwt.claim.sub',current_setting('evidence.customer'),true);
do $$ begin
 if not exists(select 1 from public.complaints where id=current_setting('evidence.complaint')::uuid and status='resolved' and admin_response<>'') then raise exception 'Reporter cannot read reply'; end if;
 begin insert into storage.objects(bucket_id,name,metadata) values('service-evidence',auth.uid()::text||'/'||current_setting('evidence.complaint')||'/'||gen_random_uuid()||'.jpg','{"mimetype":"image/jpeg","size":64}'); raise exception 'Resolved complaint changed'; exception when insufficient_privilege then null; end;
end $$;
rollback;
select 'PASS: private complaint and booking evidence, immutable publication, idempotency, five-file limits, customer avatars, participant/admin access and private replies. All fixtures rolled back.' as result;
