-- Private evidence and customer photos in the existing shared project.
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types) values
 ('customer-photos','customer-photos',false,1048576,array['image/jpeg']),
 ('service-evidence','service-evidence',false,26214400,array['image/jpeg','video/mp4']);

create function private.can_use_support() returns boolean language sql stable security definer set search_path='' as $$
 select exists(select 1 from public.profiles where id=auth.uid() and
   ((role='customer' and account_status='active') or (role='provider' and account_status in ('active','pending'))));
$$;
revoke all on function private.can_use_support() from public,anon;
grant execute on function private.can_use_support() to authenticated;

create table public.complaints (
 id uuid primary key,
 reporter_id uuid not null references public.profiles(id),
 booking_id uuid references public.bookings(id),
 subject text not null check(length(trim(subject)) between 5 and 120),
 description text not null check(length(trim(description)) between 10 and 4000),
 status text not null default 'open' check(status in ('open','in_review','resolved')),
 admin_response text not null default '' check(length(admin_response)<=4000),
 created_at timestamptz not null default now(),
 updated_at timestamptz not null default now()
);
create index complaints_reporter_created on public.complaints(reporter_id,created_at desc);
create index complaints_status_created on public.complaints(status,created_at desc);
create index complaints_booking on public.complaints(booking_id);
alter table public.complaints enable row level security;
revoke all on public.complaints from anon,authenticated;
grant select on public.complaints to authenticated;
create policy complaints_private_read on public.complaints for select to authenticated using (
 (reporter_id=(select auth.uid()) and (select private.can_use_support())) or (select private.has_role('admin'))
);

create function private.submit_complaint(p_id uuid,p_booking_id uuid,p_subject text,p_description text)
returns public.complaints language plpgsql security definer set search_path='' as $$
declare result public.complaints;
begin
 perform 1 from public.profiles where id=auth.uid() for update;
 if not private.can_use_support() then raise exception 'Account access required' using errcode='42501'; end if;
 select * into result from public.complaints where id=p_id;
 if found then
   if result.reporter_id<>auth.uid() then raise exception 'Request key unavailable' using errcode='42501'; end if;
   if result.booking_id is distinct from p_booking_id or result.subject is distinct from trim(p_subject) or result.description is distinct from trim(p_description) then
     raise exception 'This request key was used for different complaint details' using errcode='22023'; end if;
   return result;
 end if;
 if p_booking_id is not null and not private.can_access_booking(p_booking_id) then
   raise exception 'Your booking is required' using errcode='42501'; end if;
 if (select count(*) from public.complaints where reporter_id=auth.uid() and status<>'resolved')>=20 then
   raise exception 'Please wait for your existing complaints to be reviewed' using errcode='22023'; end if;
 insert into public.complaints(id,reporter_id,booking_id,subject,description)
 values(p_id,auth.uid(),p_booking_id,trim(p_subject),trim(p_description)) returning * into result;
 return result;
end;
$$;
revoke all on function private.submit_complaint(uuid,uuid,text,text) from public,anon;
grant execute on function private.submit_complaint(uuid,uuid,text,text) to authenticated;
create function public.submit_complaint(p_id uuid,p_booking_id uuid,p_subject text,p_description text)
returns public.complaints language sql security invoker set search_path='' as $$
 select private.submit_complaint(p_id,p_booking_id,p_subject,p_description);
$$;
revoke all on function public.submit_complaint(uuid,uuid,text,text) from public,anon;
grant execute on function public.submit_complaint(uuid,uuid,text,text) to authenticated;

create function private.review_complaint(p_id uuid,p_status text,p_response text)
returns void language plpgsql security definer set search_path='' as $$
begin
 if not private.has_role('admin') then raise exception 'Administrator access required' using errcode='42501'; end if;
 if p_status not in ('open','in_review','resolved') or p_response is null or length(p_response)>4000 or
   (p_status='resolved' and length(trim(p_response))<10) then raise exception 'Check status and reply' using errcode='22023'; end if;
 update public.complaints set status=p_status,admin_response=trim(p_response),updated_at=now() where id=p_id;
 if not found then raise exception 'Complaint unavailable' using errcode='22023'; end if;
 insert into public.admin_audit_log(actor_id,action,subject_id,details)
 values(auth.uid(),'complaint_review',p_id,jsonb_build_object('status',p_status));
end;
$$;
revoke all on function private.review_complaint(uuid,text,text) from public,anon;
grant execute on function private.review_complaint(uuid,text,text) to authenticated;
create function public.review_complaint(p_id uuid,p_status text,p_response text)
returns void language sql security invoker set search_path='' as $$ select private.review_complaint(p_id,p_status,p_response); $$;
revoke all on function public.review_complaint(uuid,text,text) from public,anon;
grant execute on function public.review_complaint(uuid,text,text) to authenticated;

create table public.service_attachments (
 id uuid primary key,
 owner_id uuid not null references public.profiles(id),
 booking_id uuid references public.bookings(id),
 complaint_id uuid references public.complaints(id),
 object_path text not null unique,
 media_type text not null check(media_type in ('image/jpeg','video/mp4')),
 byte_size bigint not null check(byte_size between 1 and 26214400),
 created_at timestamptz not null default now(),
 check(num_nonnulls(booking_id,complaint_id)=1),
 check(object_path=owner_id::text||'/'||coalesce(booking_id,complaint_id)::text||'/'||id::text||
   case when media_type='image/jpeg' then '.jpg' else '.mp4' end)
);
create index attachment_booking on public.service_attachments(booking_id);
create index attachment_complaint on public.service_attachments(complaint_id);
create index attachment_owner on public.service_attachments(owner_id);
alter table public.service_attachments enable row level security;
revoke all on public.service_attachments from anon,authenticated;
grant select on public.service_attachments to authenticated;
create policy evidence_private_read on public.service_attachments for select to authenticated using (
 (booking_id is not null and private.can_access_booking(booking_id)) or
 (complaint_id is not null and exists(select 1 from public.complaints c where c.id=complaint_id))
);

create function private.can_upload_evidence(p_path text) returns boolean
language plpgsql stable security definer set search_path='' as $$
declare context_id uuid;
begin
 if not private.can_use_support() or p_path !~ ('^'||auth.uid()::text||'/[0-9a-f-]{36}/[0-9a-f-]{36}[.](jpg|mp4)$') then return false; end if;
 begin context_id:=split_part(p_path,'/',2)::uuid; exception when invalid_text_representation then return false; end;
 return private.can_access_booking(context_id) or exists(select 1 from public.complaints where id=context_id and reporter_id=auth.uid() and status<>'resolved');
end;
$$;
revoke all on function private.can_upload_evidence(text) from public,anon;
grant execute on function private.can_upload_evidence(text) to authenticated;
create policy evidence_upload on storage.objects for insert to authenticated
 with check(bucket_id='service-evidence' and private.can_upload_evidence(name));
create policy evidence_download on storage.objects for select to authenticated using (
 bucket_id='service-evidence' and (
   private.can_upload_evidence(name) or exists(select 1 from public.service_attachments where object_path=name)
 )
);
-- Published evidence is immutable. Only abandoned uploads can be deleted by their owner.
create policy evidence_abandoned_delete on storage.objects for delete to authenticated using (
 bucket_id='service-evidence' and private.can_upload_evidence(name) and
 not exists(select 1 from public.service_attachments where object_path=name)
);
create function private.publish_service_attachment(p_id uuid,p_booking_id uuid,p_complaint_id uuid,p_path text)
returns public.service_attachments language plpgsql security definer set search_path='' as $$
declare result public.service_attachments; meta jsonb; mime text; bytes bigint;
begin
 perform 1 from public.profiles where id=auth.uid() for update;
 if not private.can_upload_evidence(p_path) then raise exception 'Your booking or complaint is required' using errcode='42501'; end if;
 select * into result from public.service_attachments where id=p_id;
 if found then
   if result.owner_id<>auth.uid() or result.object_path<>p_path then raise exception 'Attachment unavailable' using errcode='42501'; end if;
   return result;
 end if;
 if num_nonnulls(p_booking_id,p_complaint_id)<>1 then raise exception 'One context is required'; end if;
 if p_booking_id is not null then
   perform 1 from public.bookings where id=p_booking_id for update;
 else
   perform 1 from public.complaints where id=p_complaint_id and reporter_id=auth.uid() and status<>'resolved' for update;
   if not found then raise exception 'Open complaint required' using errcode='42501'; end if;
 end if;
 if (select count(*) from public.service_attachments where owner_id=auth.uid() and
    (booking_id=p_booking_id or complaint_id=p_complaint_id))>=5 then raise exception 'Maximum five attachments per person per request' using errcode='22023'; end if;
 select metadata into meta from storage.objects where bucket_id='service-evidence' and name=p_path;
 mime:=meta->>'mimetype'; bytes:=(meta->>'size')::bigint;
 if mime is null or mime not in ('image/jpeg','video/mp4') or bytes is null or bytes not between 1 and 26214400 then
   raise exception 'Upload a JPEG image or MP4 video up to 25 MB' using errcode='22023'; end if;
 insert into public.service_attachments(id,owner_id,booking_id,complaint_id,object_path,media_type,byte_size)
 values(p_id,auth.uid(),p_booking_id,p_complaint_id,p_path,mime,bytes) returning * into result;
 return result;
end;
$$;
revoke all on function private.publish_service_attachment(uuid,uuid,uuid,text) from public,anon;
grant execute on function private.publish_service_attachment(uuid,uuid,uuid,text) to authenticated;
create function public.publish_service_attachment(p_id uuid,p_booking_id uuid,p_complaint_id uuid,p_path text)
returns public.service_attachments language sql security invoker set search_path='' as $$
 select private.publish_service_attachment(p_id,p_booking_id,p_complaint_id,p_path);
$$;
revoke all on function public.publish_service_attachment(uuid,uuid,uuid,text) from public,anon;
grant execute on function public.publish_service_attachment(uuid,uuid,uuid,text) to authenticated;

create policy customer_photo_read on storage.objects for select to authenticated using (
 bucket_id='customer-photos' and (private.has_role('admin') or (split_part(name,'/',1)=auth.uid()::text and private.has_role('customer')))
);
create policy customer_photo_insert on storage.objects for insert to authenticated with check (
 bucket_id='customer-photos' and private.has_role('customer') and name ~ ('^'||auth.uid()::text||'/[0-9a-f-]{36}[.]jpg$')
);
create policy customer_photo_delete on storage.objects for delete to authenticated using (
 bucket_id='customer-photos' and private.has_role('customer') and split_part(name,'/',1)=auth.uid()::text
);
create function private.set_customer_photo(p_avatar_path text) returns void
language plpgsql security definer set search_path='' as $$
begin
 perform 1 from public.profiles where id=auth.uid() for update;
 if not private.has_role('customer') then raise exception 'Active customer account required' using errcode='42501'; end if;
 if p_avatar_path is not null and (p_avatar_path !~ ('^'||auth.uid()::text||'/[0-9a-f-]{36}[.]jpg$') or not exists(
   select 1 from storage.objects where bucket_id='customer-photos' and name=p_avatar_path and
    metadata->>'mimetype'='image/jpeg' and (metadata->>'size')::bigint between 1 and 1048576
 )) then raise exception 'Upload your own JPEG photo before saving' using errcode='22023'; end if;
 update public.profiles set avatar_path=p_avatar_path where id=auth.uid();
end;
$$;
revoke all on function private.set_customer_photo(text) from public,anon;
grant execute on function private.set_customer_photo(text) to authenticated;
create function public.set_customer_photo(p_avatar_path text) returns void language sql security invoker set search_path='' as $$ select private.set_customer_photo(p_avatar_path); $$;
revoke all on function public.set_customer_photo(text) from public,anon;
grant execute on function public.set_customer_photo(text) to authenticated;
