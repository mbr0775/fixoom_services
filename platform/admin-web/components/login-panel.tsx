"use client";
import { useState, type FormEvent } from "react";
import { getSupabase } from "@/lib/supabase";
import { Brand } from "./brand";

export function LoginPanel() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function signIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError("");
    try {
      const { error } = await getSupabase().auth.signInWithPassword({ email: email.trim(), password });
      if (error) throw error;
      setPassword("");
    } catch { setError("Unable to sign in. Check your email and password, then try again."); }
    finally { setBusy(false); }
  }
  return <main className="login-page">
    <section className="login-story"><Brand light />
      <div className="login-message"><span className="eyebrow">CARE FOR EVERY HOME. CONTROL FOR YOUR TEAM.</span>
        <h1>Good service<br />starts here<span>.</span></h1>
        <p>Your customers, your service professionals, and every job in between. One place to keep Fixoom moving.</p>
        <div className="story-flow"><span>Customer</span><b aria-hidden="true">↗</b><span>Provider</span><b aria-hidden="true">↗</b><span>Admin</span></div>
      </div><a href="mailto:fixoomservices@gmail.com" className="story-footer">fixoomservices@gmail.com <span aria-hidden="true">↗</span></a>
    </section>
    <section className="login-form-section"><div className="login-form"><span className="eyebrow">FIXOOM ADMIN</span><h2>Welcome back.</h2><p>Sign in to your operations workspace.</p>
      <form onSubmit={signIn}>
        <label htmlFor="email">Email address</label><input id="email" type="email" autoComplete="username" placeholder="you@company.com" value={email} onChange={e => setEmail(e.target.value)} required maxLength={254} />
        <label htmlFor="password">Password</label><input id="password" type="password" autoComplete="current-password" value={password} onChange={e => setPassword(e.target.value)} required />
        {error && <p className="alert alert-error" role="alert">{error}</p>}
        <button className="button button-primary" disabled={busy}>{busy ? "Signing in…" : "Open workspace"}<span aria-hidden="true">↗</span></button>
      </form><div className="login-note"><span aria-hidden="true">◈</span><p>Access is reserved for approved administrators. Customer and provider accounts use the mobile apps.</p></div>
    </div><small>Fixoom Services · Made for better service.</small></section>
  </main>;
}
