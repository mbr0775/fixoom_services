"use client";
import { useState, useRef, useEffect } from "react";
import { getSupabase } from "@/lib/supabase";
import type { DashboardData } from "@/lib/types";
import type { Section } from "./workspace";
import { Status } from "./brand";

type Action = { title: string; description: string; run: () => Promise<void> };
const formatDate = (date: string) => new Intl.DateTimeFormat("en-LK", { timeZone: "Asia/Colombo", dateStyle: "medium", timeStyle: "short" }).format(new Date(date));

export function Records({ section, data, adminEmail, onNavigate, onRefresh }: { section: Section; data: DashboardData; adminEmail: string; onNavigate: (section: Section) => void; onRefresh: () => void }) {
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("all");
  const [pending, setPending] = useState<Action | null>(null);
  const [working, setWorking] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const query = search.trim().toLowerCase();
  const matching = (values: (string | null)[]) => values.some(v => v?.toLowerCase().includes(query));
  const nameOf = (id: string) => data.profiles.find(p => p.id === id)?.full_name || `${id.slice(0, 8)}…`;
  const categoryOf = (slug: string | null) => data.categories.find(c => c.slug === slug)?.name ?? "Not set";

  async function perform() {
    if (!pending || working) return;
    setWorking(true); setError(""); setNotice("");
    try { await pending.run(); setNotice("Changes saved."); setPending(null); onRefresh(); }
    catch { setError("The change could not be saved. Check your connection and try again."); }
    finally { setWorking(false); }
  }
  function review(id: string, decision: string) {
    setPending({ title: `${decision === "active" ? "Approve" : decision === "rejected" ? "Reject" : "Suspend"} ${nameOf(id)}?`, description: decision === "active" ? "This provider will be allowed to sign in and receive jobs after completing their service profile." : "This provider will not be able to receive new jobs. You can review their access again later.", run: async () => { const { error } = await getSupabase().rpc("review_provider", { provider_id: id, decision }); if (error) throw error; } });
  }
  const providers = data.profiles.filter(p => p.role === "provider");

  return <>
    {notice && <div className="alert alert-success" role="status">{notice}</div>}
    {error && !pending && <div className="alert alert-error" role="alert">{error}</div>}
    {section === "Overview" && <>
      <div className="metrics">{[["Customers", data.counts.customers, "People using Fixoom", "◎"], ["Service providers", data.counts.providers, "Registered professionals", "♧"], ["Awaiting review", data.counts.pending, "Provider approvals", "◷"], ["Bookings", data.counts.bookings, "Requests across all statuses", "▤"]].map(([title, value, subtitle, icon]) => <div className="metric" key={title}><div><span>{title}</span><i aria-hidden="true">{icon}</i></div><strong>{value}</strong><small>{subtitle}</small></div>)}</div>
      <section className="approval-banner"><div><span className="eyebrow">YOUR NEXT STEP</span><h2>{data.counts.pending ? `${data.counts.pending} provider${data.counts.pending === 1 ? " is" : "s are"} waiting for review.` : "Ready for your first service professionals."}</h2><p>Review each provider before they become available to customers.</p></div><button className="button button-primary" onClick={() => onNavigate("Providers")}>Review providers <span aria-hidden="true">↗</span></button></section>
      <div className="overview-grid"><section className="panel"><div className="panel-heading"><h2>Recent bookings</h2><button className="text-button" onClick={() => onNavigate("Bookings")}>View bookings ↗</button></div>{data.bookings.length ? data.bookings.slice(0, 5).map(b => <div className="activity-row" key={b.id}><div><strong>{b.reference}</strong><span>{categoryOf(b.category_slug)} · {formatDate(b.scheduled_at)}</span></div><Status value={b.status} /></div>) : <Empty title="Your first booking starts the story." text="Bookings will appear here when customers send requests through the connected booking flow." />}</section>
      <section className="panel"><div className="panel-heading"><h2>Workspace activity</h2><span className="muted">Recent actions</span></div>{data.audit.length ? data.audit.map(a => <div className="activity-row" key={a.id}><div><strong>{a.action === "provider_review" ? `Provider ${a.details.decision === "active" ? "approved" : a.details.decision}` : "Booking cancelled"}</strong><span>{formatDate(a.created_at)}</span></div><span className="activity-symbol" aria-hidden="true">↗</span></div>) : <Empty title="A clean start." text="Provider reviews and booking cancellations are recorded here automatically." />}</section></div>
    </>}
    {(section === "Providers" || section === "Customers" || section === "Bookings") && <section className="panel records-panel">
      <div className="records-toolbar"><label className="search-field"><span aria-hidden="true">⌕</span><input aria-label={`Search ${section.toLowerCase()}`} placeholder={`Search ${section.toLowerCase()}…`} value={search} onChange={e => setSearch(e.target.value)} /></label>{section !== "Customers" && <select aria-label="Filter status" value={filter} onChange={e => setFilter(e.target.value)}><option value="all">All statuses</option>{(section === "Providers" ? ["pending", "active", "rejected", "suspended", "deleted"] : ["requested", "accepted", "in_progress", "completed", "cancelled", "rejected"]).map(s => <option key={s} value={s}>{s.replaceAll("_", " ")}</option>)}</select>}</div>
      <div className="table-scroll"><table><caption className="sr-only">{section}, most recent records</caption><thead><tr>{(section === "Providers" ? ["Professional", "Service / area", "Account status", "Review"] : section === "Customers" ? ["Customer", "Phone", "Joined", "Status"] : ["Booking", "Customer / provider", "Scheduled", "Status", "Action"]).map(h => <th key={h} scope="col">{h}</th>)}</tr></thead><tbody>
        {section === "Providers" && providers.filter(p => (filter === "all" || p.account_status === filter) && matching([p.full_name, p.phone, data.providers.find(x => x.id === p.id)?.service_area ?? null])).map(p => { const detail = data.providers.find(x => x.id === p.id); return <tr key={p.id}><td><strong>{p.full_name || "Unnamed provider"}</strong><small>{p.phone || "No phone supplied"}</small></td><td><strong>{categoryOf(detail?.category_slug ?? null)}</strong><small>{detail?.service_area || "Profile incomplete"}</small></td><td><Status value={p.account_status} /></td><td><div className="row-actions">{p.account_status !== "active" && p.account_status !== "deleted" && <button onClick={() => review(p.id, "active")}>Approve</button>}{p.account_status === "pending" && <button className="danger-text" onClick={() => review(p.id, "rejected")}>Reject</button>}{p.account_status === "active" && <button className="danger-text" onClick={() => review(p.id, "suspended")}>Suspend</button>}</div></td></tr>; })}
        {section === "Customers" && data.profiles.filter(p => p.role === "customer" && matching([p.full_name, p.phone])).map(p => <tr key={p.id}><td><strong>{p.full_name || "Unnamed customer"}</strong><small>{p.id.slice(0, 8)}</small></td><td>{p.phone || "—"}</td><td>{formatDate(p.created_at)}</td><td><Status value={p.account_status} /></td></tr>)}
        {section === "Bookings" && data.bookings.filter(b => (filter === "all" || b.status === filter) && matching([b.reference, nameOf(b.customer_id), nameOf(b.provider_id), b.service_address])).map(b => <tr key={b.id}><td><strong>{b.reference}</strong><small>{categoryOf(b.category_slug)}</small><details><summary>Service details</summary><p>{b.problem_description}</p><p>{b.service_address}</p></details></td><td><strong>{nameOf(b.customer_id)}</strong><small>{nameOf(b.provider_id)}</small></td><td>{formatDate(b.scheduled_at)}</td><td><Status value={b.status} /></td><td>{!["completed", "cancelled", "rejected"].includes(b.status) && <button className="text-button danger-text" onClick={() => setPending({ title: `Cancel ${b.reference}?`, description: "This closes the booking for both the customer and the provider. This action will be recorded in the activity log.", run: async () => { const { error } = await getSupabase().rpc("change_booking_status", { booking_id: b.id, next_status: "cancelled" }); if (error) throw error; } })}>Cancel booking</button>}</td></tr>)}
      </tbody></table></div>
      {!(section === "Providers" ? providers : section === "Customers" ? data.profiles.filter(p => p.role === "customer") : data.bookings).length && <Empty title={`No ${section.toLowerCase()} yet.`} text={section === "Providers" ? "New provider registrations will appear here for your review." : section === "Customers" ? "Customer accounts will appear here after registration." : "Customer service requests will appear here when submitted."} />}
      <div className="table-footer">Showing the latest 100 records at most. Search and filters apply to these records.</div>
    </section>}
    {section === "Services" && <div className="services-list">{data.categories.map((c, i) => <section className="service-row" key={c.slug}><span className="service-number">0{i + 1}</span><div><h2>{c.name}</h2><p>{c.description}</p></div><Status value={c.is_active ? "active" : "suspended"} /><button className="button" onClick={() => setPending({ title: `${c.is_active ? "Pause" : "Enable"} ${c.name.toLowerCase()}?`, description: c.is_active ? "Customers will not be able to submit new bookings in this category. Existing bookings are preserved." : "This category will be available for new bookings.", run: async () => { const { error } = await getSupabase().from("service_categories").update({ is_active: !c.is_active }).eq("slug", c.slug).select("slug").single(); if (error) throw error; } })}>{c.is_active ? "Pause service" : "Enable service"}</button></section>)}</div>}
    {section === "Settings" && <section className="panel settings-panel"><span className="eyebrow">WORKSPACE IDENTITY</span><h2>Built around your team.</h2><dl><div><dt>Application</dt><dd>Fixoom Services</dd></div><div><dt>Administrator login</dt><dd>{adminEmail}</dd></div><div><dt>Official app email</dt><dd><a href="mailto:fixoomservices@gmail.com">fixoomservices@gmail.com</a></dd></div><div><dt>Timezone</dt><dd>Asia/Colombo · Sri Lanka</dd></div></dl><p className="muted">Administrator access is managed in the protected database. Customer and provider registration cannot grant this role.</p></section>}
    {pending && <Confirmation action={pending} busy={working} error={error} onConfirm={() => void perform()} onDismiss={() => { if (!working) { setPending(null); setError(""); } }} />}
  </>;
}

function Empty({ title, text }: { title: string; text: string }) {
  return <div className="empty-state"><span className="empty-icon" aria-hidden="true">◇</span><h3>{title}</h3><p>{text}</p></div>;
}
function Confirmation({ action, busy, error, onConfirm, onDismiss }: { action: Action; busy: boolean; error: string; onConfirm: () => void; onDismiss: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null);
  useEffect(() => { dialog.current?.showModal(); }, []);
  return <dialog ref={dialog} className="confirm-dialog" aria-labelledby="confirm-title" onCancel={e => { e.preventDefault(); if (!busy) onDismiss(); }}><span className="eyebrow">CONFIRM CHANGE</span><h2 id="confirm-title">{action.title}</h2><p>{action.description}</p>{error && <p className="alert alert-error" role="alert">{error}</p>}<div className="actions"><button className="button" onClick={onDismiss} disabled={busy} autoFocus>Go back</button><button className="button button-primary" onClick={onConfirm} disabled={busy}>{busy ? "Saving…" : "Confirm"}</button></div></dialog>;
}
