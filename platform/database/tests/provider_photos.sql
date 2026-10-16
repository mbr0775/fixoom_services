begin;
select set_config('fixoom.photo.a',gen_random_uuid()::text,true);
select set_config('fixoom.photo.b',gen_random_uuid()::text,true);
select set_config('fixoom.photo.customer',gen_random_uuid()::text,true);
select set_config('fixoom.photo.admin',(select id::text from public.profiles where role='admin' and account_status='active' limit 1),true);
select set_config('fixoom.photo.path_a',current_setting('fixoom.photo.a')||'/'||gen_random_uuid()::text||'.jpg',true);
select set_config('fixoom.photo.path_b',current_setting('fixoom.photo.b')||'/'||gen_random_uuid()::text||'.jpg',true);
insert into auth.users(id,email,raw_user_meta_data) values
(current_setting('fixoom.photo.a')::uuid,'fixoom-photo-a@example.invalid','{"role":"provider","full_name":"Photo permission A"}'),
(current_setting('fixoom.photo.b')::uuid,'fixoom-photo-b@example.invalid','{"role":"provider","full_name":"Photo permission B"}'),
(current_setting('fixoom.photo.customer')::uuid,'fixoom-photo-customer@example.invalid','{"role":"customer","full_name":"Photo permission customer"}');
update public.profiles set account_status='active' where id=current_setting('fixoom.photo.b')::uuid;
update public.provider_profiles set category_slug=(select slug from public.service_categories where is_active order by sort_order limit 1), service_area='Permission test area',is_available=true where id=current_setting('fixoom.photo.b')::uuid;
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('fixoom.photo.a'),true);
do $$ begin
  begin
    perform public.set_provider_photo(current_setting('fixoom.photo.path_a'));
    raise exception 'Missing storage object was published';
  exception when invalid_parameter_value then null; end;
  begin
    insert into storage.objects(bucket_id,name,metadata) values('provider-photos',current_setting('fixoom.photo.path_b'),'{"mimetype":"image/jpeg","size":64}');
    raise exception 'Provider uploaded into another account folder';
  exception when insufficient_privilege then null; end;
end $$;
insert into storage.objects(bucket_id,name,metadata) values('provider-photos',current_setting('fixoom.photo.path_a'),'{"mimetype":"image/jpeg","size":64}');
select public.set_provider_photo(current_setting('fixoom.photo.path_a'));
do $$ begin
  if not exists(select 1 from public.profiles where id=auth.uid() and avatar_path=current_setting('fixoom.photo.path_a')) then raise exception 'Private profile did not save photo'; end if;
  if not exists(select 1 from public.provider_profiles where id=auth.uid() and avatar_path=current_setting('fixoom.photo.path_a')) then raise exception 'Provider directory did not save photo'; end if;
  begin
    perform public.set_provider_photo(current_setting('fixoom.photo.path_b'));
    raise exception 'Provider linked another account photo';
  exception when invalid_parameter_value then null; end;
  begin
    update public.profiles set avatar_path='https://untrusted.invalid/image' where id=auth.uid();
    raise exception 'Direct avatar URL edit allowed';
  exception when insufficient_privilege then null; end;
  update storage.objects set metadata='{"size":128}' where bucket_id='provider-photos' and name=current_setting('fixoom.photo.path_a');
  if found then raise exception 'Immutable photo overwritten'; end if;
end $$;
set local role anon;
select set_config('request.jwt.claim.sub','',true);
do $$ begin
  if exists(select 1 from storage.objects where bucket_id='provider-photos' and name=current_setting('fixoom.photo.path_a')) then raise exception 'Pending photo exposed'; end if;
end $$;
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('fixoom.photo.b'),true);
insert into storage.objects(bucket_id,name,metadata) values('provider-photos',current_setting('fixoom.photo.path_b'),'{"mimetype":"image/jpeg","size":64}');
select public.set_provider_photo(current_setting('fixoom.photo.path_b'));
select set_config('request.jwt.claim.sub',current_setting('fixoom.photo.customer'),true);
do $$ begin
  if exists(select 1 from storage.objects where bucket_id='provider-photos' and name=current_setting('fixoom.photo.path_a')) then raise exception 'Customer can read pending photo'; end if;
  if not exists(select 1 from storage.objects where bucket_id='provider-photos' and name=current_setting('fixoom.photo.path_b')) then raise exception 'Customer cannot read approved photo'; end if;
  begin
    perform public.set_provider_photo(null);
    raise exception 'Customer can update provider photo';
  exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.photo.admin'),true);
do $$ begin
  if not exists(select 1 from storage.objects where bucket_id='provider-photos' and name=current_setting('fixoom.photo.path_a')) then raise exception 'Admin cannot review pending photo'; end if;
end $$;
reset role;
update public.profiles set account_status='suspended' where id=current_setting('fixoom.photo.b')::uuid;
set local role anon;
select set_config('request.jwt.claim.sub','',true);
do $$ begin
  if exists(select 1 from storage.objects where bucket_id='provider-photos' and name=current_setting('fixoom.photo.path_b')) then raise exception 'Suspended photo exposed'; end if;
end $$;
reset role;
update public.profiles set account_status='active' where id=current_setting('fixoom.photo.b')::uuid;
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('fixoom.photo.b'),true);
select public.set_provider_photo(null);
set local role anon;
select set_config('request.jwt.claim.sub','',true);
do $$ begin
  if exists(select 1 from storage.objects where bucket_id='provider-photos' and name=current_setting('fixoom.photo.path_b')) then raise exception 'Removed photo exposed'; end if;
end $$;
rollback;
select 'PASS: owned uploads, validated publication, private pending photos, approved visibility, admin review, no arbitrary URLs/overwrites, suspension and removal. All fixtures rolled back.' as result;
