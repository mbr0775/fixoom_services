"use client";
import { useEffect, useState } from "react";
import { getSupabase } from "@/lib/supabase";
import type {
  AuditEntry,
  Booking,
  DashboardData,
  Profile,
  Provider,
} from "@/lib/types";
import { displayStatus, downloadCsv, formatDate } from "@/lib/format";
import { Status } from "./brand";
import { Dialog } from "./dialog";
import { Empty } from "./overview";
import { Icon } from "./icon";

type ListSection = "Providers" | "Customers" | "Bookings" | "Activity";
type Rows = {
  profiles: Profile[];
  providers: Provider[];
  bookings: Booking[];
  audit: AuditEntry[];
  names: Profile[];
  total: number;
};
const blank: Rows = {
  profiles: [],
  providers: [],
  bookings: [],
  audit: [],
  names: [],
  total: 0,
};
const pageSize = 20;
export function RecordBrowser({
  section,
  data,
  revision,
  review,
  cancel,
}: {
  section: ListSection;
  data: DashboardData;
  revision: number;
  review: (id: string, name: string, decision: string) => void;
  cancel: (booking: Booking) => void;
}) {
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("all");
  const [page, setPage] = useState(0);
  const [rows, setRows] = useState<Rows>(blank);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const [detail, setDetail] = useState<Profile | Booking | AuditEntry | null>(
    null,
  );
  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery(search);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);
  useEffect(() => {
    const abort = new AbortController();
    async function load() {
      setBusy(true);
      setError("");
      const db = getSupabase();
      // Restrict filter syntax to literal words; punctuation never becomes PostgREST syntax.
      const term = query
        .trim()
        .replace(/[^\p{L}\p{N}\s-]/gu, "")
        .slice(0, 80);
      const from = page * pageSize;
      try {
        const result: Rows = { ...blank };
        if (section === "Providers" || section === "Customers") {
          let request = db
            .from("profiles")
            .select("id,full_name,phone,role,account_status,created_at", {
              count: "exact",
            })
            .eq("role", section === "Providers" ? "provider" : "customer");
          if (filter !== "all") request = request.eq("account_status", filter);
          if (term)
            request = request.or(
              `full_name.ilike.%${term}%,phone.ilike.%${term}%`,
            );
          const response = await request
            .order("created_at", { ascending: false })
            .order("id")
            .range(from, from + pageSize - 1)
            .abortSignal(abort.signal);
          if (response.error) throw response.error;
          result.profiles = response.data ?? [];
          result.total = response.count ?? 0;
          if (section === "Providers" && result.profiles.length) {
            const detail = await db
              .from("provider_profiles")
              .select(
                "id,display_name,category_slug,service_area,bio,is_available",
              )
              .in(
                "id",
                result.profiles.map((p) => p.id),
              )
              .abortSignal(abort.signal);
            if (detail.error) throw detail.error;
            result.providers = detail.data ?? [];
          }
        } else if (section === "Bookings") {
          let request = db.from("bookings").select("*", { count: "exact" });
          if (filter !== "all") request = request.eq("status", filter);
          if (term)
            request = request.or(
              `reference.ilike.%${term}%,service_address.ilike.%${term}%,problem_description.ilike.%${term}%`,
            );
          const response = await request
            .order("created_at", { ascending: false })
            .order("id")
            .range(from, from + pageSize - 1)
            .abortSignal(abort.signal);
          if (response.error) throw response.error;
          result.bookings = response.data ?? [];
          result.total = response.count ?? 0;
        } else {
          let request = db
            .from("admin_audit_log")
            .select("id,actor_id,action,subject_id,details,created_at", {
              count: "exact",
            });
          if (filter !== "all") request = request.eq("action", filter);
          if (term)
            request = request.ilike("action", `%${term.replaceAll(" ", "_")}%`);
          const response = await request
            .order("created_at", { ascending: false })
            .order("id")
            .range(from, from + pageSize - 1)
            .abortSignal(abort.signal);
          if (response.error) throw response.error;
          result.audit = response.data ?? [];
          result.total = response.count ?? 0;
        }
        const ids = [
          ...new Set([
            ...result.bookings.flatMap((b) => [b.customer_id, b.provider_id]),
            ...result.audit
              .flatMap((a) => [
                a.actor_id,
                ...(a.action === "provider_review" ? [a.subject_id] : []),
              ])
              .filter((id): id is string => Boolean(id)),
          ]),
        ];
        if (ids.length) {
          const names = await db
            .from("profiles")
            .select("id,full_name,phone,role,account_status,created_at")
            .in("id", ids)
            .abortSignal(abort.signal);
          if (names.error) throw names.error;
          result.names = names.data ?? [];
        }
        if (!abort.signal.aborted) {
          if (page > 0 && result.total <= from)
            setPage(Math.max(0, Math.ceil(result.total / pageSize) - 1));
          else setRows(result);
        }
      } catch {
        if (!abort.signal.aborted)
          setError(
            "Records could not be loaded. Check your connection and try again.",
          );
      } finally {
        if (!abort.signal.aborted) setBusy(false);
      }
    }
    void load();
    return () => abort.abort();
  }, [section, query, filter, page, revision, retry]);
  const nameOf = (id: string | null) =>
    rows.names.find((p) => p.id === id)?.full_name ||
    data.profiles.find((p) => p.id === id)?.full_name ||
    (id ? `${id.slice(0, 8)}…` : "Unknown administrator");
  const categoryOf = (slug: string | null) =>
    data.categories.find((c) => c.slug === slug)?.name ?? "Not set";
  const list =
    section === "Bookings"
      ? rows.bookings
      : section === "Activity"
        ? rows.audit
        : rows.profiles;
  const statuses =
    section === "Bookings"
      ? [
          "requested",
          "accepted",
          "in_progress",
          "completed",
          "cancelled",
          "rejected",
        ]
      : section === "Activity"
        ? ["provider_review", "booking_cancelled"]
        : ["pending", "active", "rejected", "suspended", "deleted"];
  function exportPage() {
    const values =
      section === "Bookings"
        ? [
            [
              "Reference",
              "Customer",
              "Provider",
              "Service",
              "Scheduled (Asia/Colombo)",
              "Status",
            ],
            ...rows.bookings.map((b) => [
              b.reference,
              nameOf(b.customer_id),
              nameOf(b.provider_id),
              categoryOf(b.category_slug),
              formatDate(b.scheduled_at),
              b.status,
            ]),
          ]
        : section === "Activity"
          ? [
              [
                "Action",
                "Administrator",
                "Subject ID",
                "Previous status",
                "Decision",
                "Timestamp (Asia/Colombo)",
              ],
              ...rows.audit.map((a) => [
                a.action,
                nameOf(a.actor_id),
                a.subject_id,
                a.details.previous_status,
                a.details.decision ?? a.details.status,
                formatDate(a.created_at),
              ]),
            ]
          : [
              ["Name", "Phone", "Status", "Joined (Asia/Colombo)"],
              ...rows.profiles.map((p) => [
                p.full_name,
                p.phone,
                p.account_status,
                formatDate(p.created_at),
              ]),
            ];
    downloadCsv(`fixoom-${section.toLowerCase()}-page-${page + 1}.csv`, values);
  }
  return (
    <>
      <section className="panel records-panel" aria-busy={busy}>
        <div className="records-toolbar">
          <label className="search-field">
            <Icon name="search" size={18} />
            <input
              aria-label={`Search ${section.toLowerCase()}`}
              placeholder={
                section === "Bookings"
                  ? "Reference, address or description…"
                  : section === "Activity"
                    ? "Search actions…"
                    : "Search name or phone…"
              }
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              maxLength={80}
            />
          </label>
          <div className="toolbar-actions">
            <select
              aria-label="Filter status"
              value={filter}
              onChange={(event) => {
                setFilter(event.target.value);
                setPage(0);
              }}
            >
              <option value="all">
                {section === "Activity" ? "All actions" : "All statuses"}
              </option>
              {statuses.map((status) => (
                <option key={status} value={status}>
                  {displayStatus(status)}
                </option>
              ))}
            </select>
            <button
              className="button"
              disabled={busy || !!error || !list.length}
              onClick={exportPage}
            >
              <Icon name="download" size={17} />
              Export page
            </button>
          </div>
        </div>
        {error ? (
          <div className="empty-state">
            <h3>Unable to load records</h3>
            <p role="alert">{error}</p>
            <button className="button" onClick={() => setRetry((n) => n + 1)}>
              Retry
            </button>
          </div>
        ) : busy ? (
          <div className="empty-state" role="status">
            <span className="spinner" />
            <h3>Loading records…</h3>
          </div>
        ) : !list.length ? (
          <Empty
            title={
              search || filter !== "all"
                ? "No matching records."
                : `No ${section.toLowerCase()} yet.`
            }
            text={
              search || filter !== "all"
                ? "Try another search or choose a different status."
                : "New records will appear here as your Fixoom platform grows."
            }
            icon={section === "Activity" ? "activity" : "search"}
          />
        ) : (
          <div className="table-scroll">
            <table>
              <caption className="sr-only">
                {section} · page {page + 1}
              </caption>
              <thead>
                <tr>
                  {(section === "Providers"
                    ? ["Professional", "Service & area", "Status", "Actions"]
                    : section === "Customers"
                      ? ["Customer", "Phone", "Joined", "Status", "Details"]
                      : section === "Bookings"
                        ? [
                            "Booking",
                            "Customer & provider",
                            "Scheduled",
                            "Status",
                            "Details",
                          ]
                        : [
                            "Action",
                            "Administrator",
                            "Subject",
                            "Timestamp",
                            "Details",
                          ]
                  ).map((heading) => (
                    <th scope="col" key={heading}>
                      {heading}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.profiles.map((p) => {
                  const provider = rows.providers.find(
                    (detail) => detail.id === p.id,
                  );
                  return (
                    <tr key={p.id}>
                      <td>
                        <div className="person-cell">
                          <span className="avatar">
                            {(p.full_name || "?")[0].toUpperCase()}
                          </span>
                          <div>
                            <strong>{p.full_name || "Unnamed account"}</strong>
                            <small>
                              {section === "Providers"
                                ? p.phone || "No phone supplied"
                                : p.id.slice(0, 8)}
                            </small>
                          </div>
                        </div>
                      </td>
                      {section === "Providers" ? (
                        <td>
                          <strong>
                            {categoryOf(provider?.category_slug ?? null)}
                          </strong>
                          <small>
                            {provider?.service_area ||
                              "Service profile incomplete"}
                          </small>
                        </td>
                      ) : (
                        <>
                          <td>{p.phone || "Not supplied"}</td>
                          <td>{formatDate(p.created_at)}</td>
                        </>
                      )}
                      <td>
                        <Status value={p.account_status} />
                      </td>
                      <td>
                        <div className="row-actions">
                          <button onClick={() => setDetail(p)}>
                            View profile
                          </button>
                          {section === "Providers" &&
                            p.account_status !== "active" &&
                            p.account_status !== "deleted" && (
                              <button
                                onClick={() =>
                                  review(p.id, p.full_name, "active")
                                }
                              >
                                Approve
                              </button>
                            )}
                          {section === "Providers" &&
                            p.account_status === "pending" && (
                              <button
                                className="danger-text"
                                onClick={() =>
                                  review(p.id, p.full_name, "rejected")
                                }
                              >
                                Reject
                              </button>
                            )}
                          {section === "Providers" &&
                            p.account_status === "active" && (
                              <button
                                className="danger-text"
                                onClick={() =>
                                  review(p.id, p.full_name, "suspended")
                                }
                              >
                                Suspend
                              </button>
                            )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {rows.bookings.map((b) => (
                  <tr key={b.id}>
                    <td>
                      <strong>{b.reference}</strong>
                      <small>{categoryOf(b.category_slug)}</small>
                    </td>
                    <td>
                      <strong>{nameOf(b.customer_id)}</strong>
                      <small>{nameOf(b.provider_id)}</small>
                    </td>
                    <td>{formatDate(b.scheduled_at)}</td>
                    <td>
                      <Status value={b.status} />
                    </td>
                    <td>
                      <button
                        className="text-button"
                        onClick={() => setDetail(b)}
                      >
                        View booking <Icon name="arrow" size={15} />
                      </button>
                    </td>
                  </tr>
                ))}
                {rows.audit.map((a) => (
                  <tr key={a.id}>
                    <td>
                      <strong>{displayStatus(a.action)}</strong>
                      <small>
                        {a.details.decision
                          ? `Decision: ${displayStatus(a.details.decision)}`
                          : a.details.status
                            ? `Status: ${a.details.status}`
                            : "Recorded action"}
                      </small>
                    </td>
                    <td>{nameOf(a.actor_id)}</td>
                    <td>
                      {a.action === "provider_review"
                        ? nameOf(a.subject_id)
                        : `${a.subject_id.slice(0, 8)}…`}
                    </td>
                    <td>{formatDate(a.created_at)}</td>
                    <td>
                      <button
                        className="text-button"
                        onClick={() => setDetail(a)}
                      >
                        View event
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div className="table-footer">
          <span>
            {busy
              ? "Updating results…"
              : `${rows.total ? page * pageSize + 1 : 0}–${Math.min((page + 1) * pageSize, rows.total)} of ${rows.total.toLocaleString()} records`}
          </span>
          <div className="pagination">
            <button
              className="button"
              disabled={busy || page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              Previous
            </button>
            <span>Page {page + 1}</span>
            <button
              className="button"
              disabled={busy || (page + 1) * pageSize >= rows.total}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </button>
          </div>
        </div>
      </section>
      <p className="list-note">
        Search and filters run across all records. CSV exports include the
        current page only.
      </p>
      {detail && (
        <Dialog
          title={
            "reference" in detail
              ? detail.reference
              : "action" in detail
                ? "Activity details"
                : detail.full_name || "Account details"
          }
          onClose={() => setDetail(null)}
        >
          {"reference" in detail ? (
            <>
              <Status value={detail.status} />
              <dl className="detail-list">
                <div>
                  <dt>Customer</dt>
                  <dd>{nameOf(detail.customer_id)}</dd>
                </div>
                <div>
                  <dt>Service professional</dt>
                  <dd>{nameOf(detail.provider_id)}</dd>
                </div>
                <div>
                  <dt>Service</dt>
                  <dd>{categoryOf(detail.category_slug)}</dd>
                </div>
                <div>
                  <dt>Scheduled</dt>
                  <dd>{formatDate(detail.scheduled_at)}</dd>
                </div>
                <div>
                  <dt>Requested</dt>
                  <dd>{formatDate(detail.created_at)}</dd>
                </div>
                <div>
                  <dt>Service address</dt>
                  <dd>{detail.service_address}</dd>
                </div>
                <div>
                  <dt>Customer request</dt>
                  <dd>{detail.problem_description}</dd>
                </div>
              </dl>
              {!["completed", "cancelled", "rejected"].includes(
                detail.status,
              ) && (
                <button
                  className="button button-danger"
                  onClick={() => {
                    cancel(detail);
                    setDetail(null);
                  }}
                >
                  Cancel booking
                </button>
              )}
            </>
          ) : "action" in detail ? (
            <dl className="detail-list">
              <div>
                <dt>Action</dt>
                <dd>{displayStatus(detail.action)}</dd>
              </div>
              <div>
                <dt>Administrator</dt>
                <dd>{nameOf(detail.actor_id)}</dd>
              </div>
              <div>
                <dt>Subject ID</dt>
                <dd className="mono">{detail.subject_id}</dd>
              </div>
              <div>
                <dt>Timestamp</dt>
                <dd>{formatDate(detail.created_at)}</dd>
              </div>
              <div>
                <dt>Previous status</dt>
                <dd>{detail.details.previous_status ?? "Not recorded"}</dd>
              </div>
              <div>
                <dt>Decision</dt>
                <dd>
                  {detail.details.decision ??
                    detail.details.status ??
                    "Not recorded"}
                </dd>
              </div>
              <div>
                <dt>Event ID</dt>
                <dd className="mono">{detail.id}</dd>
              </div>
            </dl>
          ) : (
            <>
              <Status value={detail.account_status} />
              <dl className="detail-list">
                <div>
                  <dt>Account type</dt>
                  <dd>{detail.role}</dd>
                </div>
                <div>
                  <dt>Phone</dt>
                  <dd>{detail.phone || "Not supplied"}</dd>
                </div>
                <div>
                  <dt>Joined</dt>
                  <dd>{formatDate(detail.created_at)}</dd>
                </div>
                <div>
                  <dt>Account ID</dt>
                  <dd className="mono">{detail.id}</dd>
                </div>
                {section === "Providers" &&
                  (() => {
                    const p = rows.providers.find((p) => p.id === detail.id);
                    return (
                      <>
                        <div>
                          <dt>Service</dt>
                          <dd>{categoryOf(p?.category_slug ?? null)}</dd>
                        </div>
                        <div>
                          <dt>Service area</dt>
                          <dd>{p?.service_area || "Not supplied"}</dd>
                        </div>
                        <div>
                          <dt>Availability</dt>
                          <dd>
                            {p?.is_available
                              ? "Available for requests"
                              : "Unavailable"}
                          </dd>
                        </div>
                        <div>
                          <dt>About</dt>
                          <dd>{p?.bio || "No introduction supplied"}</dd>
                        </div>
                      </>
                    );
                  })()}
              </dl>
            </>
          )}
        </Dialog>
      )}
    </>
  );
}
