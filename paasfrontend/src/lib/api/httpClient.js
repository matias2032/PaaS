import axios from 'axios';
import { getAccessToken, clearAccessToken } from './tokenStore';

const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

httpClient.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const requestUrl = error.config?.url?.toLowerCase() || '';

      // Isenta requisições de login, registo e alteração de password
      // de dispararem o logout automático por 401.
      const isCredentialCheck = 
        requestUrl.includes('change-password') || 
        requestUrl.includes('password') || 
        requestUrl.includes('login');

      if (!isCredentialCheck) {
        // Token de sessão realmente expirado/inválido
        clearAccessToken();
        window.dispatchEvent(new Event('auth:unauthorized'));
      }
    }
    return Promise.reject(error);
  }
);

export default httpClient;