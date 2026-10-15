import type { CSSProperties } from "react";

export type IconName =
  | "overview"
  | "providers"
  | "customers"
  | "bookings"
  | "services"
  | "activity"
  | "settings"
  | "arrow"
  | "refresh"
  | "search"
  | "close"
  | "menu"
  | "logout"
  | "download"
  | "shield"
  | "check"
  | "clock"
  | "eye"
  | "home";
const paths: Record<IconName, string> = {
  overview: "M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z",
  providers:
    "M14 6a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM2 21v-3a6 6 0 0 1 12 0v3 M17 10l2 2 4-4 M17 18h5",
  customers:
    "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM18 8a3 3 0 0 1 0 6 M22 21v-2a4 4 0 0 0-3-3.87",
  bookings:
    "M6 4h12a2 2 0 0 1 2 2v14H4V6a2 2 0 0 1 2-2ZM8 2v4 M16 2v4 M4 10h16 M8 14h3 M8 17h6",
  services: "m12 3 9 5-9 5-9-5 9-5ZM3 12l9 5 9-5 M3 16l9 5 9-5",
  activity: "M3 12h4l3-8 4 16 3-8h4",
  settings:
    "M12 8a4 4 0 1 1 0 8 4 4 0 0 1 0-8ZM9 3h6l1 3 3 1 2 5-2 5-3 1-1 3H9l-1-3-3-1-2-5 2-5 3-1 1-3Z",
  arrow: "M5 12h14 M13 6l6 6-6 6",
  refresh:
    "M20 7v5h-5 M4 17v-5h5 M5 7a8 8 0 0 1 13-2l2 3 M4 16l2 3a8 8 0 0 0 13-2",
  search: "M17 10a7 7 0 1 1-14 0 7 7 0 0 1 14 0ZM15 15l6 6",
  close: "m6 6 12 12 M6 18 18 6",
  menu: "M4 6h16 M4 12h16 M4 18h16",
  logout: "M10 4H4v16h6 M8 12h13 M16 7l5 5-5 5",
  download: "M12 3v12 M7 10l5 5 5-5 M4 16v5h16v-5",
  shield: "m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6l8-3ZM8 12l3 3 5-6",
  check: "m5 12 4 4L19 6",
  clock: "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0ZM12 7v5l3 2",
  eye: "M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7ZM15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z",
  home: "m3 10 9-7 9 7 M5 9v12h14V9 M9 21v-7h6v7",
};
export function Icon({
  name,
  size = 20,
  style,
}: {
  name: IconName;
  size?: number;
  style?: CSSProperties;
}) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      style={style}
    >
      <path d={paths[name]} />
    </svg>
  );
}
