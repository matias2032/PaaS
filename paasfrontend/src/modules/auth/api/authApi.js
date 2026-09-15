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

/**
 * PATCH /api/auth/me
 * Requires a valid Bearer token (attached automatically by httpClient).
 * @param {import('../types/auth.types').UpdateProfileRequest} data
 * @returns {Promise<import('../types/auth.types').AuthResponse>}
 */
export async function updateProfile(data) {
  const response = await httpClient.patch('/auth/me', data);
  return response.data;
}

/**
 * PUT /api/auth/me/password
 * Requires a valid Bearer token (attached automatically by httpClient).
 * @param {import('../types/auth.types').ChangePasswordRequest} data
 * @returns {Promise<import('../types/auth.types').AuthResponse>}
 */
export async function changePassword(data) {
  const response = await httpClient.put('/auth/me/password', data);
  return response.data;
}

/**
 * POST /api/auth/forgot-password
 * Public endpoint. Always resolves with a generic message regardless
 * of whether the email exists (backend behaviour, avoids account
 * enumeration).
 * @param {import('../types/auth.types').ForgotPasswordRequest} data
 * @returns {Promise<import('../types/auth.types').MessageResponse>}
 */
export async function forgotPassword(data) {
  const response = await httpClient.post('/auth/forgot-password', data);
  return response.data;
}

/**
 * POST /api/auth/reset-password
 * Public endpoint.
 * @param {import('../types/auth.types').ResetPasswordRequest} data
 * @returns {Promise<import('../types/auth.types').MessageResponse>}
 */
export async function resetPassword(data) {
  const response = await httpClient.post('/auth/reset-password', data);
  return response.data;
}