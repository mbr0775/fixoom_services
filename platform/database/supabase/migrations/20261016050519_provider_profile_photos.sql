-- Private, immutable photo objects; publication follows provider approval.
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values('provider-photos','provider-photos',false,1048576,array['image/jpeg']);

alter table public.provider_profiles add column avatar_path text;
alter table public.provider_profiles add constraint provider_avatar_owned_path check (
  avatar_path is null or avatar_path ~ ('^'||id::text||'/[0-9a-f-]{36}[.]jpg$')
);
-- Clients must use the validated photo RPC, rather than publish arbitrary URLs.
revoke update(avatar_path) on public.profiles from authenticated;

create function private.can_manage_provider_photo(object_path text)
returns boolean language sql stable security definer set search_path='' as $$
  select exists(select 1 from public.profiles p where p.id=auth.uid()
    and p.role='provider' and p.account_status in ('pending','active')
    and object_path ~ ('^'||p.id::text||'/[0-9a-f-]{36}[.]jpg$'));
$$;
revoke all on function private.can_manage_provider_photo(text) from public,anon,authenticated;
grant execute on function private.can_manage_provider_photo(text) to authenticated;

create function private.can_read_provider_photo(object_path text)
returns boolean language sql stable security definer set search_path='' as $$
  select private.has_role('admin') or exists(
    select 1 from public.provider_profiles pp
    join public.profiles p on p.id=pp.id
    join public.service_categories c on c.slug=pp.category_slug
    where pp.avatar_path=object_path and p.role='provider' and p.account_status='active'
      and c.is_active and trim(pp.display_name)<>'' and trim(pp.service_area)<>''
  );
$$;
revoke all on function private.can_read_provider_photo(text) from public,anon,authenticated;
grant execute on function private.can_read_provider_photo(text) to anon,authenticated;

create policy provider_photo_read on storage.objects for select to anon,authenticated
using (bucket_id='provider-photos' and (
  private.can_read_provider_photo(name) or
  (auth.uid() is not null and (storage.foldername(name))[1]=auth.uid()::text)
));
create policy provider_photo_insert on storage.objects for insert to authenticated
with check (bucket_id='provider-photos' and private.can_manage_provider_photo(name));
create policy provider_photo_delete on storage.objects for delete to authenticated
using (bucket_id='provider-photos' and private.can_manage_provider_photo(name));
-- No UPDATE policy: every replacement uses a fresh path, avoiding stale caches.

create function private.set_provider_photo(p_avatar_path text)
returns void language plpgsql security definer set search_path='' as $$
declare account public.profiles;
begin
  select * into account from public.profiles where id=auth.uid() for update;
  if account.id is null or account.role<>'provider' or account.account_status not in ('pending','active') then
    raise exception 'Provider account required' using errcode='42501';
  end if;
  if p_avatar_path is not null and (
    not private.can_manage_provider_photo(p_avatar_path) or not exists(
      select 1 from storage.objects where bucket_id='provider-photos' and name=p_avatar_path
        and metadata->>'mimetype'='image/jpeg'
        and (metadata->>'size')::bigint between 1 and 1048576
    )
  ) then raise exception 'Upload your own JPEG photo before saving it' using errcode='22023'; end if;
  update public.provider_profiles set avatar_path=p_avatar_path,updated_at=now() where id=account.id;
  if not found then raise exception 'Professional profile missing'; end if;
  update public.profiles set avatar_path=p_avatar_path where id=account.id;
end;
$$;
revoke all on function private.set_provider_photo(text) from public,anon,authenticated;
grant execute on function private.set_provider_photo(text) to authenticated;
create function public.set_provider_photo(p_avatar_path text)
returns void language sql security invoker set search_path='' as $$
  select private.set_provider_photo(p_avatar_path);
$$;
revoke all on function public.set_provider_photo(text) from public,anon;
grant execute on function public.set_provider_photo(text) to authenticated;
