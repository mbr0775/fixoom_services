export type ServiceAccount = {
  client_email: string;
  private_key: string;
  project_id: string;
};
export type PushJob = {
  dispatch_id: string;
  notification_id: string;
  token: string;
  recipient_id: string;
  booking_id: string;
  created_at: string;
  kind: string;
  title: string;
  body: string;
};
type Rpc = (name: string, params?: Record<string, unknown>) => Promise<unknown>;
const base64url = (value: Uint8Array) =>
  btoa(String.fromCharCode(...value)).replaceAll("+", "-").replaceAll("/", "_")
    .replaceAll("=", "");
const encoded = (value: unknown) =>
  base64url(new TextEncoder().encode(JSON.stringify(value)));

export async function googleAccessToken(
  account: ServiceAccount,
  request: typeof fetch = fetch,
) {
  const seconds = Math.floor(Date.now() / 1000);
  const head = encoded({ alg: "RS256", typ: "JWT" });
  const claim = encoded({
    iss: account.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: seconds,
    exp: seconds + 3600,
  });
  const pem = account.private_key.replace(/-----[^-]+-----/g, "").replace(
    /\s/g,
    "",
  );
  const bytes = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey(
    "pkcs8",
    bytes,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(head + "." + claim),
  );
  const response = await request("https://oauth2.googleapis.com/token", {
    method: "POST",
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: head + "." + claim + "." +
        base64url(new Uint8Array(signature)),
    }),
    signal: AbortSignal.timeout(10000),
  });
  if (!response.ok) throw new Error("google_auth_failed");
  const result = await response.json();
  if (typeof result.access_token !== "string") {
    throw new Error("google_auth_failed");
  }
  return result.access_token as string;
}
export function messagePayload(job: PushJob) {
  return {
    message: {
      token: job.token,
      notification: {
        title: job.title,
        body: job.body,
      },
      data: {
        notification_id: job.notification_id,
        fixoom_booking_id: job.booking_id,
        recipient_id: job.recipient_id,
        created_at: job.created_at,
        kind: job.kind,
      },
      android: {
        priority: "high",
        ttl: "86400s",
        notification: {
          channel_id: "fixoom_bookings",
          tag: job.notification_id,
          visibility: "PRIVATE",
          icon: "ic_fixoom_notification",
          defaultSound: true,
          defaultVibrateTimings: true,
          notificationPriority: "PRIORITY_HIGH",
        },
      },
    },
  };
}
async function sameKey(candidate: string, expected: string) {
  const hash = async (text: string) =>
    new Uint8Array(
      await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text)),
    );
  const [a, b] = await Promise.all([hash(candidate), hash(expected)]);
  let difference = 0;
  for (let i = 0; i < a.length; i++) difference |= a[i] ^ b[i];
  return difference === 0;
}
export function createDispatcher(rpc: Rpc, request: typeof fetch = fetch) {
  return async (req: Request) => {
    if (req.method !== "POST") {
      return new Response("Method not allowed", { status: 405 });
    }
    const presented = req.headers.get("x-fixoom-dispatch-key") ?? "";
    if (presented.length < 60 || presented.length > 200) {
      return new Response("Unauthorized", { status: 401 });
    }
    try {
      const config = await rpc("push_server_config") as {
        dispatch_key?: string;
        service_account?: ServiceAccount;
      };
      if (
        !config.dispatch_key ||
        !await sameKey(
          req.headers.get("x-fixoom-dispatch-key") ?? "",
          config.dispatch_key,
        )
      ) return new Response("Unauthorized", { status: 401 });
      if (!config.service_account) {
        return new Response("Push delivery is not configured", { status: 503 });
      }
      const account = config.service_account;
      const accessToken = await googleAccessToken(account, request);
      const jobs = await rpc("claim_booking_push_v2", {
        p_limit: 50,
      }) as PushJob[];
      let sent = 0;
      await Promise.all(jobs.map(async (job) => {
        let success = false, invalid = false, code: string | null = null;
        try {
          const response = await request(
            `https://fcm.googleapis.com/v1/projects/${
              encodeURIComponent(account.project_id)
            }/messages:send`,
            {
              method: "POST",
              headers: {
                Authorization: `Bearer ${accessToken}`,
                "Content-Type": "application/json",
              },
              body: JSON.stringify(messagePayload(job)),
              signal: AbortSignal.timeout(10000),
            },
          );
          success = response.ok;
          if (!success) {
            const result = await response.json().catch(() => ({}));
            invalid = Array.isArray(result.error?.details) &&
              result.error.details.some((d: { errorCode?: string }) =>
                d.errorCode === "UNREGISTERED"
              );
            code = invalid
              ? "unregistered_token"
              : `fcm_http_${response.status}`;
          }
        } catch {
          code = "fcm_network_error";
        }
        await rpc("finish_booking_push", {
          p_id: job.dispatch_id,
          p_sent: success,
          p_error: code,
          p_invalid_token: invalid,
        });
        if (success) sent++;
      }));
      return Response.json({ claimed: jobs.length, sent });
    } catch {
      return new Response("Push dispatch failed; queued requests will retry", {
        status: 503,
      });
    }
  };
}
