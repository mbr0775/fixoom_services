-- Keep the legacy claim contract for older senders; v2 adds trusted inbox content.
create function private.claim_booking_push_v2(p_limit integer)
returns table(dispatch_id uuid,notification_id uuid,token text,recipient_id uuid,booking_id uuid,created_at timestamptz,kind text,title text,body text)
language sql security definer set search_path='' as $$
 select c.*,n.kind,n.title,n.body from private.claim_booking_push(p_limit) c
 join public.notifications n on n.id=c.notification_id and n.recipient_id=c.recipient_id;
$$;
revoke all on function private.claim_booking_push_v2(integer) from public,anon,authenticated;
grant execute on function private.claim_booking_push_v2(integer) to service_role;
create function public.claim_booking_push_v2(p_limit integer default 50)
returns table(dispatch_id uuid,notification_id uuid,token text,recipient_id uuid,booking_id uuid,created_at timestamptz,kind text,title text,body text)
language sql security invoker set search_path='' as $$select * from private.claim_booking_push_v2(p_limit);$$;
revoke all on function public.claim_booking_push_v2(integer) from public,anon,authenticated;
grant execute on function public.claim_booking_push_v2(integer) to service_role;

-- Notification content is produced by the real booking/message transaction, never a client payload.
create or replace function private.notify_booking_change() returns trigger
language plpgsql security definer set search_path='' as $$
declare booking public.bookings; headline text; event_kind text; detail text; sender_name text;
begin
 if tg_table_name='booking_messages' then
   select * into booking from public.bookings where id=new.booking_id;
   sender_name:=case when new.sender_id=booking.customer_id then booking.customer_name else booking.provider_name end;
   insert into public.notifications(recipient_id,actor_id,booking_id,kind,title,body)
   values(case when new.sender_id=booking.customer_id then booking.provider_id else booking.customer_id end,
     new.sender_id,booking.id,'message','New message from '||left(coalesce(nullif(trim(sender_name),''),'your booking contact'),60),
     booking.reference||' · '||booking.category_name||'. Open your booking to read the message.');
 else
   booking:=new;
   if tg_op='UPDATE' and new.status is not distinct from old.status and new.direct_payment_received is not distinct from old.direct_payment_received then return new;end if;
   event_kind:=case when tg_op='UPDATE' and new.direct_payment_received and not old.direct_payment_received then 'direct_payment_received' else new.status end;
   headline:=case event_kind when 'requested' then 'New service request' when 'accepted' then 'Booking accepted' when 'in_progress' then 'Service started' when 'completed' then 'Service completed' when 'cancelled' then 'Booking cancelled' when 'rejected' then 'Booking declined' when 'direct_payment_received' then 'Payment receipt recorded' else 'Booking updated' end;
   detail:=booking.reference||' · '||booking.category_name;
   if event_kind='requested' then
     detail:=left(coalesce(nullif(trim(booking.customer_name),''),'Customer'),60)||' requested '||booking.category_name||' · '||booking.reference;
   elsif event_kind in ('accepted','in_progress','completed','rejected','direct_payment_received') then
     detail:=detail||' · '||left(coalesce(nullif(trim(booking.provider_name),''),'Service provider'),60);
   end if;
   insert into public.notifications(recipient_id,actor_id,booking_id,kind,title,body)
   select recipient,auth.uid(),booking.id,event_kind,headline,detail from unnest(array[booking.customer_id,booking.provider_id]) recipient;
 end if;
 return new;
end;$$;
revoke all on function private.notify_booking_change() from public,anon,authenticated;
