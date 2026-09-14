import httpClient from '../../../lib/api/httpClient';

/**
 * Espelha AuthController do backend (auth/controller/AuthController.java).
 * Cada função corresponde 1:1 a um endpoint.
 */

/**
 * POST /api/auth/register
 * @param {import('../types/auth.types').AuthRequest} data
 * @returns {Promise<import('../types/auth.types').AuthResponse>}
 */
export async function register(data) {
  const response = await httpClient.post('/auth/register', data);
  return response.data;
}

/**
 * POST /api/auth/login
 * @param {import('../types/auth.types').AuthRequest} data
 * @returns {Promise<import('../types/auth.types').AuthResponse>}
 */
export async function login(data) {
  const response = await httpClient.post('/auth/login', data);
  return response.data;
}

/**
 * GET /api/auth/{publicUuid}
 * @param {string} publicUuid
 * @returns {Promise<import('../types/auth.types').AuthResponse>}
 */
export async function getByPublicUuid(publicUuid) {
  const response = await httpClient.get(`/auth/${publicUuid}`);
  return response.data;
}