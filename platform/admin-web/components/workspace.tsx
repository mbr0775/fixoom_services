"use client";
import { useCallback, useEffect, useState } from "react";
import { getSupabase } from "@/lib/supabase";
import type { DashboardData, Profile } from "@/lib/types";
import { Brand } from "./brand";
import { Records } from "./workspace-records";

const sections = ["Overview", "Providers", "Customers", "Bookings", "Services", "Settings"] as const;
export type Section = typeof sections[number];
const symbols = ["◫", "♧", "◎", "▤", "◇", "⚙"];

export function Workspace({ profile, email, onSignOut }: { profile: Profile; email: string; onSignOut: () => Promise<void> }) {
  const [section, setSection] = useState<Section>("Overview");
  const [data, setData] = useState<DashboardData | null>(null);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState("");
  const [updated, setUpdated] = useState<string>("");
  const [revision, setRevision] = useState(0);
  const refresh = useCallback(() => setRevision(n => n + 1), []);

  useEffect(() => {
    const abort = new AbortController();
    async function load() {
      setBusy(true); setError("");
      try {
        const db = getSupabase();
        const results = await Promise.all([
          db.from("profiles").select("id,full_name,phone,role,account_status,created_at").order("created_at", { ascending: false }).limit(100).abortSignal(abort.signal),
          db.from("provider_profiles").select("id,display_name,category_slug,service_area,bio,is_available").order("created_at", { ascending: false }).limit(100).abortSignal(abort.signal),
          db.from("service_categories").select("*").order("sort_order").abortSignal(abort.signal),
          db.from("bookings").select("*").order("created_at", { ascending: false }).limit(100).abortSignal(abort.signal),
          db.from("admin_audit_log").select("id,action,subject_id,details,created_at").order("created_at", { ascending: false }).limit(12).abortSignal(abort.signal),
          db.from("profiles").select("id", { count: "exact", head: true }).eq("role", "customer").abortSignal(abort.signal),
          db.from("profiles").select("id", { count: "exact", head: true }).eq("role", "provider").abortSignal(abort.signal),
          db.from("profiles").select("id", { count: "exact", head: true }).eq("role", "provider").eq("account_status", "pending").abortSignal(abort.signal),
          db.from("bookings").select("id", { count: "exact", head: true }).abortSignal(abort.signal),
        ]);
        if (results.some(r => r.error)) throw new Error("Unable to load your workspace. Check your connection or sign in again.");
        if (abort.signal.aborted) return;
        setData({ profiles: results[0].data ?? [], providers: results[1].data ?? [], categories: results[2].data ?? [], bookings: results[3].data ?? [], audit: results[4].data ?? [], counts: { customers: results[5].count ?? 0, providers: results[6].count ?? 0, pending: results[7].count ?? 0, bookings: results[8].count ?? 0 } } as DashboardData);
        setUpdated(new Date().toLocaleTimeString("en-LK", { timeZone: "Asia/Colombo", hour: "2-digit", minute: "2-digit" }));
      } catch (e) {
        if (!abort.signal.aborted) setError(e instanceof Error ? e.message : "Unable to load data.");
      } finally { if (!abort.signal.aborted) setBusy(false); }
    }
    void load(); return () => abort.abort();
  }, [revision]);

  useEffect(() => {
    const onFocus = () => refresh();
    window.addEventListener("focus", onFocus);
    return () => window.removeEventListener("focus", onFocus);
  }, [refresh]);

  return <div className="workspace">
    <aside className="sidebar"><Brand /><span className="nav-label">WORKSPACE</span>
      <nav aria-label="Workspace sections">{sections.map((name, i) => <button key={name} className={section === name ? "nav-item selected" : "nav-item"} onClick={() => setSection(name)} aria-current={section === name ? "page" : undefined}><span aria-hidden="true">{symbols[i]}</span>{name}{name === "Providers" && Boolean(data?.counts.pending) && <b>{data?.counts.pending}</b>}</button>)}</nav>
      <div className="sidebar-footer"><div className="workspace-label"><span className="online-dot" />Fixoom Services<span>Customer + Provider</span></div><button className="nav-item sign-out" onClick={() => void onSignOut().catch(() => setError("Sign out failed. Check your connection and retry."))}><span aria-hidden="true">↗</span>Sign out</button></div>
    </aside>
    <div className="workspace-main"><header className="topbar"><div><span className="breadcrumb">Workspace</span><span aria-hidden="true"> / </span><strong>{section}</strong></div><div className="admin-identity"><span className="avatar">{(profile.full_name || email)[0]?.toUpperCase()}</span><span>{profile.full_name || "Administrator"}<small>Administrator</small></span></div></header>
      <main className="content"><div className="page-heading"><div><span className="eyebrow">FIXOOM OPERATIONS</span><h1>{section === "Overview" ? "A clearer view of your service." : section}</h1><p>{section === "Overview" ? "Keep your team approved, your customers supported, and your bookings moving." : descriptions[section]}</p></div><button className="button refresh" disabled={busy} onClick={refresh}><span aria-hidden="true">↻</span>{busy ? "Refreshing…" : "Refresh"}</button></div>
        <div className="sync-line" aria-live="polite"><span className="online-dot" />{busy ? "Loading from Supabase…" : error ? "Refresh failed" : `Last refreshed ${updated} · Sri Lanka time`}</div>
        {error && <div className="alert alert-error" role="alert">{error}{data && " Previously loaded records may be out of date."}<button onClick={refresh}>Retry</button></div>}
        {busy && !data && <div className="empty-state"><span className="spinner" /><h2>Loading your workspace</h2></div>}
        {data && <Records key={section} section={section} data={data} adminEmail={email} onNavigate={setSection} onRefresh={refresh} />}
      </main><footer className="workspace-bottom">Fixoom Services <span>One team. Better service.</span></footer>
    </div>
  </div>;
}
const descriptions: Record<Section, string> = {
  Overview: "", Providers: "Review professionals before they can receive customer jobs.", Customers: "See the people using Fixoom to care for their homes.", Bookings: "Follow requests from the first booking to the completed service.", Services: "Control which service categories customers can discover.", Settings: "Your workspace identity and official contact information.",
};
