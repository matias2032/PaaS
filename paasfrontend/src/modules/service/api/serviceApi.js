import httpClient from '../../../lib/api/httpClient';

/**
 * Mirrors ServiceController from paasbackend
 * (service/controller/ServiceController.java). Each function
 * corresponds 1:1 to an endpoint. Same split-resource shape as
 * ProjectController: /api/projects/{projectPublicUuid}/services
 * (scoped to a project) plus /api/services/{publicUuid} (by uuid
 * directly) plus one group per sub-resource
 * (repository/build-config/resource-config/environment-variables/
 * domains/deployments), all keyed off servicePublicUuid, plus a
 * separate read-only service-types catalog group.
 *
 * Authorization (handoff section 3.9, not re-stated per function
 * below): every GET requires membership (any role) in the service's
 * organization; every POST/PUT/PATCH/DELETE requires OWNER. All
 * writes are additionally blocked (409) while the organization is
 * INACTIVE — reads stay allowed.
 */

// ---- Service types (read-only catalog) ----

/**
 * GET /api/service-types
 * Public catalog, no project/org scoping — same WEB_SERVICE/
 * STATIC_SITE/WORKER/DATABASE/CRON_JOB list for everyone. Mirrors
 * listGitProviders().
 * @returns {Promise<import('../types/service.types').ServiceTypeResponse[]>}
 */
export async function listServiceTypes() {
  const response = await httpClient.get('/service-types');
  return response.data;
}

// ---- Services ----

/**
 * POST /api/projects/{projectPublicUuid}/services
 * Requires OWNER role in the project's organization.
 * @param {string} projectPublicUuid
 * @param {import('../types/service.types').ServiceRequest} data
 * @returns {Promise<import('../types/service.types').ServiceResponse>}
 */
export async function createService(projectPublicUuid, data) {
  const response = await httpClient.post(`/projects/${projectPublicUuid}/services`, data);
  return response.data;
}

/**
 * GET /api/projects/{projectPublicUuid}/services
 * Requires membership (any role).
 * @param {string} projectPublicUuid
 * @returns {Promise<import('../types/service.types').ServiceResponse[]>}
 */
export async function listServices(projectPublicUuid) {
  const response = await httpClient.get(`/projects/${projectPublicUuid}/services`);
  return response.data;
}

/**
 * GET /api/services/{publicUuid}
 * Requires membership (any role) in the service's organization.
 * @param {string} publicUuid
 * @returns {Promise<import('../types/service.types').ServiceResponse>}
 */
export async function getService(publicUuid) {
  const response = await httpClient.get(`/services/${publicUuid}`);
  return response.data;
}

/**
 * PATCH /api/services/{publicUuid}
 * Updates name/autoDeploy — serviceTypeCode and slug are not
 * updatable via this call (serviceTypeCode is create-only, slug is
 * immutable like ProjectRequest's).
 * Requires OWNER role.
 * @param {string} publicUuid
 * @param {import('../types/service.types').ServiceRequest} data
 * @returns {Promise<import('../types/service.types').ServiceResponse>}
 */
export async function updateService(publicUuid, data) {
  const response = await httpClient.patch(`/services/${publicUuid}`, data);
  return response.data;
}

/**
 * POST /api/services/{publicUuid}/archive
 * Requires OWNER role. Reversible via reactivateService.
 * @param {string} publicUuid
 * @returns {Promise<import('../types/service.types').ServiceResponse>}
 */
export async function archiveService(publicUuid) {
  const response = await httpClient.post(`/services/${publicUuid}/archive`);
  return response.data;
}

/**
 * POST /api/services/{publicUuid}/reactivate
 * Requires OWNER role. Always returns to CREATED (never RUNNING —
 * there's nothing real running yet without INFRASTRUCTURE).
 * @param {string} publicUuid
 * @returns {Promise<import('../types/service.types').ServiceResponse>}
 */
export async function reactivateService(publicUuid) {
  const response = await httpClient.post(`/services/${publicUuid}/reactivate`);
  return response.data;
}

// ---- Repository (1:1, upsert) ----

/**
 * PUT /api/services/{servicePublicUuid}/repository
 * Requires OWNER role.
 * @param {string} servicePublicUuid
 * @param {import('../types/service.types').ServiceRepositoryRequest} data
 * @returns {Promise<import('../types/service.types').ServiceRepositoryResponse>}
 */
export async function upsertServiceRepository(servicePublicUuid, data) {
  const response = await httpClient.put(`/services/${servicePublicUuid}/repository`, data);
  return response.data;
}

/**
 * GET /api/services/{servicePublicUuid}/repository
 * Requires membership (any role). Returns 404 if never configured —
 * callers should catch and treat as "not set yet", same as
 * build-config/resource-config below.
 * @param {string} servicePublicUuid
 * @returns {Promise<import('../types/service.types').ServiceRepositoryResponse>}
 */
export async function getServiceRepository(servicePublicUuid) {
  const response = await httpClient.get(`/services/${servicePublicUuid}/repository`);
  return response.data;
}

// ---- Build config (1:1, upsert) ----

/**
 * PUT /api/services/{servicePublicUuid}/build-config
 * Requires OWNER role.
 * @param {string} servicePublicUuid
 * @param {import('../types/service.types').ServiceBuildConfigRequest} data
 * @returns {Promise<import('../types/service.types').ServiceBuildConfigResponse>}
 */
export async function upsertServiceBuildConfig(servicePublicUuid, data) {
  const response = await httpClient.put(`/services/${servicePublicUuid}/build-config`, data);
  return response.data;
}

/**
 * GET /api/services/{servicePublicUuid}/build-config
 * Requires membership (any role). Returns 404 if never configured.
 * @param {string} servicePublicUuid
 * @returns {Promise<import('../types/service.types').ServiceBuildConfigResponse>}
 */
export async function getServiceBuildConfig(servicePublicUuid) {
  const response = await httpClient.get(`/services/${servicePublicUuid}/build-config`);
  return response.data;
}

// ---- Resource config (1:1, upsert) ----

/**
 * PUT /api/services/{servicePublicUuid}/resource-config
 * Requires OWNER role. All three fields are required server-side.
 * @param {string} servicePublicUuid
 * @param {import('../types/service.types').ServiceResourceConfigRequest} data
 * @returns {Promise<import('../types/service.types').ServiceResourceConfigResponse>}
 */
export async function upsertServiceResourceConfig(servicePublicUuid, data) {
  const response = await httpClient.put(`/services/${servicePublicUuid}/resource-config`, data);
  return response.data;
}

/**
 * GET /api/services/{servicePublicUuid}/resource-config
 * Requires membership (any role). Returns 404 if never configured.
 * @param {string} servicePublicUuid
 * @returns {Promise<import('../types/service.types').ServiceResourceConfigResponse>}
 */
export async function getServiceResourceConfig(servicePublicUuid) {
  const response = await httpClient.get(`/services/${servicePublicUuid}/resource-config`);
  return response.data;
}

// ---- Environment variables (N, upsert by key) ----

/**
 * PUT /api/services/{servicePublicUuid}/environment-variables
 * Requires OWNER role. Upserts by variableKey — the response never
 * echoes `value` back (see EnvironmentVariableResponse).
 * @param {string} servicePublicUuid
 * @param {import('../types/service.types').EnvironmentVariableRequest} data
 * @returns {Promise<import('../types/service.types').EnvironmentVariableResponse>}
 */
export async function upsertEnvironmentVariable(servicePublicUuid, data) {
  const response = await httpClient.put(`/services/${servicePublicUuid}/environment-variables`, data);
  return response.data;
}

/**
 * GET /api/services/{servicePublicUuid}/environment-variables
 * Requires membership (any role). List never includes `value`.
 * @param {string} servicePublicUuid
 * @returns {Promise<import('../types/service.types').EnvironmentVariableResponse[]>}
 */
export async function listEnvironmentVariables(servicePublicUuid) {
  const response = await httpClient.get(`/services/${servicePublicUuid}/environment-variables`);
  return response.data;
}

/**
 * DELETE /api/services/{servicePublicUuid}/environment-variables/{variableKey}
 * Requires OWNER role. Returns 204, no body.
 * @param {string} servicePublicUuid
 * @param {string} variableKey
 * @returns {Promise<void>}
 */
export async function deleteEnvironmentVariable(servicePublicUuid, variableKey) {
  await httpClient.delete(
    `/services/${servicePublicUuid}/environment-variables/${encodeURIComponent(variableKey)}`
  );
}

// ---- Domains (N) ----

/**
 * POST /api/services/{servicePublicUuid}/domains
 * Requires OWNER role. Setting isPrimary=true unsets the previous
 * primary domain automatically server-side.
 * @param {string} servicePublicUuid
 * @param {import('../types/service.types').DomainRequest} data
 * @returns {Promise<import('../types/service.types').DomainResponse>}
 */
export async function createDomain(servicePublicUuid, data) {
  const response = await httpClient.post(`/services/${servicePublicUuid}/domains`, data);
  return response.data;
}

/**
 * GET /api/services/{servicePublicUuid}/domains
 * Requires membership (any role).
 * @param {string} servicePublicUuid
 * @returns {Promise<import('../types/service.types').DomainResponse[]>}
 */
export async function listDomains(servicePublicUuid) {
  const response = await httpClient.get(`/services/${servicePublicUuid}/domains`);
  return response.data;
}

/**
 * DELETE /api/domains/{domainPublicUuid}
 * Requires OWNER role. Note this is NOT nested under
 * /services/{servicePublicUuid} — the domain's own publicUuid is
 * enough (mirrors revokeGitConnection's shape, not archiveProject's).
 * Returns 204, no body.
 * @param {string} domainPublicUuid
 * @returns {Promise<void>}
 */
export async function deleteDomain(domainPublicUuid) {
  await httpClient.delete(`/domains/${domainPublicUuid}`);
}

// ---- Deployments (N, manual history record) ----

/**
 * POST /api/services/{servicePublicUuid}/deployments
 * Requires OWNER role. Logs a deployment record — does not trigger a
 * real deployment (no Coolify yet). status is always "QUEUED" in the
 * response and never advances.
 * @param {string} servicePublicUuid
 * @param {import('../types/service.types').DeploymentRequest} data
 * @returns {Promise<import('../types/service.types').DeploymentResponse>}
 */
export async function createDeployment(servicePublicUuid, data) {
  const response = await httpClient.post(`/services/${servicePublicUuid}/deployments`, data);
  return response.data;
}

/**
 * GET /api/services/{servicePublicUuid}/deployments
 * Requires membership (any role). Ordered most-recent-first
 * server-side.
 * @param {string} servicePublicUuid
 * @returns {Promise<import('../types/service.types').DeploymentResponse[]>}
 */
export async function listDeployments(servicePublicUuid) {
  const response = await httpClient.get(`/services/${servicePublicUuid}/deployments`);
  return response.data;
}

/**
 * GET /api/deployments/{publicUuid}
 * Requires membership (any role). Not nested under
 * /services/{servicePublicUuid} — same shape as deleteDomain above.
 * @param {string} publicUuid
 * @returns {Promise<import('../types/service.types').DeploymentResponse>}
 */
export async function getDeployment(publicUuid) {
  const response = await httpClient.get(`/deployments/${publicUuid}`);
  return response.data;
}