begin;
select set_config('fixoom.test.email_provider',gen_random_uuid()::text,true);
select set_config('fixoom.test.email_customer',gen_random_uuid()::text,true);
select set_config('fixoom.test.email_admin',(select id::text from public.profiles where role='admin' and account_status='active' limit 1),true);
insert into auth.users(id,email,raw_user_meta_data) values
  (current_setting('fixoom.test.email_provider')::uuid,'fixoom-email-provider@example.invalid','{"role":"provider","full_name":"Contact regression"}'),
  (current_setting('fixoom.test.email_customer')::uuid,'fixoom-email-customer@example.invalid','{"role":"customer","full_name":"Contact isolation"}');
do $$ begin
  if not exists(select 1 from public.profiles where id=current_setting('fixoom.test.email_provider')::uuid and email='fixoom-email-provider@example.invalid') then raise exception 'Registration email did not sync'; end if;
end $$;
update auth.users set email='fixoom-email-updated@example.invalid' where id=current_setting('fixoom.test.email_provider')::uuid;
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.email_provider'),true);
do $$ begin
  if not exists(select 1 from public.profiles where id=auth.uid() and email='fixoom-email-updated@example.invalid') then raise exception 'Own verified email change did not sync'; end if;
  begin
    update public.profiles set email='forged@example.invalid' where id=auth.uid();
    raise exception 'Client overwrote Auth email';
  exception when insufficient_privilege then null;
  end;
end $$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.email_customer'),true);
do $$ begin
  if exists(select 1 from public.profiles where id=current_setting('fixoom.test.email_provider')::uuid) then raise exception 'Unrelated customer can read provider email'; end if;
end $$;
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.email_admin'),true);
do $$ begin
  if not exists(select 1 from public.profiles where id=current_setting('fixoom.test.email_provider')::uuid and email='fixoom-email-updated@example.invalid') then raise exception 'Admin cannot read provider contact'; end if;
end $$;
set local role anon;
select set_config('request.jwt.claim.sub','',true);
do $$ begin
  begin
    perform email from public.profiles limit 1;
    raise exception 'Anonymous client can read private emails';
  exception when insufficient_privilege then null;
  end;
end $$;
rollback;
select 'PASS: registration/email-change synchronization, admin visibility, own-only access, no client overwrites and anonymous denial. All fixtures rolled back.' as result;
