-- An authorized administrator may review older signup requests with sparse details.
-- Missing descriptions stay honest and do not bypass account/category visibility checks.
create or replace function private.review_provider_skill(p_provider_id uuid,p_category_slug text,p_decision text)
returns void language plpgsql security definer set search_path='' as $$
declare previous text;
begin
 if not private.has_role('admin') then raise exception 'Administrator access required' using errcode='42501';end if;
 if p_decision is null or p_decision not in ('approved','rejected','suspended') then raise exception 'Invalid skill decision';end if;
 perform id from public.profiles where id=p_provider_id and role='provider' and account_status<>'deleted' for update;
 if not found then raise exception 'Provider not found';end if;
 select status into previous from public.provider_skills where provider_id=p_provider_id and category_slug=p_category_slug for update;
 if previous is null or previous='withdrawn' then raise exception 'Requested skill not found';end if;
 if previous=p_decision then return;end if;
 if p_decision='approved' and not exists(select 1 from public.service_categories where slug=p_category_slug and is_active) then raise exception 'Enable this service category before approval';end if;
 update public.provider_skills set status=p_decision,reviewed_at=now(),updated_at=now() where provider_id=p_provider_id and category_slug=p_category_slug;
 insert into public.admin_audit_log(actor_id,action,subject_id,details) values(auth.uid(),'provider_skill_review',p_provider_id,
 jsonb_build_object('category_slug',p_category_slug,'previous_status',previous,'decision',p_decision));
end;$$;
revoke all on function private.review_provider_skill(uuid,text,text) from public,anon,authenticated;
grant execute on function private.review_provider_skill(uuid,text,text) to authenticated;
