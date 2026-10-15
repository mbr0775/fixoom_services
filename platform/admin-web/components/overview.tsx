import type { DashboardData } from "@/lib/types";
import { formatDate, displayStatus } from "@/lib/format";
import type { Section } from "./workspace";
import { Status } from "./brand";
import { Icon, type IconName } from "./icon";

export function Empty({
  title,
  text,
  icon = "bookings",
}: {
  title: string;
  text: string;
  icon?: IconName;
}) {
  return (
    <div className="empty-state">
      <span className="empty-icon">
        <Icon name={icon} size={25} />
      </span>
      <h3>{title}</h3>
      <p>{text}</p>
    </div>
  );
}
export function Overview({
  data,
  onNavigate,
}: {
  data: DashboardData;
  onNavigate: (section: Section) => void;
}) {
  const metrics: {
    title: string;
    value: number;
    subtitle: string;
    icon: IconName;
    section: Section;
    tone: string;
  }[] = [
    {
      title: "Total customers",
      value: data.counts.customers,
      subtitle: "Registered customer accounts",
      icon: "customers",
      section: "Customers",
      tone: "mint",
    },
    {
      title: "Service providers",
      value: data.counts.providers,
      subtitle: "Professionals on your platform",
      icon: "providers",
      section: "Providers",
      tone: "lavender",
    },
    {
      title: "Pending approvals",
      value: data.counts.pending,
      subtitle: "Provider accounts awaiting review",
      icon: "clock",
      section: "Providers",
      tone: "peach",
    },
    {
      title: "Total bookings",
      value: data.counts.bookings,
      subtitle: "Requests across every status",
      icon: "bookings",
      section: "Bookings",
      tone: "sky",
    },
  ];
  const stages = [
    "requested",
    "accepted",
    "in_progress",
    "completed",
    "cancelled",
    "rejected",
  ];
  const activeServices = data.categories.filter((c) => c.is_active).length;
  return (
    <>
      <div className="metrics">
        {metrics.map((m) => (
          <button
            className="metric"
            key={m.title}
            onClick={() => onNavigate(m.section)}
          >
            <div>
              <span>{m.title}</span>
              <i className={`tone-${m.tone}`}>
                <Icon name={m.icon} />
              </i>
            </div>
            <strong>{m.value.toLocaleString()}</strong>
            <small>{m.subtitle}</small>
            <Icon name="arrow" size={16} />
          </button>
        ))}
      </div>
      <section className="approval-banner">
        <div className="banner-icon">
          <Icon name="shield" size={32} />
        </div>
        <div>
          <span className="eyebrow">QUALITY STARTS WITH YOUR TEAM</span>
          <h2>
            {data.counts.pending
              ? `${data.counts.pending} professional${data.counts.pending === 1 ? " is" : "s are"} waiting for a review.`
              : "A good service starts with the right people."}
          </h2>
          <p>
            {data.counts.pending
              ? "Review new applications and help your provider network grow."
              : "Approve your service professionals before they receive customer requests."}
          </p>
        </div>
        <button
          className="button button-lime"
          onClick={() => onNavigate("Providers")}
        >
          Review providers <Icon name="arrow" size={17} />
        </button>
      </section>
      <div className="overview-grid">
        <section className="panel">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">SERVICE OPERATIONS</span>
              <h2>Booking pipeline</h2>
            </div>
            <span className="chip">Latest {data.bookings.length} bookings</span>
          </div>
          <div className="pipeline">
            {stages.map((stage) => {
              const count = data.bookings.filter(
                (b) => b.status === stage,
              ).length;
              return (
                <div className="pipeline-row" key={stage}>
                  <span>{displayStatus(stage)}</span>
                  <div className="pipeline-track">
                    <span
                      className={`pipeline-fill fill-${stage}`}
                      style={{
                        width: `${data.bookings.length ? (count / data.bookings.length) * 100 : 0}%`,
                      }}
                    />
                  </div>
                  <strong>{count}</strong>
                </div>
              );
            })}
            <p className="muted">
              Status distribution of the latest 100 bookings, independent of
              scheduled date.
            </p>
          </div>
        </section>
        <section className="panel service-summary">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">YOUR SERVICE CATALOG</span>
              <h2>Ready for every home</h2>
            </div>
            <Icon name="home" />
          </div>
          <div className="service-total">
            <strong>
              {activeServices}
              <span> / {data.categories.length}</span>
            </strong>
            <p>categories open for new bookings</p>
          </div>
          <div className="category-chips">
            {data.categories.map((c) => (
              <span
                className={
                  c.is_active ? "category-chip" : "category-chip paused"
                }
                key={c.slug}
              >
                <i />
                {c.name}
              </span>
            ))}
          </div>
          <button
            className="text-button"
            onClick={() => onNavigate("Services")}
          >
            Manage service catalog <Icon name="arrow" size={16} />
          </button>
        </section>
      </div>
      <div className="overview-grid lower-grid">
        <section className="panel">
          <div className="panel-heading">
            <h2>Recent bookings</h2>
            <button
              className="text-button"
              onClick={() => onNavigate("Bookings")}
            >
              View all <Icon name="arrow" size={16} />
            </button>
          </div>
          {data.bookings.length ? (
            data.bookings.slice(0, 5).map((b) => (
              <div className="activity-row" key={b.id}>
                <span className="row-icon">
                  <Icon name="bookings" size={18} />
                </span>
                <div>
                  <strong>{b.reference}</strong>
                  <span>
                    {data.categories.find((c) => c.slug === b.category_slug)
                      ?.name ?? b.category_slug}{" "}
                    · {formatDate(b.scheduled_at)}
                  </span>
                </div>
                <Status value={b.status} />
              </div>
            ))
          ) : (
            <Empty
              title="Your next booking belongs here."
              text="Customer requests will appear once they are submitted through the connected booking flow."
            />
          )}
        </section>
        <section className="panel">
          <div className="panel-heading">
            <h2>Latest activity</h2>
            <button
              className="text-button"
              onClick={() => onNavigate("Activity")}
            >
              Activity log <Icon name="arrow" size={16} />
            </button>
          </div>
          {data.audit.length ? (
            data.audit.slice(0, 5).map((a) => (
              <div className="activity-row" key={a.id}>
                <span className="row-icon">
                  <Icon
                    name={
                      a.action === "provider_review" ? "shield" : "activity"
                    }
                    size={18}
                  />
                </span>
                <div>
                  <strong>
                    {a.action === "provider_review"
                      ? `Provider ${a.details.decision === "active" ? "approved" : (a.details.decision ?? "reviewed")}`
                      : displayStatus(a.action)}
                  </strong>
                  <span>{formatDate(a.created_at)}</span>
                </div>
              </div>
            ))
          ) : (
            <Empty
              title="Every decision, accounted for."
              text="Provider reviews and booking cancellations appear here with a timestamp and administrator."
              icon="activity"
            />
          )}
        </section>
      </div>
    </>
  );
}
