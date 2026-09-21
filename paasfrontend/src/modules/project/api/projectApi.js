import httpClient from '../../../lib/api/httpClient';

/**
 * Mirrors ProjectController from paasbackend
 * (project/controller/ProjectController.java). Each function
 * corresponds 1:1 to an endpoint. Same split-resource shape as
 * BillingController: /api/organizations/{orgPublicUuid}/projects
 * (scoped to an org) plus /api/projects/{publicUuid} (by uuid
 * directly) plus a separate git-providers/git-connections group.
 */

// ---- Projects ----

/**
 * POST /api/organizations/{orgPublicUuid}/projects
 * Requires OWNER role in the organization.
 * @param {string} orgPublicUuid
 * @param {import('../types/project.types').ProjectRequest} data
 * @returns {Promise<import('../types/project.types').ProjectResponse>}
 */
export async function createProject(orgPublicUuid, data) {
  const response = await httpClient.post(`/organizations/${orgPublicUuid}/projects`, data);
  return response.data;
}

/**
 * GET /api/organizations/{orgPublicUuid}/projects
 * Requires membership (any role).
 * @param {string} orgPublicUuid
 * @returns {Promise<import('../types/project.types').ProjectResponse[]>}
 */
export async function listProjects(orgPublicUuid) {
  const response = await httpClient.get(`/organizations/${orgPublicUuid}/projects`);
  return response.data;
}

/**
 * GET /api/projects/{publicUuid}
 * Requires membership (any role) in the project's organization.
 * @param {string} publicUuid
 * @returns {Promise<import('../types/project.types').ProjectResponse>}
 */
export async function getProject(publicUuid) {
  const response = await httpClient.get(`/projects/${publicUuid}`);
  return response.data;
}

/**
 * PATCH /api/projects/{publicUuid}
 * Updates name/description only — slug is not updatable via this
 * call (mirrors PlanRequest/OrganizationRequest's slug immutability).
 * Requires OWNER role.
 * @param {string} publicUuid
 * @param {import('../types/project.types').ProjectRequest} data
 * @returns {Promise<import('../types/project.types').ProjectResponse>}
 */
export async function updateProject(publicUuid, data) {
  const response = await httpClient.patch(`/projects/${publicUuid}`, data);
  return response.data;
}

/**
 * POST /api/projects/{publicUuid}/archive
 * Requires OWNER role. Reversible via reactivateProject.
 * @param {string} publicUuid
 * @returns {Promise<import('../types/project.types').ProjectResponse>}
 */
export async function archiveProject(publicUuid) {
  const response = await httpClient.post(`/projects/${publicUuid}/archive`);
  return response.data;
}

/**
 * POST /api/projects/{publicUuid}/reactivate
 * Requires OWNER role. Reverses archiveProject.
 * @param {string} publicUuid
 * @returns {Promise<import('../types/project.types').ProjectResponse>}
 */
export async function reactivateProject(publicUuid) {
  const response = await httpClient.post(`/projects/${publicUuid}/reactivate`);
  return response.data;
}

// ---- Git providers (read-only catalog) ----

/**
 * GET /api/git-providers
 * Public catalog, no organization scoping — same GITHUB/GITLAB/
 * BITBUCKET list for every organization.
 * @returns {Promise<import('../types/project.types').GitProviderResponse[]>}
 */
export async function listGitProviders() {
  const response = await httpClient.get('/git-providers');
  return response.data;
}

// ---- Git connections ----

/**
 * POST /api/organizations/{orgPublicUuid}/git-connections
 * Requires OWNER role. Rejected if the organization already has a
 * connection to the same provider (see ProjectService — flagged there
 * as an assumption, not yet a confirmed hard rule).
 * @param {string} orgPublicUuid
 * @param {import('../types/project.types').GitConnectionRequest} data
 * @returns {Promise<import('../types/project.types').GitConnectionResponse>}
 */
export async function createGitConnection(orgPublicUuid, data) {
  const response = await httpClient.post(`/organizations/${orgPublicUuid}/git-connections`, data);
  return response.data;
}

/**
 * GET /api/organizations/{orgPublicUuid}/git-connections
 * Requires membership (any role).
 * @param {string} orgPublicUuid
 * @returns {Promise<import('../types/project.types').GitConnectionResponse[]>}
 */
export async function listGitConnections(orgPublicUuid) {
  const response = await httpClient.get(`/organizations/${orgPublicUuid}/git-connections`);
  return response.data;
}

/**
 * POST /api/git-connections/{publicUuid}/revoke
 * Requires OWNER role. Sets status -> "REVOKED".
 * @param {string} publicUuid
 * @returns {Promise<import('../types/project.types').GitConnectionResponse>}
 */
export async function revokeGitConnection(publicUuid) {
  const response = await httpClient.post(`/git-connections/${publicUuid}/revoke`);
  return response.data;
}