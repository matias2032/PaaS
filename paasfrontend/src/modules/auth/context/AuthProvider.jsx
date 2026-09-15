import { useEffect, useState } from 'react';
import { login as loginApi, register as registerApi } from '../api/authApi';
import { AuthContext } from './AuthContext';

const STORAGE_KEY = 'auth';

function loadStoredAuth() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function persistAuth(auth) {
  if (auth) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
  } else {
    localStorage.removeItem(STORAGE_KEY);
  }
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(loadStoredAuth);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    persistAuth(auth);
  }, [auth]);

  async function login(credentials) {
    setIsLoading(true);
    setError(null);
    try {
      const response = await loginApi(credentials);
      setAuth(response);
      return response;
    } catch (err) {
      setError(err?.response?.data?.message || 'Login failed');
      throw err;
    } finally {
      setIsLoading(false);
    }
  }

  async function register(data) {
    setIsLoading(true);
    setError(null);
    try {
      const response = await registerApi(data);
      setAuth(response);
      return response;
    } catch (err) {
      setError(err?.response?.data?.message || 'Registration failed');
      throw err;
    } finally {
      setIsLoading(false);
    }
  }

  function logout() {
    setAuth(null);
  }

  const value = {
    user: auth,
    token: auth?.token ?? null,
    isAuthenticated: !!auth?.token,
    isLoading,
    error,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}