"use client";
import { useState, type FormEvent } from "react";
import { getSupabase } from "@/lib/supabase";
import { Brand } from "./brand";
import { Icon } from "./icon";

export function LoginPanel() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [visible, setVisible] = useState(false);
  async function signIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const { error } = await getSupabase().auth.signInWithPassword({
        email: email.trim(),
        password,
      });
      if (error) throw error;
      setPassword("");
    } catch {
      setError(
        "Unable to sign in. Check your email and password, then try again.",
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <main className="login-page">
      <section className="login-story">
        <Brand light />
        <div className="login-message">
          <span className="eyebrow">THE PEOPLE BEHIND BETTER SERVICE</span>
          <h1>
            Every home.
            <br />
            Every professional.
            <br />
            <em>One workspace.</em>
          </h1>
          <p>
            A thoughtful space to manage your people, care for your customers,
            and keep every service moving.
          </p>
          <div className="login-art" aria-hidden="true">
            <div className="art-grid" />
            <span className="art-orbit" />
            <div className="art-home">
              <Icon name="home" size={76} />
              <span>Care, connected.</span>
            </div>
            <span className="art-badge art-provider">
              <Icon name="providers" />
              Trusted professionals
            </span>
            <span className="art-badge art-check">
              <Icon name="check" />A better experience
            </span>
          </div>
          <div className="story-flow">
            <span>
              <Icon name="customers" size={16} />
              Customer app
            </span>
            <i />
            <span>
              <Icon name="providers" size={16} />
              Provider app
            </span>
            <i />
            <span>
              <Icon name="shield" size={16} />
              Admin
            </span>
          </div>
        </div>
        <a href="mailto:fixoomservices@gmail.com" className="story-footer">
          fixoomservices@gmail.com <Icon name="arrow" size={18} />
        </a>
      </section>
      <section className="login-form-section">
        <span className="login-access">
          <Icon name="shield" size={15} />
          ADMINISTRATOR ACCESS
        </span>
        <div className="login-form">
          <span className="form-symbol">
            <Icon name="overview" size={24} />
          </span>
          <span className="eyebrow">FIXOOM OPERATIONS</span>
          <h2>Welcome back.</h2>
          <p>Good to see you. Sign in to your workspace.</p>
          <form onSubmit={signIn}>
            <label htmlFor="email">Email address</label>
            <input
              id="email"
              type="email"
              autoComplete="username"
              placeholder="Enter your admin email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              maxLength={254}
              disabled={busy}
            />
            <label htmlFor="password">Password</label>
            <div className="password-field">
              <input
                id="password"
                type={visible ? "text" : "password"}
                autoComplete="current-password"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                disabled={busy}
              />
              <button
                type="button"
                className="icon-button"
                aria-label={visible ? "Hide password" : "Show password"}
                aria-pressed={visible}
                onClick={() => setVisible(!visible)}
              >
                <Icon name="eye" size={19} />
              </button>
            </div>
            {error && (
              <p className="alert alert-error" role="alert">
                {error}
              </p>
            )}
            <button className="button button-primary" disabled={busy}>
              {busy ? "Signing in…" : "Sign in to workspace"}
              <Icon name="arrow" size={19} />
            </button>
          </form>
          <div className="login-note">
            <Icon name="shield" size={20} />
            <p>
              For approved administrators. Use your existing Fixoom admin
              credentials to continue.
            </p>
          </div>
          <p className="login-help">
            Need access?{" "}
            <a href="mailto:fixoomservices@gmail.com">Contact Fixoom</a>
          </p>
        </div>
        <small>Fixoom Services · Thoughtful care. Better service.</small>
      </section>
    </main>
  );
}
