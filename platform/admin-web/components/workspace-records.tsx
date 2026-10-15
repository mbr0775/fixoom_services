"use client";
import { useState } from "react";
import { getSupabase } from "@/lib/supabase";
import type { Booking, DashboardData } from "@/lib/types";
import type { Section } from "./workspace";
import { Dialog } from "./dialog";
import { Status } from "./brand";
import { Icon } from "./icon";
import { Overview } from "./overview";
import { RecordBrowser } from "./record-browser";

type Action = { title: string; description: string; run: () => Promise<void> };
export function Records({
  section,
  data,
  adminEmail,
  onNavigate,
  onRefresh,
  revision,
}: {
  section: Section;
  data: DashboardData;
  adminEmail: string;
  onNavigate: (section: Section) => void;
  onRefresh: () => void;
  revision: number;
}) {
  const [pending, setPending] = useState<Action | null>(null);
  const [working, setWorking] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  async function perform() {
    if (!pending || working) return;
    setWorking(true);
    setError("");
    setNotice("");
    try {
      await pending.run();
      setNotice("Changes saved successfully.");
      setPending(null);
      onRefresh();
    } catch {
      setError(
        "The change could not be saved. Check your connection and try again.",
      );
    } finally {
      setWorking(false);
    }
  }
  function review(id: string, name: string, decision: string) {
    setPending({
      title: `${decision === "active" ? "Approve" : decision === "rejected" ? "Reject" : "Suspend"} ${name || "this provider"}?`,
      description:
        decision === "active"
          ? "This provider can sign in and receive requests after completing their service profile. Your decision will be recorded in the activity log."
          : "This provider will not be able to receive new requests. Your decision will be recorded in the activity log.",
      run: async () => {
        const { error } = await getSupabase().rpc("review_provider", {
          provider_id: id,
          decision,
        });
        if (error) throw error;
      },
    });
  }
  function cancel(booking: Booking) {
    setPending({
      title: `Cancel ${booking.reference}?`,
      description:
        "This closes the booking for both the customer and the provider. Cancellation is recorded in the activity log and cannot be reversed here.",
      run: async () => {
        const { error } = await getSupabase().rpc("change_booking_status", {
          booking_id: booking.id,
          next_status: "cancelled",
        });
        if (error) throw error;
      },
    });
  }
  return (
    <>
      {notice && (
        <div className="alert alert-success" role="status">
          <Icon name="check" size={18} />
          {notice}
          <button
            className="icon-button"
            aria-label="Dismiss notification"
            onClick={() => setNotice("")}
          >
            <Icon name="close" size={16} />
          </button>
        </div>
      )}
      {section === "Overview" && (
        <Overview data={data} onNavigate={onNavigate} />
      )}
      {(section === "Providers" ||
        section === "Customers" ||
        section === "Bookings" ||
        section === "Activity") && (
        <RecordBrowser
          section={section}
          data={data}
          revision={revision}
          review={review}
          cancel={cancel}
        />
      )}
      {section === "Services" && (
        <>
          <div className="catalog-heading">
            <span className="chip">{data.categories.length} categories</span>
            <p>
              {data.categories.filter((c) => c.is_active).length} open for new
              bookings
            </p>
          </div>
          <div className="services-grid">
            {data.categories.map((c, i) => (
              <section className="service-card panel" key={c.slug}>
                <div className="service-card-top">
                  <span
                    className={`service-illustration tone-${["mint", "lavender", "peach", "sky"][i % 4]}`}
                  >
                    <Icon name={i % 2 ? "services" : "home"} size={30} />
                  </span>
                  <span className="service-number">
                    {String(i + 1).padStart(2, "0")}
                  </span>
                </div>
                <h2>{c.name}</h2>
                <p>{c.description}</p>
                <div className="service-card-bottom">
                  <Status value={c.is_active ? "active" : "paused"} />
                  <button
                    className="text-button"
                    onClick={() =>
                      setPending({
                        title: `${c.is_active ? "Pause" : "Enable"} ${c.name.toLowerCase()}?`,
                        description: c.is_active
                          ? "Customers will not be able to submit new bookings in this category. Existing bookings will be preserved."
                          : "Customers will be able to request this service again.",
                        run: async () => {
                          const { error } = await getSupabase()
                            .from("service_categories")
                            .update({ is_active: !c.is_active })
                            .eq("slug", c.slug)
                            .select("slug")
                            .single();
                          if (error) throw error;
                        },
                      })
                    }
                  >
                    {c.is_active ? "Pause service" : "Enable service"}
                    <Icon name="arrow" size={16} />
                  </button>
                </div>
              </section>
            ))}
          </div>
        </>
      )}
      {section === "Settings" && (
        <div className="settings-grid">
          <section className="panel settings-panel">
            <span className="eyebrow">WORKSPACE IDENTITY</span>
            <h2>Your team. Your workspace.</h2>
            <dl className="detail-list">
              <div>
                <dt>Application</dt>
                <dd>Fixoom Services</dd>
              </div>
              <div>
                <dt>Administrator login</dt>
                <dd>{adminEmail}</dd>
              </div>
              <div>
                <dt>Official app email</dt>
                <dd>
                  <a href="mailto:fixoomservices@gmail.com">
                    fixoomservices@gmail.com
                  </a>
                </dd>
              </div>
              <div>
                <dt>Display timezone</dt>
                <dd>Asia/Colombo · Sri Lanka</dd>
              </div>
            </dl>
          </section>
          <section className="panel settings-panel">
            <span className="settings-symbol">
              <Icon name="shield" size={28} />
            </span>
            <h2>Controlled access.</h2>
            <p>
              Only active administrators can manage this workspace. Customer and
              provider accounts cannot grant administrator access.
            </p>
            <div className="settings-note">
              <Icon name="check" size={18} />
              <span>
                Provider reviews and booking cancellations are recorded in the
                activity log.
              </span>
            </div>
            <button
              className="text-button"
              onClick={() => onNavigate("Activity")}
            >
              View activity log <Icon name="arrow" size={16} />
            </button>
          </section>
        </div>
      )}
      {pending && (
        <Dialog
          title={pending.title}
          busy={working}
          onClose={() => {
            if (!working) {
              setPending(null);
              setError("");
            }
          }}
        >
          <p className="dialog-description">{pending.description}</p>
          {error && (
            <p className="alert alert-error" role="alert">
              {error}
            </p>
          )}
          <div className="actions">
            <button
              className="button"
              disabled={working}
              onClick={() => {
                setPending(null);
                setError("");
              }}
            >
              Go back
            </button>
            <button
              className="button button-primary"
              disabled={working}
              onClick={() => void perform()}
            >
              {working ? "Saving…" : "Confirm change"}
            </button>
          </div>
        </Dialog>
      )}
    </>
  );
}
