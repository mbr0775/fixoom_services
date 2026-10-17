-- Signup descriptions are untrusted offering requests, never approval or role claims.
create or replace function private.register_initial_skills()
returns trigger language plpgsql security definer set search_path='' as $$
declare metadata jsonb; choices jsonb; descriptions jsonb;
begin
 select raw_user_meta_data into metadata from auth.users where id=new.id;
 choices:=metadata->'skill_categories';
 descriptions:=metadata->'skill_descriptions';
 if jsonb_typeof(choices)='array' then
   insert into public.provider_skills(provider_id,category_slug,description)
   select new.id,c.slug,
     case when jsonb_typeof(descriptions->c.slug)='string'
       and length(trim(descriptions->>c.slug)) between 10 and 2000
       then trim(descriptions->>c.slug) else '' end
   from public.service_categories c
   where c.is_active and c.slug in (select jsonb_array_elements_text(choices))
   order by c.sort_order,c.slug limit 12;
 end if;
 return new;
end;$$;
revoke all on function private.register_initial_skills() from public,anon,authenticated;
