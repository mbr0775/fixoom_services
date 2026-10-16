import { createDispatcher } from "./fcm.ts";
const url = Deno.env.get("SUPABASE_URL")!;
const key = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
async function rpc(name: string, params: Record<string, unknown> = {}) {
  const response = await fetch(`${url}/rest/v1/rpc/${name}`, {
    method: "POST",
    headers: {
      apikey: key,
      Authorization: `Bearer ${key}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(params),
    signal: AbortSignal.timeout(10000),
  });
  if (!response.ok) throw new Error("server_operation_failed");
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}
// JWT verification is disabled only because this endpoint authenticates a Vault-held
// server-to-server dispatch key. It never accepts a client-supplied recipient or token.
Deno.serve(createDispatcher(rpc));
