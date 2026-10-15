"use client";
import { useCallback, useEffect, useState } from "react";
import { getSupabase } from "@/lib/supabase";
import type { DashboardData, Profile } from "@/lib/types";
import { Brand } from "./brand";
import { Records } from "./workspace-records";
import { Icon, type IconName } from "./icon";

const sections = [
  "Overview",
  "Providers",
  "Customers",
  "Bookings",
  "Services",
  "Activity",
  "Settings",
] as const;
export type Section = (typeof sections)[number];
const icons: Record<Section, IconName> = {
  Overview: "overview",
  Providers: "providers",
  Customers: "customers",
  Bookings: "bookings",
  Services: "services",
  Activity: "activity",
  Settings: "settings",
};
const descriptions: Record<Section, string> = {
  Overview: "A little clarity for every part of your service.",
  Providers: "Build a trusted network of home-service professionals.",
  Customers: "The people who trust Fixoom to care for their homes.",
  Bookings: "Keep every service request moving in the right direction.",
  Services: "A considered service catalog, ready for your customers.",
  Activity: "A clear record of provider reviews and booking cancellations.",
  Settings: "Manage your workspace identity and understand access controls.",
};
export function Workspace({
  profile,
  email,
  onSignOut,
}: {
  profile: Profile;
  email: string;
  onSignOut: () => Promise<void>;
}) {
  const [section, setSection] = useState<Section>("Overview");
  const [menu, setMenu] = useState(false);
  const [data, setData] = useState<DashboardData | null>(null);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState("");
  const [updated, setUpdated] = useState("");
  const [revision, setRevision] = useState(0);
  const refresh = useCallback(() => setRevision((n) => n + 1), []);
  const navigate = useCallback((next: Section) => {
    setSection(next);
    setMenu(false);
  }, []);
  useEffect(() => {
    function restoreSection() {
      const value = window.location.hash.slice(1);
      if (value === "workspace-content") return;
      const next = sections.find(
        (section) => section.toLowerCase() === value.toLowerCase(),
      );
      setSection(next ?? "Overview");
    }
    restoreSection();
    window.addEventListener("hashchange", restoreSection);
    return () => window.removeEventListener("hashchange", restoreSection);
  }, []);
  function goTo(next: Section) {
    navigate(next);
    window.history.pushState(null, "", `#${next.toLowerCase()}`);
  }
  useEffect(() => {
    const abort = new AbortController();
    async function load() {
      setBusy(true);
      setError("");
      try {
        const db = getSupabase();
        const results = await Promise.all([
          db
            .from("profiles")
            .select("id,full_name,phone,role,account_status,created_at")
            .order("created_at", { ascending: false })
            .limit(100)
            .abortSignal(abort.signal),
          db
            .from("provider_profiles")
            .select(
              "id,display_name,category_slug,service_area,bio,is_available",
            )
            .order("created_at", { ascending: false })
            .limit(100)
            .abortSignal(abort.signal),
          db
            .from("service_categories")
            .select("*")
            .order("sort_order")
            .abortSignal(abort.signal),
          db
            .from("bookings")
            .select("*")
            .order("created_at", { ascending: false })
            .limit(100)
            .abortSignal(abort.signal),
          db
            .from("admin_audit_log")
            .select("id,actor_id,action,subject_id,details,created_at")
            .order("created_at", { ascending: false })
            .limit(12)
            .abortSignal(abort.signal),
          db
            .from("profiles")
            .select("id", { count: "exact", head: true })
            .eq("role", "customer")
            .abortSignal(abort.signal),
          db
            .from("profiles")
            .select("id", { count: "exact", head: true })
            .eq("role", "provider")
            .abortSignal(abort.signal),
          db
            .from("profiles")
            .select("id", { count: "exact", head: true })
            .eq("role", "provider")
            .eq("account_status", "pending")
            .abortSignal(abort.signal),
          db
            .from("bookings")
            .select("id", { count: "exact", head: true })
            .abortSignal(abort.signal),
        ]);
        if (results.some((r) => r.error))
          throw new Error(
            "Unable to load your workspace. Check your connection or sign in again.",
          );
        if (abort.signal.aborted) return;
        setData({
          profiles: results[0].data ?? [],
          providers: results[1].data ?? [],
          categories: results[2].data ?? [],
          bookings: results[3].data ?? [],
          audit: results[4].data ?? [],
          counts: {
            customers: results[5].count ?? 0,
            providers: results[6].count ?? 0,
            pending: results[7].count ?? 0,
            bookings: results[8].count ?? 0,
          },
        } as DashboardData);
        setUpdated(
          new Date().toLocaleTimeString("en-LK", {
            timeZone: "Asia/Colombo",
            hour: "2-digit",
            minute: "2-digit",
          }),
        );
      } catch (error) {
        if (!abort.signal.aborted)
          setError(
            error instanceof Error ? error.message : "Unable to load data.",
          );
      } finally {
        if (!abort.signal.aborted) setBusy(false);
      }
    }
    void load();
    return () => abort.abort();
  }, [revision]);
  useEffect(() => {
    window.addEventListener("focus", refresh);
    return () => window.removeEventListener("focus", refresh);
  }, [refresh]);
  return (
    <div className="workspace">
      <a href="#workspace-content" className="skip-link">
        Skip to content
      </a>
      <aside className="sidebar">
        <Brand light />
        <div className="workspace-switch">
          <span className="workspace-monogram">F</span>
          <div>
            Fixoom Services<small>Operations workspace</small>
          </div>
          <span className="workspace-badge">ADMIN</span>
        </div>
        <button
          className="mobile-menu button"
          aria-expanded={menu}
          aria-controls="workspace-navigation"
          onClick={() => setMenu(!menu)}
        >
          <Icon name={menu ? "close" : "menu"} />
          {menu ? "Close navigation" : "Workspace menu"}
        </button>
        <div
          id="workspace-navigation"
          className={menu ? "navigation navigation-open" : "navigation"}
        >
          <span className="nav-label">WORKSPACE</span>
          <nav aria-label="Workspace sections">
            {sections.map((name) => (
              <button
                key={name}
                className={section === name ? "nav-item selected" : "nav-item"}
                onClick={() => goTo(name)}
                aria-current={section === name ? "page" : undefined}
              >
                <Icon name={icons[name]} />
                {name}
                {name === "Providers" && Boolean(data?.counts.pending) && (
                  <b>{data?.counts.pending}</b>
                )}
              </button>
            ))}
          </nav>
        </div>
        <div className="sidebar-footer">
          <div className="sidebar-note">
            <Icon name="home" size={25} />
            <strong>
              Better care.
              <br />
              Connected together.
            </strong>
            <p>Customer. Professional. Fixoom.</p>
          </div>
          <button
            className="nav-item sign-out"
            onClick={() =>
              void onSignOut().catch(() =>
                setError("Sign out failed. Check your connection and retry."),
              )
            }
          >
            <Icon name="logout" />
            Sign out
          </button>
        </div>
      </aside>
      <div className="workspace-main">
        <header className="topbar">
          <div className="breadcrumbs">
            <Icon name="home" size={16} />
            <span>Workspace</span>
            <span>/</span>
            <strong>{section}</strong>
          </div>
          <div className="topbar-right">
            <span className="admin-pill">
              <Icon name="shield" size={14} />
              Admin workspace
            </span>
            <div className="admin-identity">
              <span className="avatar">
                {(profile.full_name || email || "A")[0].toUpperCase()}
              </span>
              <span>
                {profile.full_name || "Administrator"}
                <small>Administrator</small>
              </span>
            </div>
          </div>
        </header>
        <main id="workspace-content" className="content" tabIndex={-1}>
          <div className="page-heading">
            <div>
              <span className="eyebrow">YOUR FIXOOM WORKSPACE</span>
              <h1>
                {section === "Overview"
                  ? `Welcome back${profile.full_name.trim() ? `, ${profile.full_name.trim().split(/\s+/)[0]}` : ""}.`
                  : section === "Activity"
                    ? "Workspace activity"
                    : section === "Providers"
                      ? "Service providers"
                      : section === "Services"
                        ? "Service catalog"
                        : section}
              </h1>
              <p>{descriptions[section]}</p>
            </div>
            <button
              className="button refresh"
              disabled={busy}
              onClick={refresh}
            >
              <Icon name="refresh" size={17} />
              {busy ? "Refreshing…" : "Refresh data"}
            </button>
          </div>
          <div className="sync-line" aria-live="polite">
            <span className={error ? "sync-dot failed" : "sync-dot"} />
            {busy
              ? "Loading workspace data…"
              : error
                ? "Refresh failed"
                : `Updated ${updated} · Asia/Colombo`}
          </div>
          {error && (
            <div className="alert alert-error" role="alert">
              {error}
              {data && " Previously loaded records may be out of date."}
              <button onClick={refresh}>Retry</button>
            </div>
          )}
          {busy && !data && (
            <div
              className="skeleton-grid"
              role="status"
              aria-label="Loading workspace"
            >
              {[0, 1, 2, 3].map((n) => (
                <div className="skeleton" key={n} />
              ))}
            </div>
          )}
          {data && (
            <Records
              key={section}
              section={section}
              data={data}
              adminEmail={email}
              onNavigate={goTo}
              onRefresh={refresh}
              revision={revision}
            />
          )}
        </main>
        <footer className="workspace-bottom">
          <span>© Fixoom Services</span>
          <span>Thoughtful care. Better service.</span>
        </footer>
      </div>
    </div>
  );
}
