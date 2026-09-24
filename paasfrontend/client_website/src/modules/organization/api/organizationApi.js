import httpClient from '../../../lib/api/httpClient';

/**
 * Mirrors OrganizationController from paasbackend
 * (organization/controller/OrganizationController.java).
 * Each function corresponds 1:1 to an endpoint.
 */

/**
 * POST /api/organizations
 * Creates a new organization. The authenticated user becomes its
 * first member (role assigned by the backend — likely OWNER, to be
 * confirmed by OrganizationService).
 * @param {import('../types/organization.types').OrganizationRequest} data
 * @returns {Promise<import('../types/organization.types').OrganizationResponse>}
 */
export async function createOrganization(data) {
  const response = await httpClient.post('/organizations', data);
  return response.data;
}

/**
 * GET /api/organizations/{publicUuid}
 * Requires the current user to be a member of the organization.
 * @param {string} publicUuid
 * @returns {Promise<import('../types/organization.types').OrganizationResponse>}
 */
export async function getOrganization(publicUuid) {
  const response = await httpClient.get(`/organizations/${publicUuid}`);
  return response.data;
}

/**
 * PUT /api/organizations/{publicUuid}
 * Requires OWNER/ADMIN role in the organization.
 * @param {string} publicUuid
 * @param {import('../types/organization.types').OrganizationRequest} data
 * @returns {Promise<import('../types/organization.types').OrganizationResponse>}
 */
export async function updateOrganization(publicUuid, data) {
  const response = await httpClient.put(`/organizations/${publicUuid}`, data);
  return response.data;
}

/**
 * DELETE /api/organizations/{publicUuid}
 * Soft-delete: sets status to "INACTIVE" server-side. No hard delete.
 * Requires OWNER role only. Rejected with 409 if already INACTIVE.
 * @param {string} publicUuid
 * @returns {Promise<import('../types/organization.types').OrganizationResponse>}
 */
export async function deactivateOrganization(publicUuid) {
  const response = await httpClient.delete(`/organizations/${publicUuid}`);
  return response.data;
}

/**
 * POST /api/organizations/{publicUuid}/reactivate
 * Reverses deactivateOrganization: INACTIVE -> ACTIVE only. Requires
 * OWNER role only. Rejected with 409 if the organization is not
 * currently INACTIVE (e.g. already ACTIVE, or SUSPENDED — reactivation
 * does not touch SUSPENDED, see OrganizationResponse.status).
 * @param {string} publicUuid
 * @returns {Promise<import('../types/organization.types').OrganizationResponse>}
 */
export async function reactivateOrganization(publicUuid) {
  const response = await httpClient.post(`/organizations/${publicUuid}/reactivate`);
  return response.data;
}

/**
 * GET /api/organizations
 * Lists organizations the current authenticated user belongs to.
 * @returns {Promise<import('../types/organization.types').OrganizationResponse[]>}
 */
export async function listMyOrganizations() {
  const response = await httpClient.get('/organizations');
  return response.data;
}

/**
 * GET /api/organizations/roles
 * Returns the 4 seeded roles (OWNER, ADMIN, DEVELOPER, VIEWER).
 * Not organization-scoped — same list for any authenticated user.
 * @returns {Promise<import('../types/organization.types').OrganizationRole[]>}
 */
export async function listRoles() {
  const response = await httpClient.get('/organizations/roles');
  return response.data;
}

/**
 * POST /api/organizations/{publicUuid}/members
 * Requires OWNER role only (not ADMIN — decided 2026, registration is
 * OWNER-exclusive). Identifies the user to add by email.
 * @param {string} publicUuid
 * @param {import('../types/organization.types').OrganizationMemberRequest} data
 * @returns {Promise<import('../types/organization.types').OrganizationMemberResponse>}
 */
export async function addMember(publicUuid, data) {
  const response = await httpClient.post(`/organizations/${publicUuid}/members`, data);
  return response.data;
}

/**
 * DELETE /api/organizations/{publicUuid}/members/{userPublicUuid}
 * Requires OWNER/ADMIN role. No response body (204).
 * @param {string} publicUuid
 * @param {string} userPublicUuid
 * @returns {Promise<void>}
 */
export async function removeMember(publicUuid, userPublicUuid) {
  await httpClient.delete(`/organizations/${publicUuid}/members/${userPublicUuid}`);
}

/**
 * PATCH /api/organizations/{publicUuid}/members/{userPublicUuid}
 * Requires OWNER role only (not ADMIN). Only `roleCode` is read from
 * the body for this operation — `userEmail` is ignored.
 * @param {string} publicUuid
 * @param {string} userPublicUuid
 * @param {{ roleCode: string }} data
 * @returns {Promise<import('../types/organization.types').OrganizationMemberResponse>}
 */
export async function changeMemberRole(publicUuid, userPublicUuid, data) {
  const response = await httpClient.patch(
    `/organizations/${publicUuid}/members/${userPublicUuid}`,
    data
  );
  return response.data;
}

/**
 * GET /api/organizations/{publicUuid}/members
 * Requires the current user to be a member of the organization.
 * @param {string} publicUuid
 * @returns {Promise<import('../types/organization.types').OrganizationMemberResponse[]>}
 */
export async function listMembers(publicUuid) {
  const response = await httpClient.get(`/organizations/${publicUuid}/members`);
  return response.data;
}