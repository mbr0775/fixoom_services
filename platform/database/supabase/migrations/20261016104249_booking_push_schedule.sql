-- Immediate dispatch after transaction commit; cron retries network failures without a mobile app open.
create extension if not exists pg_net with schema extensions;
create extension if not exists pg_cron with schema pg_catalog;
create function private.kick_booking_push() returns void language plpgsql security definer set search_path='' as $$begin
 if exists(select 1 from vault.secrets where name='fixoom_fcm_service_account') and exists(
 select 1 from private.push_outbox o join private.push_devices d on d.id=o.device_id join public.notifications n on n.id=o.notification_id
 where ((o.state='pending' and o.next_attempt_at<=now()) or (o.state='sending' and o.claimed_at<now()-interval '5 minutes')) and d.enabled and d.user_id=n.recipient_id
 ) then
 perform net.http_post(url:='https://mzkrxovluljvsqzbowwb.supabase.co/functions/v1/dispatch-booking-alerts',
 headers:=jsonb_build_object('Content-Type','application/json','x-fixoom-dispatch-key',(select decrypted_secret from vault.decrypted_secrets where name='fixoom_push_dispatch_key' limit 1)),body:='{}'::jsonb,timeout_milliseconds:=10000);
 end if;
end;$$;
revoke all on function private.kick_booking_push() from public,anon,authenticated;
create or replace function private.enqueue_booking_push() returns trigger language plpgsql security definer set search_path='' as $$begin
 if new.actor_id is distinct from new.recipient_id then
 insert into private.push_outbox(notification_id,device_id) select new.id,d.id from private.push_devices d where d.user_id=new.recipient_id and d.enabled and d.updated_at>now()-interval '60 days';
 perform private.kick_booking_push();
 end if;return new;
end;$$;
select cron.schedule('fixoom-booking-push-retry','* * * * *',$cron$select private.kick_booking_push();$cron$);
