export const formatDate = (value: string) =>
  new Intl.DateTimeFormat("en-LK", {
    timeZone: "Asia/Colombo",
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
export const displayStatus = (value: string) => value.replaceAll("_", " ");

// Quote every cell, and prevent spreadsheet formula execution in user-supplied values.
export function csvContent(rows: (string | number | null | undefined)[][]) {
  return (
    "\uFEFF" +
    rows
      .map((row) =>
        row
          .map((cell) => {
            let value = String(cell ?? "");
            if (/^[\s\u0000-\u001f]*[=+@-]/.test(value)) value = "'" + value;
            return `"${value.replaceAll('"', '""')}"`;
          })
          .join(","),
      )
      .join("\r\n")
  );
}
export function downloadCsv(
  filename: string,
  rows: (string | number | null | undefined)[][],
) {
  const url = URL.createObjectURL(
    new Blob([csvContent(rows)], { type: "text/csv;charset=utf-8;" }),
  );
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
