-- FCM delivery credentials remain in Vault; only the server role can claim/send jobs.
create table private.push_devices (
 id uuid primary key default gen_random_uuid(),user_id uuid not null references public.profiles(id) on delete cascade,
 installation_id uuid not null,app_role text not null check(app_role in ('customer','provider')),
 token text not null check(length(token) between 20 and 4096),enabled boolean not null default true,updated_at timestamptz not null default now(),
 unique(app_role,installation_id),unique(app_role,token)
);
create index push_devices_user_idx on private.push_devices(user_id);
alter table private.push_devices enable row level security;
revoke all on private.push_devices from public,anon,authenticated;
create table private.push_outbox (
 id uuid primary key default gen_random_uuid(),notification_id uuid not null references public.notifications(id) on delete cascade,
 device_id uuid not null references private.push_devices(id) on delete cascade,
 state text not null default 'pending' check(state in ('pending','sending','sent','dead')),
 attempts integer not null default 0,next_attempt_at timestamptz not null default now(),claimed_at timestamptz,sent_at timestamptz,last_error text,
 unique(notification_id,device_id)
);
create index push_outbox_due_idx on private.push_outbox(state,next_attempt_at);
create index push_outbox_device_idx on private.push_outbox(device_id);
alter table private.push_outbox enable row level security;
revoke all on private.push_outbox from public,anon,authenticated;
create function private.register_push_device(p_installation_id uuid,p_token text,p_app_role text) returns void language plpgsql security definer set search_path='' as $$
begin
 if p_app_role is null or p_app_role not in ('customer','provider') or not private.has_role(p_app_role::public.app_role) then raise exception 'Active application account required' using errcode='42501';end if;
 if p_installation_id is null or p_token is null or length(p_token) not between 20 and 4096 then raise exception 'Invalid notification registration';end if;
 delete from private.push_devices where app_role=p_app_role and ((token=p_token and installation_id<>p_installation_id) or (installation_id=p_installation_id and user_id<>auth.uid()));
 insert into private.push_devices(user_id,installation_id,app_role,token) values(auth.uid(),p_installation_id,p_app_role,p_token)
 on conflict(app_role,installation_id) do update set user_id=auth.uid(),token=excluded.token,enabled=true,updated_at=now();
end;$$;
revoke all on function private.register_push_device(uuid,text,text) from public,anon,authenticated;
grant execute on function private.register_push_device(uuid,text,text) to authenticated;
create function public.register_push_device(p_installation_id uuid,p_token text,p_app_role text) returns void language sql security invoker set search_path='' as $$select private.register_push_device(p_installation_id,p_token,p_app_role);$$;
revoke all on function public.register_push_device(uuid,text,text) from public,anon,authenticated;
grant execute on function public.register_push_device(uuid,text,text) to authenticated;
create function private.unregister_push_device(p_installation_id uuid) returns void language plpgsql security definer set search_path='' as $$begin
 delete from private.push_devices where user_id=auth.uid() and installation_id=p_installation_id;
end;$$;
revoke all on function private.unregister_push_device(uuid) from public,anon,authenticated;
grant execute on function private.unregister_push_device(uuid) to authenticated;
create function public.unregister_push_device(p_installation_id uuid) returns void language sql security invoker set search_path='' as $$select private.unregister_push_device(p_installation_id);$$;
revoke all on function public.unregister_push_device(uuid) from public,anon,authenticated;
grant execute on function public.unregister_push_device(uuid) to authenticated;
create function private.enqueue_booking_push() returns trigger language plpgsql security definer set search_path='' as $$begin
 if new.actor_id is distinct from new.recipient_id then
 insert into private.push_outbox(notification_id,device_id) select new.id,d.id from private.push_devices d where d.user_id=new.recipient_id and d.enabled and d.updated_at>now()-interval '60 days';
 end if;return new;
end;$$;
revoke all on function private.enqueue_booking_push() from public,anon,authenticated;
create trigger enqueue_booking_push after insert on public.notifications for each row execute function private.enqueue_booking_push();

-- These APIs never grant privileged credentials or device tokens to mobile/browser roles.
grant usage on schema private to service_role;
create function private.push_server_config() returns jsonb language sql stable security definer set search_path='' as $$
 select jsonb_build_object('service_account',(select decrypted_secret::jsonb from vault.decrypted_secrets where name='fixoom_fcm_service_account' limit 1),
 'dispatch_key',(select decrypted_secret from vault.decrypted_secrets where name='fixoom_push_dispatch_key' limit 1));
$$;
revoke all on function private.push_server_config() from public,anon,authenticated;
grant execute on function private.push_server_config() to service_role;
create function public.push_server_config() returns jsonb language sql security invoker set search_path='' as $$select private.push_server_config();$$;
revoke all on function public.push_server_config() from public,anon,authenticated;
grant execute on function public.push_server_config() to service_role;
create function private.claim_booking_push(p_limit integer) returns table(dispatch_id uuid,notification_id uuid,token text,recipient_id uuid,booking_id uuid,created_at timestamptz) language plpgsql security definer set search_path='' as $$begin
 update private.push_outbox set state='pending' where state='sending' and claimed_at<now()-interval '5 minutes';
 update private.push_outbox o set state='dead',last_error='expired_or_read' from public.notifications n where n.id=o.notification_id and o.state='pending' and (n.read_at is not null or n.created_at<now()-interval '1 day');
 return query with due as (
 select o.id from private.push_outbox o join private.push_devices d on d.id=o.device_id join public.notifications n on n.id=o.notification_id join public.profiles p on p.id=n.recipient_id
 where o.state='pending' and o.next_attempt_at<=now() and o.attempts<8 and d.enabled and d.user_id=n.recipient_id and d.updated_at>now()-interval '60 days' and p.account_status='active' and n.read_at is null and n.created_at>now()-interval '1 day'
 order by o.next_attempt_at,o.id limit least(greatest(p_limit,1),100) for update of o skip locked
 ), claimed as (update private.push_outbox o set state='sending',attempts=attempts+1,claimed_at=now() from due where o.id=due.id returning o.*)
 select c.id,n.id,d.token,n.recipient_id,n.booking_id,n.created_at from claimed c join private.push_devices d on d.id=c.device_id join public.notifications n on n.id=c.notification_id;
end;$$;
revoke all on function private.claim_booking_push(integer) from public,anon,authenticated;
grant execute on function private.claim_booking_push(integer) to service_role;
create function public.claim_booking_push(p_limit integer default 50) returns table(dispatch_id uuid,notification_id uuid,token text,recipient_id uuid,booking_id uuid,created_at timestamptz) language sql security invoker set search_path='' as $$select * from private.claim_booking_push(p_limit);$$;
revoke all on function public.claim_booking_push(integer) from public,anon,authenticated;
grant execute on function public.claim_booking_push(integer) to service_role;
create function private.finish_booking_push(p_id uuid,p_sent boolean,p_error text,p_invalid_token boolean) returns void language plpgsql security definer set search_path='' as $$begin
 if p_invalid_token then update private.push_devices set enabled=false where id=(select device_id from private.push_outbox where id=p_id and state='sending');end if;
 update private.push_outbox set state=case when p_sent then 'sent' when p_invalid_token or attempts>=8 then 'dead' else 'pending' end,
 sent_at=case when p_sent then now() else null end,last_error=left(p_error,80),next_attempt_at=now()+make_interval(secs=>least(3600,30*power(2,attempts))::integer)
 where id=p_id and state='sending';
end;$$;
revoke all on function private.finish_booking_push(uuid,boolean,text,boolean) from public,anon,authenticated;
grant execute on function private.finish_booking_push(uuid,boolean,text,boolean) to service_role;
create function public.finish_booking_push(p_id uuid,p_sent boolean,p_error text default null,p_invalid_token boolean default false) returns void language sql security invoker set search_path='' as $$select private.finish_booking_push(p_id,p_sent,p_error,p_invalid_token);$$;
revoke all on function public.finish_booking_push(uuid,boolean,text,boolean) from public,anon,authenticated;
grant execute on function public.finish_booking_push(uuid,boolean,text,boolean) to service_role;

-- A random server-to-server key is encrypted in Vault, never written into source.
do $$begin
 if not exists(select 1 from vault.secrets where name='fixoom_push_dispatch_key') then
  perform vault.create_secret(gen_random_uuid()::text||gen_random_uuid()::text,'fixoom_push_dispatch_key','Fixoom push dispatcher authentication');
 end if;
end;$$;
