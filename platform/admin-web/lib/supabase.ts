import { createClient, type SupabaseClient } from "@supabase/supabase-js";

export const isConfigured = Boolean(
  process.env.NEXT_PUBLIC_SUPABASE_URL &&
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY,
);
let client: SupabaseClient | undefined;
export function getSupabase() {
  if (!isConfigured) throw new Error("Supabase configuration is missing.");
  client ??= createClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
  );
  return client;
}
