import axios from 'axios';
import { getAccessToken, clearAccessToken } from './tokenStore';

// Sem refresh token por agora — access token dura 24h (definido no backend,
// jwt.expiration). Quando o refresh token for adicionado, o interceptor
// de resposta abaixo ganha a lógica de retry com /auth/refresh.

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
      // Token inválido/expirado. Sem refresh ainda, então limpamos
      // e deixamos o AuthContext (próxima etapa) decidir o redirect.
      clearAccessToken();
    }
    return Promise.reject(error);
  }
);

export default httpClient;