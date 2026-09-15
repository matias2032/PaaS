import { useEffect, useState } from 'react';
import {
  login as loginApi,
  register as registerApi,
  updateProfile as updateProfileApi,
  changePassword as changePasswordApi,
  forgotPassword as forgotPasswordApi,
  resetPassword as resetPasswordApi,
} from '../api/authApi';
import { AuthContext } from './AuthContext';
import { setAccessToken, clearAccessToken } from '../../../lib/api/tokenStore';

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

    // Mantém o tokenStore (em memória, usado pelo httpClient) sincronizado
    // com o estado de auth. Isto também restaura o token em memória após
    // um refresh de página (F5), já que tokenStore começa sempre vazio
    // mas o localStorage pode ter uma sessão válida.
    if (auth?.token) {
      setAccessToken(auth.token);
    } else {
      clearAccessToken();
    }
  }, [auth]);

  // CORREÇÃO ADICIONADA: Escuta o evento global de não autorizado para limpar a sessão
  useEffect(() => {
    function handleUnauthorized() {
      setAuth(null);
    }

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

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

  // updateProfile/changePassword don't return a token (see
  // AuthResponseDTO on the backend — token is only populated on
  // login/register). We merge the fresh user data into the existing
  // session while keeping the current token intact.
  async function updateProfile(data) {
    setIsLoading(true);
    setError(null);
    try {
      const response = await updateProfileApi(data);
      setAuth((prev) => ({ ...response, token: prev?.token ?? null }));
      return response;
    } catch (err) {
      setError(err?.response?.data?.message || 'Profile update failed');
      throw err;
    } finally {
      setIsLoading(false);
    }
  }

  async function changePassword(data) {
    setIsLoading(true);
    setError(null);
    try {
      const response = await changePasswordApi(data);
      setAuth((prev) => ({ ...response, token: prev?.token ?? null }));
      return response;
    } catch (err) {
      setError(err?.response?.data?.message || 'Password change failed');
      throw err;
    } finally {
      setIsLoading(false);
    }
  }

  // Neither forgotPassword nor resetPassword touch the current
  // session: forgotPassword is typically called by someone who isn't
  // logged in, and resetPassword doesn't get a token back from the
  // backend — the user is expected to log in manually afterwards.
  async function forgotPassword(data) {
    setIsLoading(true);
    setError(null);
    try {
      return await forgotPasswordApi(data);
    } catch (err) {
      setError(err?.response?.data?.message || 'Request failed');
      throw err;
    } finally {
      setIsLoading(false);
    }
  }

  async function resetPassword(data) {
    setIsLoading(true);
    setError(null);
    try {
      return await resetPasswordApi(data);
    } catch (err) {
      setError(err?.response?.data?.message || 'Password reset failed');
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
    updateProfile,
    changePassword,
    forgotPassword,
    resetPassword,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}