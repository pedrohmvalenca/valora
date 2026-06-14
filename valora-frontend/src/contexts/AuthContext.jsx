import { createContext, useContext, useEffect, useState } from "react";
import * as authService from "@/services/auth";
import { Profile } from "@/lib/constants";

const PWA_STUDENT_BLOCKED_KEY = "valora.pwa_student_blocked";

const AuthContext = createContext(null);

async function bootstrapMe() {
  try {
    const data = await authService.me();
    const fetched = data?.user ?? null;
    if (fetched?.profile === Profile.STUDENT) {
      await authService.logout().catch(() => {});
      if (typeof window !== "undefined") {
        sessionStorage.setItem(PWA_STUDENT_BLOCKED_KEY, "1");
      }
      return { user: null, blockedStudent: true };
    }
    return { user: fetched, blockedStudent: false };
  } catch {
    return { user: null, blockedStudent: false };
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isBootstrapping, setIsBootstrapping] = useState(true);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    bootstrapMe().then((result) => {
      if (cancelled) return;
      setUser(result.user);
      setIsBootstrapping(false);
      setIsLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  async function login(email, password) {
    setIsLoading(true);
    try {
      const data = await authService.login(email, password);
      setUser(data?.user ?? null);
      return data?.user ?? null;
    } finally {
      setIsLoading(false);
    }
  }

  async function logout() {
    try {
      await authService.logout();
    } finally {
      setUser(null);
    }
  }

  function markPasswordChanged() {
    setUser((u) => (u ? { ...u, mustChangePassword: false } : u));
  }

  const value = {
    user,
    profile: user?.profile ?? null,
    isAuthenticated: user !== null,
    mustChangePassword: Boolean(user?.mustChangePassword),
    isBootstrapping,
    isLoading,
    login,
    logout,
    markPasswordChanged,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (ctx === null) {
    throw new Error("useAuth precisa ser chamado dentro de <AuthProvider>");
  }
  return ctx;
}
