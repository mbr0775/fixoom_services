begin;
do $$ begin
  if private.normalize_contact_phone('077 123 4567') is distinct from '+94771234567' then raise exception 'Local phone normalization failed'; end if;
  if private.normalize_contact_phone('+94 (77) 123-4567') is distinct from '+94771234567' then raise exception 'Formatted phone normalization failed'; end if;
  if private.normalize_contact_phone('0094771234567') is distinct from '+94771234567' then raise exception 'International prefix normalization failed'; end if;
  if private.normalize_contact_phone('94771234567') is distinct from '+94771234567' then raise exception 'Country code normalization failed'; end if;
  if private.normalize_contact_phone('call 0771234567') is not null then raise exception 'Invalid contact accepted'; end if;
end $$;
select set_config('fixoom.test.phone_user',gen_random_uuid()::text,true);
insert into auth.users(id,email,raw_user_meta_data)
values(current_setting('fixoom.test.phone_user')::uuid,'fixoom-phone-regression@example.invalid',
  '{"role":"provider","full_name":"Phone regression","phone":"077 123 4567"}');
do $$ begin
  if not exists(select 1 from public.profiles where id=current_setting('fixoom.test.phone_user')::uuid and phone='+94771234567' and role='provider' and account_status='pending') then raise exception 'Signup contact was lost or approval bypassed'; end if;
end $$;
insert into auth.users(id,email,raw_user_meta_data)
values(gen_random_uuid(),'fixoom-shared-phone-regression@example.invalid',
  '{"role":"customer","full_name":"Shared contact regression","phone":"0771234567"}');
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('fixoom.test.phone_user'),true);
update public.profiles set phone='+442079460000' where id=auth.uid();
do $$ begin
  if not exists(select 1 from public.profiles where id=auth.uid() and phone='+442079460000') then raise exception 'Valid contact edit failed'; end if;
  begin
    update public.profiles set phone='not a number' where id=auth.uid();
    raise exception 'Invalid profile phone accepted';
  exception when check_violation then null;
  end;
end $$;
rollback;
select 'PASS: local/international phones, registration persistence, pending approval and contact edit validation. All fixtures rolled back.' as result;
