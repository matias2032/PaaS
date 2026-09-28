import httpClient from '../../../lib/api/httpClient';

/**
 * Mirrors the client-facing half of ApiKeyController (paasbackend).
 * The admin endpoints (/api/admin/...) are not used by this app.
 */

/**
 * POST /api/organizations/{orgUuid}/api-keys
 * OWNER only. 409 on duplicate name OR when the org is INACTIVE/SUSPENDED
 * (decide by organization.status, not by the message).
 * @param {string} orgUuid
 * @param {import('../types/apiKey.types').ApiKeyRequest} data
 * @returns {Promise<import('../types/apiKey.types').ApiKeyCreated>}
 */
export async function createApiKey(orgUuid, data) {
  const response = await httpClient.post(`/organizations/${orgUuid}/api-keys`, data);
  return response.data;
}

/**
 * GET /api/organizations/{orgUuid}/api-keys
 * Any member. Not blocked by INACTIVE/SUSPENDED.
 * @param {string} orgUuid
 * @returns {Promise<import('../types/apiKey.types').ApiKey[]>}
 */
export async function listApiKeys(orgUuid) {
  const response = await httpClient.get(`/organizations/${orgUuid}/api-keys`);
  return response.data;
}

/**
 * PATCH /api/organizations/{orgUuid}/api-keys/{keyUuid}/revoke
 * OWNER only, NO body (self-revocation has no reason).
 * @param {string} orgUuid
 * @param {string} keyUuid
 * @returns {Promise<import('../types/apiKey.types').ApiKey>}
 */
export async function revokeApiKey(orgUuid, keyUuid) {
  const response = await httpClient.patch(
    `/organizations/${orgUuid}/api-keys/${keyUuid}/revoke`
  );
  return response.data;
}