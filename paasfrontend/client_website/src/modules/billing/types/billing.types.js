/**
 * Mirrors billing/dto/*.java from paasbackend.
 * No TypeScript — JSDoc typedefs only, for editor autocomplete and
 * as documentation of the exact JSON shape exchanged with the backend.
 */

/**
 * Request body for POST /api/plans and PUT /api/plans/{publicUuid}.
 * Same shape for create and update (see PlanRequestDTO.java). Does not
 * touch resource limits or prices — those have their own DTOs/flows
 * (see PlanResourceLimitRequest and PlanPriceRequest below), mirroring
 * how OrganizationRequest doesn't touch members.
 * @typedef {Object} PlanRequest
 * @property {string} name - max 100 chars
 * @property {string} slug - max 100 chars
 * @property {string} [description]
 */

/**
 * Response body for plan endpoints (create, get, update, list,
 * deactivate, reactivate, archive). Comes back with resourceLimits and
 * prices already embedded — no extra requests needed to assemble a
 * pricing page.
 * @typedef {Object} PlanResponse
 * @property {string} publicUuid - UUID
 * @property {string} name
 * @property {string} slug
 * @property {string} description
 * @property {string} status - "ACTIVE" | "INACTIVE" | "ARCHIVED".
 *   INACTIVE via deactivatePlan, reversible via reactivatePlan.
 *   ARCHIVED via archivePlan — terminal, no "un-archive" endpoint.
 * @property {PlanResourceLimitResponse|null} resourceLimits - null
 *   until setResourceLimits has been called at least once for this plan
 * @property {PlanPriceResponse[]} prices - only CURRENT prices
 *   (effectiveUntil == null), one per billing cycle in force. Closed/
 *   historical prices are never embedded here.
 * @property {string} createdAt - ISO 8601 (OffsetDateTime)
 * @property {string} updatedAt - ISO 8601 (OffsetDateTime)
 */

/**
 * Request body for PUT /api/plans/{publicUuid}/resource-limits.
 * Full replace — every field required, no partial updates.
 * @typedef {Object} PlanResourceLimitRequest
 * @property {number} cpuLimit - decimal, must be > 0
 * @property {number} memoryLimitMb - integer, >= 0
 * @property {number} storageLimitMb - integer, >= 0
 * @property {number} maxProjects - integer, >= 0
 * @property {number} maxServices - integer, >= 0
 * @property {number} maxDomains - integer, >= 0
 * @property {number} [maxEnvironmentVariables] - integer, optional
 * @property {number} [bandwidthLimitMb] - integer, optional
 */

/**
 * Response body embedded in PlanResponse.resourceLimits and returned
 * directly by PUT .../resource-limits.
 * @typedef {Object} PlanResourceLimitResponse
 * @property {number} cpuLimit
 * @property {number} memoryLimitMb
 * @property {number} storageLimitMb
 * @property {number} maxProjects
 * @property {number} maxServices
 * @property {number} maxDomains
 * @property {number} maxEnvironmentVariables
 * @property {number} bandwidthLimitMb
 */

/**
 * Request body for POST /api/plans/{publicUuid}/prices. The backend
 * closes the current price of the same billingCycle (sets
 * effectiveUntil = now) instead of updating it — a PlanPrice in force
 * is never mutated, only superseded.
 * @typedef {Object} PlanPriceRequest
 * @property {string} billingCycle - e.g. "MONTHLY" | "YEARLY"
 *   (backend does not constrain the exact set — treat as a plain string)
 * @property {number} amount - decimal, >= 0
 * @property {string} [currency]
 */

/**
 * Response body for a plan price. When embedded in PlanResponse.prices
 * it always represents a CURRENT price (effectiveUntil == null) —
 * historical/closed prices are not exposed by any endpoint currently
 * listed in the handoff.
 * @typedef {Object} PlanPriceResponse
 * @property {string} publicUuid - UUID. This is what
 *   SubscriptionRequest.planPricePublicUuid references.
 * @property {string} billingCycle
 * @property {number} amount
 * @property {string} currency
 * @property {string} effectiveFrom - ISO 8601 (OffsetDateTime)
 */

/**
 * Request body for POST /api/organizations/{orgPublicUuid}/subscription.
 * The client picks a PlanPrice directly (which already fixes plan +
 * cycle) rather than a Plan + separate billingCycle field — avoids a
 * second field that would need validating against the chosen plan.
 * OWNER-only (see BillingService.requireOwner).
 * @typedef {Object} SubscriptionRequest
 * @property {string} planPricePublicUuid - UUID
 */

/**
 * Response body for subscription endpoints (subscribe, get current,
 * cancel) and each item of listSubscriptionHistory. Plan/price are
 * flattened into the response (no second GET to /plans/{uuid} needed
 * to know what the org actually contracted).
 * @typedef {Object} SubscriptionResponse
 * @property {string} publicUuid - UUID
 * @property {string} organizationPublicUuid - UUID
 * @property {string} planPublicUuid - UUID
 * @property {string} planName
 * @property {string} billingCycle
 * @property {string} status - "PENDING" | "ACTIVE" | "PAST_DUE" |
 *   "SUSPENDED" | "CANCELLED" (the first four are the "live" set the
 *   backend checks against for the single-active-subscription rule —
 *   see BillingService.LIVE_SUBSCRIPTION_STATUSES)
 * @property {string} currentPeriodStart - ISO 8601 (OffsetDateTime)
 * @property {string} currentPeriodEnd - ISO 8601 (OffsetDateTime)
 * @property {boolean} autoRenew
 * @property {string|null} cancelledAt - ISO 8601 (OffsetDateTime), null
 *   unless status is CANCELLED
 * @property {string} createdAt - ISO 8601 (OffsetDateTime)
 * @property {string} updatedAt - ISO 8601 (OffsetDateTime)
 */

export {};