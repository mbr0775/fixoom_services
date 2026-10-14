"use client";
import { useEffect, useState } from "react";
import type { User } from "@supabase/supabase-js";
import { getSupabase, isConfigured } from "@/lib/supabase";
import type { Profile } from "@/lib/types";
import { LoginPanel } from "./login-panel";
import { Workspace } from "./workspace";
import { Brand } from "./brand";
type Access = { profile: Profile; userId: string } | { error: string; userId: string };

export function AdminConsole() {
  const [user, setUser] = useState<User | null>(null);
  const [initialized, setInitialized] = useState(false);
  const [access, setAccess] = useState<Access | null>(null);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    if (!isConfigured) return;
    let mounted = true;
    const { data: { subscription } } = getSupabase().auth.onAuthStateChange((_, session) => {
      if (mounted) { setUser(session?.user ?? null); setInitialized(true); }
    });
    return () => { mounted = false; subscription.unsubscribe(); };
  }, []);
  useEffect(() => {
    if (!user) return;
    const abort = new AbortController();
    const userId = user.id;
    async function verify() {
      try {
        const supabase = getSupabase();
        const verified = await supabase.auth.getUser();
        if (verified.error || verified.data.user?.id !== userId) throw new Error("Your session has expired. Sign in again.");
        const result = await supabase.from("profiles").select("id,full_name,phone,role,account_status,created_at").eq("id", userId).abortSignal(abort.signal).single();
        if (result.error) throw new Error("We couldn’t verify access. Check your connection and retry.");
        const profile = result.data as Profile;
        if (profile.role !== "admin" || profile.account_status !== "active") throw new Error("This account does not have active administrator access.");
        if (!abort.signal.aborted) setAccess({ profile, userId });
      } catch (error) {
        if (!abort.signal.aborted) setAccess({ error: error instanceof Error ? error.message : "Unable to verify access.", userId });
      }
    }
    void verify(); return () => abort.abort();
  }, [user, retry]);
  async function signOut() {
    const { error } = await getSupabase().auth.signOut(); if (error) throw error;
    setAccess(null);
  }
  if (!isConfigured) return <main className="center-state"><Brand /><h1>Workspace setup required</h1><p>Configure the Supabase environment variables before using this workspace.</p></main>;
  if (!initialized || (user && access?.userId !== user.id)) return <main className="center-state"><Brand /><span className="spinner" /><p>Verifying workspace access…</p></main>;
  if (!user) return <LoginPanel />;
  if (access && "error" in access) return <main className="center-state"><Brand /><h1>Workspace access</h1><p role="alert">{access.error}</p><div className="actions"><button className="button" onClick={() => setRetry(n => n + 1)}>Retry</button><button className="button button-primary" onClick={() => void signOut().catch(() => setAccess({ userId: user.id, error: "Sign out failed. Check your connection and retry." }))}>Sign out</button></div></main>;
  if (!access || !("profile" in access)) return null;
  return <Workspace key={user.id} profile={access.profile} email={user.email ?? ""} onSignOut={signOut} />;
}
