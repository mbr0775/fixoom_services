export type Profile = {
  id: string;
  full_name: string;
  phone: string | null;
  role: "customer" | "provider" | "admin";
  account_status: "pending" | "active" | "rejected" | "suspended" | "deleted";
  created_at: string;
};
export type Provider = {
  id: string;
  display_name: string;
  category_slug: string | null;
  service_area: string;
  bio: string;
  is_available: boolean;
};
export type Category = {
  slug: string;
  name: string;
  description: string;
  is_active: boolean;
  sort_order: number;
};
export type Booking = {
  id: string;
  reference: string;
  customer_id: string;
  provider_id: string;
  category_slug: string;
  scheduled_at: string;
  service_address: string;
  problem_description: string;
  status: string;
  created_at: string;
};
export type AuditEntry = {
  id: string;
  actor_id: string | null;
  action: string;
  subject_id: string;
  details: { decision?: string; status?: string; previous_status?: string };
  created_at: string;
};
export type DashboardData = {
  profiles: Profile[];
  providers: Provider[];
  categories: Category[];
  bookings: Booking[];
  audit: AuditEntry[];
  counts: {
    customers: number;
    providers: number;
    pending: number;
    bookings: number;
  };
};
