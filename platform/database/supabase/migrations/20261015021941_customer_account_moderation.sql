create function private.review_customer(customer_id uuid,decision text)
returns void language plpgsql security definer set search_path='' as $$
declare previous public.account_status;
begin
  if not private.has_role('admin') then raise exception 'Administrator access required' using errcode='42501';end if;
  if decision not in ('active','suspended') then raise exception 'Invalid customer review decision';end if;
  select account_status into previous from public.profiles where id=customer_id and role='customer' for update;
  if not found or previous='deleted' then raise exception 'Customer cannot be reviewed';end if;
  update public.profiles set account_status=decision::public.account_status where id=customer_id;
  insert into public.admin_audit_log(actor_id,action,subject_id,details) values(auth.uid(),'customer_review',customer_id,jsonb_build_object('previous_status',previous,'decision',decision));
end;
$$;
revoke all on function private.review_customer(uuid,text) from public,anon;
grant execute on function private.review_customer(uuid,text) to authenticated;
create function public.review_customer(customer_id uuid,decision text)
returns void language sql security invoker set search_path='' as $$select private.review_customer(customer_id,decision);$$;
revoke all on function public.review_customer(uuid,text) from public,anon;
grant execute on function public.review_customer(uuid,text) to authenticated;
