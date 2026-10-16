import { createDispatcher, messagePayload, type PushJob } from "./fcm.ts";
const assert = (condition: unknown, message = "Assertion failed") => {
  if (!condition) throw new Error(message);
};
const key = "isolated-dispatch-test-".repeat(4);
const job: PushJob = {
  dispatch_id: "dispatch-fixture",
  notification_id: "notice-fixture",
  token: "fixture-token-never-used",
  recipient_id: "recipient-fixture",
  booking_id: "booking-fixture",
  created_at: "2026-10-16T10:00:00Z",
};
Deno.test("unauthenticated dispatcher cannot read config or claim jobs", async () => {
  let reads = 0;
  const handler = createDispatcher(async () => {
    reads++;
    return {};
  });
  const response = await handler(
    new Request("https://fixture.invalid", { method: "POST" }),
  );
  assert(response.status === 401);
  assert(reads === 0);
});
Deno.test("missing FCM credentials cannot report delivery success", async () => {
  let claims = 0;
  const handler = createDispatcher(async (name) => {
    if (name === "claim_booking_push") claims++;
    return { dispatch_key: key };
  });
  const response = await handler(
    new Request("https://fixture.invalid", {
      method: "POST",
      headers: { "x-fixoom-dispatch-key": key },
    }),
  );
  assert(response.status === 503);
  assert(claims === 0);
});
Deno.test("notification payload is generic and routes only to its protected booking", () => {
  const payload = messagePayload(job).message;
  assert(payload.data.fixoom_booking_id === job.booking_id);
  assert(payload.data.recipient_id === job.recipient_id);
  assert(payload.android.notification.tag === job.notification_id);
  assert(!JSON.stringify(payload.notification).includes("recipient-fixture"));
});
Deno.test("OAuth assertion is signed; unregistered devices are retired without a fake success", async () => {
  const pair = await crypto.subtle.generateKey(
    {
      name: "RSASSA-PKCS1-v1_5",
      modulusLength: 2048,
      publicExponent: new Uint8Array([1, 0, 1]),
      hash: "SHA-256",
    },
    true,
    ["sign", "verify"],
  );
  const pem = "-----BEGIN PRIVATE KEY-----\n" +
    btoa(
      String.fromCharCode(
        ...new Uint8Array(
          await crypto.subtle.exportKey("pkcs8", pair.privateKey),
        ),
      ),
    ) + "\n-----END PRIVATE KEY-----";
  const account = {
    project_id: "isolated-project",
    client_email: "fixture@example.invalid",
    private_key: pem,
  };
  let finished: Record<string, unknown> | undefined;
  const rpc = async (name: string, params?: Record<string, unknown>) => {
    if (name === "push_server_config") {
      return { dispatch_key: key, service_account: account };
    }
    if (name === "claim_booking_push") return [job];
    if (name === "finish_booking_push") {
      finished = params;
      return null;
    }
    throw new Error("Unexpected RPC");
  };
  const request =
    (async (url: string | URL | Request, options?: RequestInit) => {
      if (String(url) === "https://oauth2.googleapis.com/token") {
        const assertion = (options!.body as URLSearchParams).get("assertion")!;
        const [head, claim, signature] = assertion.split(".");
        const decoded = JSON.parse(
          atob(claim.replaceAll("-", "+").replaceAll("_", "/")),
        );
        assert(decoded.aud === "https://oauth2.googleapis.com/token");
        assert(decoded.exp - decoded.iat === 3600);
        assert(
          await crypto.subtle.verify(
            "RSASSA-PKCS1-v1_5",
            pair.publicKey,
            Uint8Array.from(
              atob(signature.replaceAll("-", "+").replaceAll("_", "/")),
              (c) => c.charCodeAt(0),
            ),
            new TextEncoder().encode(head + "." + claim),
          ),
        );
        return Response.json({ access_token: "isolated-oauth-token" });
      }
      assert(
        String(url) ===
          "https://fcm.googleapis.com/v1/projects/isolated-project/messages:send",
      );
      return Response.json({
        error: { details: [{ errorCode: "UNREGISTERED" }] },
      }, { status: 404 });
    }) as typeof fetch;
  const response = await createDispatcher(rpc, request)(
    new Request("https://fixture.invalid", {
      method: "POST",
      headers: { "x-fixoom-dispatch-key": key },
      body: JSON.stringify({ recipient_id: "attacker" }),
    }),
  );
  const result = await response.json();
  assert(result.sent === 0 && result.claimed === 1);
  assert(
    finished?.p_sent === false && finished?.p_invalid_token === true &&
      finished?.p_id === job.dispatch_id,
  );
});
