import httpClient from '../../../lib/api/httpClient';

/**
 * Mirrors BillingController from paasbackend
 * (billing/controller/BillingController.java). Each function
 * corresponds 1:1 to an endpoint.
 *
 * The controller has no class-level @RequestMapping because it unites
 * two resources with different roots: /api/plans (catalog) and
 * /api/organizations/{orgPublicUuid}/subscription* (per organization).
 * This file mirrors that split below with a comment divider, same as
 * the controller does.
 *
 * NOTE: /api/plans/** endpoints are only behind normal authentication
 * right now — no admin/platform_role check exists yet backend-side
 * (see handoff §3). Nothing here restricts who can call
 * createPlan/updatePlan/etc.; that gate will need to be added once
 * platform_role exists, both here and in the UI that calls these
 * functions.
 */

// ---- Plans ----

/**
 * POST /api/plans
 * @param {import('../types/billing.types').PlanRequest} data
 * @returns {Promise<import('../types/billing.types').PlanResponse>}
 */
export async function createPlan(data) {
  const response = await httpClient.post('/plans', data);
  return response.data;
}

/**
 * GET /api/plans
 * Client-facing pricing listing — only ACTIVE plans (backend filters
 * server-side; INACTIVE/ARCHIVED plans never appear here, see
 * BillingService.listActivePlans).
 * @returns {Promise<import('../types/billing.types').PlanResponse[]>}
 */
export async function listActivePlans() {
  const response = await httpClient.get('/plans');
  return response.data;
}

/**
 * GET /api/plans/{publicUuid}
 * Unlike listActivePlans, this returns a plan regardless of status —
 * needed e.g. to resolve a plan referenced by an old/cancelled
 * subscription even if it's since gone INACTIVE or ARCHIVED.
 * @param {string} publicUuid
 * @returns {Promise<import('../types/billing.types').PlanResponse>}
 */
export async function getPlan(publicUuid) {
  const response = await httpClient.get(`/plans/${publicUuid}`);
  return response.data;
}

/**
 * PUT /api/plans/{publicUuid}
 * Updates name/description only. Slug is intentionally not
 * updatable via this call (mirrors OrganizationRequest.slug).
 * @param {string} publicUuid
 * @param {import('../types/billing.types').PlanRequest} data
 * @returns {Promise<import('../types/billing.types').PlanResponse>}
 */
export async function updatePlan(publicUuid, data) {
  const response = await httpClient.put(`/plans/${publicUuid}`, data);
  return response.data;
}

/**
 * DELETE /api/plans/{publicUuid}
 * Soft-disable: sets status to "INACTIVE" server-side. Reversible via
 * reactivatePlan. Not terminal (contrast with archivePlan).
 * @param {string} publicUuid
 * @returns {Promise<import('../types/billing.types').PlanResponse>}
 */
export async function deactivatePlan(publicUuid) {
  const response = await httpClient.delete(`/plans/${publicUuid}`);
  return response.data;
}

/**
 * POST /api/plans/{publicUuid}/reactivate
 * Reverses deactivatePlan: INACTIVE -> ACTIVE. Mirrors
 * reactivateOrganization's pattern.
 * @param {string} publicUuid
 * @returns {Promise<import('../types/billing.types').PlanResponse>}
 */
export async function reactivatePlan(publicUuid) {
  const response = await httpClient.post(`/plans/${publicUuid}/reactivate`);
  return response.data;
}

/**
 * POST /api/plans/{publicUuid}/archive
 * Terminal — unlike deactivate/reactivate, there is no "un-archive"
 * endpoint. Once ARCHIVED, always ARCHIVED.
 * @param {string} publicUuid
 * @returns {Promise<import('../types/billing.types').PlanResponse>}
 */
export async function archivePlan(publicUuid) {
  const response = await httpClient.post(`/plans/${publicUuid}/archive`);
  return response.data;
}

/**
 * PUT /api/plans/{publicUuid}/resource-limits
 * Full replace of the plan's resource limits (create-or-update in one
 * call — backend upserts, see BillingService.setResourceLimits).
 * @param {string} publicUuid
 * @param {import('../types/billing.types').PlanResourceLimitRequest} data
 * @returns {Promise<import('../types/billing.types').PlanResourceLimitResponse>}
 */
export async function setResourceLimits(publicUuid, data) {
  const response = await httpClient.put(`/plans/${publicUuid}/resource-limits`, data);
  return response.data;
}

/**
 * POST /api/plans/{publicUuid}/prices
 * Adds a new price for a billing cycle. The backend closes (sets
 * effectiveUntil) the current price of the same cycle, if any — never
 * mutates a price already in force. Returns only the newly created
 * price.
 * @param {string} publicUuid
 * @param {import('../types/billing.types').PlanPriceRequest} data
 * @returns {Promise<import('../types/billing.types').PlanPriceResponse>}
 */
export async function addPrice(publicUuid, data) {
  const response = await httpClient.post(`/plans/${publicUuid}/prices`, data);
  return response.data;
}

// ---- Subscriptions ----

/**
 * POST /api/organizations/{orgPublicUuid}/subscription
 * Requires OWNER role in the organization. Rejected if the
 * organization already has a live subscription (PENDING/ACTIVE/
 * PAST_DUE/SUSPENDED) — see BillingService.LIVE_SUBSCRIPTION_STATUSES.
 * @param {string} orgPublicUuid
 * @param {import('../types/billing.types').SubscriptionRequest} data
 * @returns {Promise<import('../types/billing.types').SubscriptionResponse>}
 */
export async function subscribe(orgPublicUuid, data) {
  const response = await httpClient.post(`/organizations/${orgPublicUuid}/subscription`, data);
  return response.data;
}

/**
 * GET /api/organizations/{orgPublicUuid}/subscription
 * Requires the current user to be a member of the organization (any
 * role — read, not write). Returns the current live subscription;
 * 404s if none exists.
 * @param {string} orgPublicUuid
 * @returns {Promise<import('../types/billing.types').SubscriptionResponse>}
 */
export async function getCurrentSubscription(orgPublicUuid) {
  const response = await httpClient.get(`/organizations/${orgPublicUuid}/subscription`);
  return response.data;
}

/**
 * GET /api/organizations/{orgPublicUuid}/subscriptions
 * Requires membership (any role). Full history, not just live ones —
 * includes CANCELLED entries too.
 * @param {string} orgPublicUuid
 * @returns {Promise<import('../types/billing.types').SubscriptionResponse[]>}
 */
export async function listSubscriptionHistory(orgPublicUuid) {
  const response = await httpClient.get(`/organizations/${orgPublicUuid}/subscriptions`);
  return response.data;
}

/**
 * DELETE /api/organizations/{orgPublicUuid}/subscription
 * Requires OWNER role. Cancels the current live subscription
 * (status -> "CANCELLED", autoRenew -> false, cancelledAt set).
 * 404s if there is no live subscription to cancel.
 * @param {string} orgPublicUuid
 * @returns {Promise<import('../types/billing.types').SubscriptionResponse>}
 */
export async function cancelSubscription(orgPublicUuid) {
  const response = await httpClient.delete(`/organizations/${orgPublicUuid}/subscription`);
  return response.data;
}

/**
 * PUT /api/organizations/{orgPublicUuid}/subscription
 * Requires OWNER role. Replaces whatever live subscription exists
 * (cancelling it) with a subscription to the given plan price, in one
 * atomic backend transaction — this is the operation to use when
 * switching plans, instead of calling cancelSubscription() followed by
 * subscribe() as two separate requests (that sequence has a real
 * failure window: if the second call fails after the first succeeds,
 * the organization is left with no subscription at all).
 * @param {string} orgPublicUuid
 * @param {import('../types/billing.types').SubscriptionRequest} data
 * @returns {Promise<import('../types/billing.types').SubscriptionResponse>}
 */
export async function switchSubscription(orgPublicUuid, data) {
  const response = await httpClient.put(`/organizations/${orgPublicUuid}/subscription`, data);
  return response.data;
}