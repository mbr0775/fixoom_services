/* Browser-only fixtures. All Supabase requests are intercepted; no test users or production records are created. */
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require(
  process.env.FIXOOM_PLAYWRIGHT_PATH || "playwright",
);
const base = process.env.FIXOOM_QA_URL || "http://localhost:3010";
const output =
  process.env.FIXOOM_QA_OUTPUT || path.resolve("artifacts/admin-qa");
fs.mkdirSync(output, { recursive: true });
const id = (n) => `00000000-0000-4000-8000-${String(n).padStart(12, "0")}`;
const created = "2026-10-14T09:00:00Z";
const admin = {
  id: id(1),
  full_name: "Demo Administrator",
  phone: null,
  role: "admin",
  account_status: "active",
  created_at: created,
};
const user = {
  id: admin.id,
  email: "admin@example.test",
  aud: "authenticated",
  role: "authenticated",
  app_metadata: {},
  user_metadata: {},
  created_at: created,
};
const profiles = [
  admin,
  ...Array.from({ length: 24 }, (_, i) => ({
    id: id(i + 10),
    full_name:
      i === 0 ? "=2+2" : `Demo Professional ${String(i + 1).padStart(2, "0")}`,
    phone: "+94770000000",
    role: "provider",
    account_status: i < 4 ? "pending" : "active",
    created_at: created,
  })),
  ...Array.from({ length: 23 }, (_, i) => ({
    id: id(i + 50),
    full_name: `Demo Customer ${i + 1}`,
    phone: null,
    role: "customer",
    account_status: "active",
    created_at: created,
  })),
];
const categories = [
  "Plumbing",
  "Electrical",
  "Carpentry",
  "Painting",
  "Masonry",
  "Cleaning",
].map((name, i) => ({
  slug: name.toLowerCase(),
  name,
  description: `Professional ${name.toLowerCase()} services for your home.`,
  is_active: true,
  sort_order: i,
}));
const providers = profiles
  .filter((p) => p.role === "provider")
  .map((p, i) => ({
    id: p.id,
    display_name: p.full_name,
    category_slug: categories[i % 6].slug,
    service_area: "Demo service area",
    bio: "Synthetic professional profile used only for browser testing.",
    is_available: true,
  }));
const bookings = [
  "requested",
  "accepted",
  "in_progress",
  "completed",
  "cancelled",
  "rejected",
].map((status, i) => ({
  id: id(i + 100),
  reference: `FX-DEMO-${i + 1}`,
  customer_id: id(50),
  provider_id: id(10),
  category_slug: "plumbing",
  scheduled_at: "2026-10-18T04:30:00Z",
  service_address: "Test address, Colombo",
  problem_description: "Synthetic service request for UI verification.",
  status,
  created_at: created,
}));
const audit = [
  {
    id: id(200),
    actor_id: admin.id,
    action: "provider_review",
    subject_id: id(10),
    details: { previous_status: "pending", decision: "active" },
    created_at: created,
  },
];
let writes = [],
  failWrite = false,
  failRead = false;
async function installMock(context, role = "admin") {
  const exp = Math.floor(Date.now() / 1000) + 3600;
  const jwt = [
    "e30",
    Buffer.from(
      JSON.stringify({ sub: admin.id, exp, aud: "authenticated" }),
    ).toString("base64url"),
    "fixture",
  ].join(".");
  await context.addInitScript(
    ({ session }) =>
      localStorage.setItem(
        "sb-mzkrxovluljvsqzbowwb-auth-token",
        JSON.stringify(session),
      ),
    {
      session: {
        access_token: jwt,
        refresh_token: "fixture",
        expires_at: exp,
        expires_in: 3600,
        token_type: "bearer",
        user,
      },
    },
  );
  await context.route("https://*.supabase.co/**", async (route) => {
    const request = route.request(),
      url = new URL(request.url()),
      table = url.pathname.split("/").pop();
    const reply = (data, status = 200, headers = {}) =>
      route.fulfill({
        status,
        contentType: "application/json",
        headers: {
          "access-control-allow-origin": "*",
          "access-control-expose-headers": "content-range",
          ...headers,
        },
        body: JSON.stringify(data),
      });
    if (url.pathname.endsWith("/auth/v1/user")) return reply(user);
    if (request.method() === "POST" || request.method() === "PATCH") {
      writes.push({ table, body: request.postDataJSON() });
      if (failWrite)
        return reply({ message: "Simulated permission error" }, 403);
      if (table === "review_provider") {
        const body = request.postDataJSON();
        profiles.find((p) => p.id === body.provider_id).account_status =
          body.decision;
        return reply(null);
      }
      if (table === "change_booking_status") {
        const body = request.postDataJSON();
        bookings.find((b) => b.id === body.booking_id).status =
          body.next_status;
        return reply(null);
      }
      if (table === "service_categories") {
        const slug = url.searchParams.get("slug").slice(3);
        categories.find((c) => c.slug === slug).is_active =
          request.postDataJSON().is_active;
        return reply({ slug });
      }
      return reply(null);
    }
    if (failRead && table === "bookings")
      return reply({ message: "Simulated offline state" }, 503);
    let rows =
      {
        profiles,
        provider_profiles: providers,
        service_categories: categories,
        bookings,
        admin_audit_log: audit,
      }[table] || [];
    for (const [key, value] of url.searchParams) {
      if (value.startsWith("eq."))
        rows = rows.filter((r) => String(r[key]) === value.slice(3));
      if (value.startsWith("in.")) {
        const values = value
          .slice(4, -1)
          .split(",")
          .map((v) => v.replaceAll('"', ""));
        rows = rows.filter((r) => values.includes(r[key]));
      }
    }
    const or = url.searchParams.get("or");
    if (or) {
      const term = or.match(/ilike\.%([^%]+)%/);
      if (term)
        rows = rows.filter((r) =>
          [
            r.full_name,
            r.phone,
            r.reference,
            r.service_address,
            r.problem_description,
          ].some((v) => v?.toLowerCase().includes(term[1].toLowerCase())),
        );
    }
    const total = rows.length,
      from = Number(url.searchParams.get("offset") || 0),
      limit = Number(url.searchParams.get("limit") || 1000);
    rows = rows.slice(from, from + limit);
    if (table === "profiles" && url.searchParams.get("id") === `eq.${admin.id}`)
      return reply({ ...admin, role });
    return reply(request.method() === "HEAD" ? null : rows, 200, {
      "content-range": `${from}-${Math.max(from, from + rows.length - 1)}/${total}`,
    });
  });
}
async function overflow(page) {
  assert.equal(
    await page.evaluate(
      () => document.documentElement.scrollWidth > innerWidth,
    ),
    false,
    "Horizontal page overflow",
  );
}
(async () => {
  const browser = await chromium.launch({ headless: true, channel: "chrome" });
  const originalContext = browser.newContext.bind(browser);
  browser.newContext = async (options) => {
    const context = await originalContext(options);
    context.setDefaultTimeout(15000);
    return context;
  };
  const errors = [];
  try {
    const loginContext = await browser.newContext();
    const login = await loginContext.newPage();
    login.on("pageerror", (e) => errors.push(e.message));
    await login.route("https://*.supabase.co/auth/v1/token**", (r) =>
      r.fulfill({
        status: 400,
        contentType: "application/json",
        body: JSON.stringify({
          code: "invalid_credentials",
          message: "Invalid login credentials",
        }),
      }),
    );
    for (const width of [1440, 1024, 390, 360]) {
      await login.setViewportSize({ width, height: 900 });
      await login.goto(base);
      await login.getByRole("heading", { name: "Welcome back." }).waitFor();
      await overflow(login);
      await login.screenshot({
        path: path.join(output, `login-${width}.png`),
        fullPage: true,
      });
    }
    await login
      .getByLabel("Password", { exact: true })
      .fill("synthetic-password");
    await login.getByRole("button", { name: "Show password" }).click();
    assert.equal(await login.locator("#password").getAttribute("type"), "text");
    await login.getByLabel("Email address").fill("invalid@example.test");
    await login.getByRole("button", { name: "Sign in to workspace" }).click();
    await login.locator("form [role=alert]").waitFor();
    await loginContext.close();
    const denied = await browser.newContext();
    await installMock(denied, "customer");
    const deniedPage = await denied.newPage();
    await deniedPage.goto(base);
    await deniedPage
      .getByText("This account does not have active administrator access.")
      .waitFor();
    assert.equal(await deniedPage.locator(".workspace").count(), 0);
    await denied.close();
    const context = await browser.newContext({
      viewport: { width: 1440, height: 1100 },
      hasTouch: true,
      acceptDownloads: true,
    });
    await installMock(context);
    const page = await context.newPage();
    page.on("pageerror", (e) => errors.push(e.message));
    await page.goto(base);
    await page.getByRole("heading", { name: "Welcome back, Demo." }).waitFor();
    await page.locator(".metrics").waitFor();
    await page.screenshot({
      path: path.join(output, "dashboard-desktop-fixture.png"),
      fullPage: true,
    });
    await overflow(page);
    const nav = (name) =>
      page
        .getByRole("navigation", { name: "Workspace sections" })
        .getByRole("button", { name: new RegExp("^" + name) });
    await nav("Providers").click();
    await page.getByText("1–20 of 24 records").waitFor();
    const statusFilter = page.getByRole("combobox", { name: "Filter status" });
    await statusFilter.click();
    await page.getByRole("option", { name: "Pending", exact: true }).click();
    await page.getByText("1–4 of 4 records").waitFor();
    await statusFilter.press("ArrowDown");
    await statusFilter.press("Home");
    await statusFilter.press("Enter");
    await page.getByText("1–20 of 24 records").waitFor();
    await statusFilter.press("Enter");
    await statusFilter.press("Escape");
    assert.equal(await statusFilter.getAttribute("aria-expanded"), "false");
    await page.getByLabel("Search providers").focus();
    await page.waitForFunction(
      () =>
        getComputedStyle(document.querySelector(".search-field"))
          .borderColor === "rgb(25, 72, 80)",
    );
    assert.equal(
      await page
        .getByLabel("Search providers")
        .evaluate((input) => getComputedStyle(input).outlineStyle),
      "none",
    );
    assert.equal(
      await page
        .locator(".search-field")
        .evaluate((field) => getComputedStyle(field).borderColor),
      "rgb(25, 72, 80)",
    );
    await page.screenshot({
      path: path.join(output, "search-focus-desktop-fixture.png"),
    });
    await page.getByRole("button", { name: "Next", exact: true }).click();
    await page.getByText("21–24 of 24 records").waitFor();
    await page.getByRole("button", { name: "Previous", exact: true }).click();
    await page.getByText("1–20 of 24 records").waitFor();
    const dl = page.waitForEvent("download");
    await page.getByRole("button", { name: "Export page" }).click();
    const download = await dl;
    const csv = fs.readFileSync(await download.path(), "utf8");
    assert.ok(csv.includes("'=2+2"), "CSV formula must be escaped");
    await page.getByLabel("Search providers").fill("unmatched professional");
    await page.getByText("No matching records.").waitFor();
    await page.getByLabel("Search providers").fill("");
    await page.getByText("1–20 of 24 records").waitFor();
    const firstRow = page.locator("tbody tr").first();
    await firstRow.getByRole("button", { name: "View profile" }).click();
    await page.getByRole("dialog").waitFor();
    await page.keyboard.press("Escape");
    assert.equal(await page.getByRole("dialog").count(), 0);
    await firstRow
      .getByRole("button", { name: "Approve", exact: true })
      .click();
    assert.equal(writes.length, 0);
    await page.getByRole("button", { name: "Go back" }).click();
    assert.equal(writes.length, 0);
    await firstRow
      .getByRole("button", { name: "Approve", exact: true })
      .click();
    await page.getByRole("button", { name: "Confirm change" }).click();
    await page.getByText("Changes saved successfully.").waitFor();
    assert.equal(writes[0].table, "review_provider");
    await nav("Bookings").click();
    await page.getByText("1–6 of 6 records").waitFor();
    await page
      .locator("tbody tr")
      .first()
      .getByRole("button", { name: "View booking" })
      .click();
    await page
      .getByText("Synthetic service request for UI verification.")
      .waitFor();
    await page
      .getByRole("button", { name: "Cancel booking", exact: true })
      .click();
    failWrite = true;
    await page.getByRole("button", { name: "Confirm change" }).click();
    await page.getByRole("dialog").getByRole("alert").waitFor();
    failWrite = false;
    await page.getByRole("button", { name: "Go back" }).click();
    await page
      .locator("tbody tr")
      .first()
      .getByRole("button", { name: "View booking" })
      .click();
    await page
      .getByRole("button", { name: "Cancel booking", exact: true })
      .click();
    await page.getByRole("button", { name: "Confirm change" }).click();
    await page.getByText("Changes saved successfully.").waitFor();
    assert.equal(bookings[0].status, "cancelled");
    await nav("Activity").click();
    await page.getByText("1–1 of 1 records").waitFor();
    await page.getByRole("combobox", { name: "Filter action" }).click();
    await page.getByRole("listbox").waitFor();
    await page.waitForFunction(
      () =>
        document.querySelector(".filter-menu") &&
        getComputedStyle(document.querySelector(".filter-menu")).opacity ===
          "1",
    );
    await page.screenshot({
      path: path.join(output, "dropdown-desktop-fixture.png"),
      animations: "disabled",
    });
    assert.equal(
      await page
        .getByRole("combobox", { name: "Filter action" })
        .getAttribute("aria-expanded"),
      "true",
    );
    await page.getByLabel("Search activity").click();
    assert.equal(await page.getByRole("listbox").count(), 0);
    await page.getByRole("button", { name: "View event" }).click();
    await page.getByText("Event ID", { exact: true }).waitFor();
    await page.keyboard.press("Escape");
    await nav("Services").click();
    await page
      .getByRole("heading", { name: "Plumbing", exact: true })
      .waitFor();
    await page.screenshot({
      path: path.join(output, "services-desktop-fixture.png"),
      fullPage: true,
    });
    await page
      .getByRole("button", { name: "Pause service", exact: true })
      .first()
      .click();
    await page.getByRole("button", { name: "Confirm change" }).click();
    await page.getByText("Changes saved successfully.").waitFor();
    await nav("Settings").click();
    await page.getByText("admin@example.test", { exact: true }).waitFor();
    await page.goBack();
    await page
      .getByRole("heading", { name: "Service catalog", exact: true })
      .waitFor();
    failRead = true;
    await nav("Bookings").click();
    await page
      .getByRole("heading", { name: "Unable to load records" })
      .waitFor();
    failRead = false;
    await page.getByRole("button", { name: "Retry", exact: true }).click();
    await page.getByText("1–6 of 6 records").waitFor();
    for (const width of [1440, 1024, 390, 360]) {
      await page.setViewportSize({ width, height: 1000 });
      if (width <= 700)
        await page.getByRole("button", { name: "Workspace menu" }).click();
      await nav("Overview").click();
      await overflow(page);
      await page.screenshot({
        path: path.join(output, `dashboard-${width}-fixture.png`),
        fullPage: true,
      });
      for (const section of [
        "Providers",
        "Customers",
        "Bookings",
        "Services",
        "Activity",
        "Settings",
      ]) {
        if (width <= 700)
          await page.getByRole("button", { name: "Workspace menu" }).click();
        await nav(section).click();
        await overflow(page);
        if (section === "Activity" && width <= 390) {
          const filter = page.getByRole("combobox", { name: "Filter action" });
          await filter.tap();
          await page.getByRole("listbox").waitFor();
          const box = await page.getByRole("listbox").boundingBox();
          assert.ok(
            box.x >= 0 && box.x + box.width <= width,
            "Menu must fit the viewport",
          );
          await page.screenshot({
            path: path.join(output, `dropdown-${width}-fixture.png`),
            animations: "disabled",
          });
          await page
            .getByRole("option", { name: "Provider review", exact: true })
            .tap();
          assert.equal(await filter.getAttribute("aria-expanded"), "false");
          await page.getByText("1–1 of 1 records").waitFor();
        }
      }
    }
    await page.emulateMedia({ reducedMotion: "reduce" });
    assert.equal(
      await page.evaluate(
        () => matchMedia("(prefers-reduced-motion: reduce)").matches,
      ),
      true,
    );
    await context.close();
    assert.deepEqual(errors, [], "Browser runtime errors");
    console.log(
      "PASS: login, password visibility, access denial, pagination, search, details, approval confirmation, write errors, CSV safety, catalog, activity, settings, 4 responsive widths, reduced motion.",
    );
  } finally {
    await browser.close();
  }
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
