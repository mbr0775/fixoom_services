export function Brand({ light = false }: { light?: boolean }) {
  return <div className={`brand ${light ? "brand-light" : ""}`}><span className="brand-mark" aria-hidden="true">F<span /></span><span className="brand-word">fixoom<span>Operations workspace</span></span></div>;
}
export function Status({ value }: { value: string }) {
  return <span className={`status status-${value}`}><i aria-hidden="true" />{value.replaceAll("_", " ")}</span>;
}
