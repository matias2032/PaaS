/**
 * Mirrors service/dto/*.java from paasbackend.
 * No TypeScript — JSDoc typedefs only, for editor autocomplete and
 * as documentation of the exact JSON shape exchanged with the backend.
 *
 * Service has 7 related sub-resources (unlike Project's 1), each with
 * its own typedef pair below, grouped in the same order as handoff
 * section 3.
 */

// ---- ServiceType (catalog, read-only) ----

/**
 * Response body for GET /api/service-types. Read-only catalog, no
 * request DTO — seeded server-side: WEB_SERVICE, STATIC_SITE, WORKER,
 * DATABASE, CRON_JOB. Same shape/reasoning as GitProviderResponse in
 * project.types.js.
 * @typedef {Object} ServiceTypeResponse
 * @property {number} idServiceType
 * @property {string} code - e.g. "WEB_SERVICE" | "STATIC_SITE" | "WORKER" | "DATABASE" | "CRON_JOB"
 * @property {string} name
 */

// ---- Service (main resource) ----

/**
 * Request body for POST /api/projects/{projectPublicUuid}/services and
 * PATCH /api/services/{publicUuid}. Same shape for create and update
 * (mirrors ProjectRequest) — serviceTypeCode is only read on create,
 * slug is accepted but ignored server-side on update.
 * @typedef {Object} ServiceRequest
 * @property {string} name - max 150 chars
 * @property {string} slug - max 150 chars, ignored on update
 * @property {string} serviceTypeCode - must match a ServiceTypeResponse.code, only used on create
 * @property {boolean} [autoDeploy]
 */

/**
 * Response body for service endpoints (create, get, update, list,
 * archive, reactivate).
 *
 * organizationPublicUuid is included here by backend decision (Option
 * B from the SERVICE frontend handoff) so pages can compute
 * isOwner/canManage in one hop, same as ProjectResponse already does
 * — unlike the original ServiceResponseDTO which only carried
 * projectPublicUuid.
 * @typedef {Object} ServiceResponse
 * @property {string} publicUuid - UUID
 * @property {string} projectPublicUuid - UUID
 * @property {string} organizationPublicUuid - UUID
 * @property {string} serviceTypeCode
 * @property {string} serviceTypeName
 * @property {string} name
 * @property {string} slug
 * @property {string} status - "CREATED" | "PROVISIONING" | "RUNNING" | "STOPPED" | "FAILED" | "SUSPENDED" | "ARCHIVED".
 *   Only CREATED and ARCHIVED are reachable via the API for now — the
 *   other 5 are reserved for when INFRASTRUCTURE exists; don't build
 *   UI for them yet, but don't assume only 2 values either.
 * @property {boolean} autoDeploy
 * @property {string} createdAt - ISO 8601 (OffsetDateTime)
 * @property {string} updatedAt - ISO 8601 (OffsetDateTime)
 */

// ---- ServiceRepositoryEntity (1:1, upsert) ----

/**
 * Request body for PUT /api/services/{servicePublicUuid}/repository.
 * gitConnectionPublicUuid reuses an existing GitConnection from the
 * project module — the UI should let the user pick one from the same
 * listing GitConnectionsPage already uses, not type it freehand.
 * @typedef {Object} ServiceRepositoryRequest
 * @property {string} [gitConnectionPublicUuid] - UUID, optional (nullable server-side)
 * @property {string} repositoryUrl - max 1000 chars
 * @property {string} [repositoryOwner]
 * @property {string} [repositoryName]
 * @property {string} [branch] - default "main"
 */

/**
 * Response body for repository get/upsert. GET returns 404 if never
 * set (no repository configured yet for this service).
 * @typedef {Object} ServiceRepositoryResponse
 * @property {string} [gitConnectionPublicUuid] - UUID, optional
 * @property {string} repositoryUrl
 * @property {string} [repositoryOwner]
 * @property {string} [repositoryName]
 * @property {string} branch
 * @property {string} createdAt - ISO 8601 (OffsetDateTime)
 * @property {string} updatedAt - ISO 8601 (OffsetDateTime)
 */

// ---- ServiceBuildConfig (1:1, upsert) ----

/**
 * Request body for PUT /api/services/{servicePublicUuid}/build-config.
 * @typedef {Object} ServiceBuildConfigRequest
 * @property {string} [rootDirectory] - default "/"
 * @property {string} [buildCommand]
 * @property {string} [startCommand]
 * @property {string} [dockerfilePath]
 * @property {string} [healthCheckPath]
 * @property {number} [port] - 1–65535
 */

/**
 * Response body for build-config get/upsert. GET returns 404 if never
 * set.
 * @typedef {Object} ServiceBuildConfigResponse
 * @property {string} rootDirectory
 * @property {string} [buildCommand]
 * @property {string} [startCommand]
 * @property {string} [dockerfilePath]
 * @property {string} [healthCheckPath]
 * @property {number} [port]
 * @property {string} createdAt - ISO 8601 (OffsetDateTime)
 * @property {string} updatedAt - ISO 8601 (OffsetDateTime)
 */

// ---- ServiceResourceConfig (1:1, upsert) ----

/**
 * Request body for PUT /api/services/{servicePublicUuid}/resource-config.
 * All fields required server-side (@NotNull) — unlike build-config,
 * there's no "partially configured" state to render for this one.
 *
 * Not yet validated against PlanResourceLimit (BILLING) — the backend
 * accepts any positive value for now. Don't build UI that implies
 * "within your plan", it would be misleading.
 * @typedef {Object} ServiceResourceConfigRequest
 * @property {number} cpuLimit - BigDecimal server-side, must be > 0
 * @property {number} memoryLimitMb - must be > 0
 * @property {number} storageLimitMb - must be > 0
 */

/**
 * Response body for resource-config get/upsert. GET returns 404 if
 * never set.
 * @typedef {Object} ServiceResourceConfigResponse
 * @property {number} cpuLimit
 * @property {number} memoryLimitMb
 * @property {number} storageLimitMb
 * @property {string} createdAt - ISO 8601 (OffsetDateTime)
 * @property {string} updatedAt - ISO 8601 (OffsetDateTime)
 */

// ---- EnvironmentVariable (N, upsert by key) ----

/**
 * Request body for PUT .../environment-variables. `value` is
 * plaintext over the wire but never stored or returned as-is server
 * side — see EnvironmentVariableResponse below.
 * @typedef {Object} EnvironmentVariableRequest
 * @property {string} variableKey - max 255 chars
 * @property {string} value - plaintext in the request only, never echoed back
 * @property {boolean} [isSecret] - default true
 */

/**
 * Response body for environment-variable list/upsert.
 *
 * Deliberately has NO `value` field — the backend never returns the
 * decrypted value to anyone. Treat this like a password field: a
 * form editing an existing variable must always start blank and
 * write a brand-new value, never pre-fill from a previous fetch.
 * @typedef {Object} EnvironmentVariableResponse
 * @property {string} variableKey
 * @property {boolean} isSecret
 * @property {string} createdAt - ISO 8601 (OffsetDateTime)
 * @property {string} updatedAt - ISO 8601 (OffsetDateTime)
 */

// ---- Domain (N) ----

/**
 * Request body for POST /api/services/{servicePublicUuid}/domains.
 * @typedef {Object} DomainRequest
 * @property {string} hostname - max 255 chars, globally unique
 * @property {boolean} [isPrimary]
 */

/**
 * Response body for domain create/list. Both verificationStatus and
 * sslStatus stay PENDING for now (no real Coolify issuing certs yet)
 * — show this as a stable state, not as "processing".
 * @typedef {Object} DomainResponse
 * @property {string} publicUuid - UUID
 * @property {string} hostname
 * @property {boolean} isPrimary - only one domain per service can be true; backend unsets the previous one automatically
 * @property {string} verificationStatus - "PENDING" | "VERIFIED" | "FAILED"
 * @property {string} sslStatus - "PENDING" | "ISSUING" | "ACTIVE" | "FAILED" | "EXPIRED"
 * @property {string|null} verifiedAt - ISO 8601 (OffsetDateTime), null until VERIFIED
 * @property {string} createdAt - ISO 8601 (OffsetDateTime)
 * @property {string} updatedAt - ISO 8601 (OffsetDateTime)
 */

// ---- Deployment (N, manual history record) ----

/**
 * Request body for POST /api/services/{servicePublicUuid}/deployments.
 * This logs a deployment record — it does not trigger a real
 * deployment (no Coolify yet). UI copy should say "Log a deployment",
 * not "Deploy now".
 * @typedef {Object} DeploymentRequest
 * @property {string} triggerType - "MANUAL" | "GIT_PUSH" | "WEBHOOK" | "API" | "SYSTEM"
 * @property {string} [commitHash]
 * @property {string} [commitMessage]
 * @property {string} [branch]
 */

/**
 * Response body for deployment create/list/get. status is always
 * QUEUED on creation and never advances on its own — there's nothing
 * real running it yet.
 * @typedef {Object} DeploymentResponse
 * @property {string} publicUuid - UUID
 * @property {string|null} coolifyDeploymentUuid - null until real Coolify integration exists
 * @property {string} [commitHash]
 * @property {string} [commitMessage]
 * @property {string} [branch]
 * @property {string} triggerType
 * @property {string} status - always "QUEUED" for now
 * @property {string|null} startedAt - ISO 8601 (OffsetDateTime), null until real execution exists
 * @property {string|null} finishedAt - ISO 8601 (OffsetDateTime), null until real execution exists
 * @property {string} createdAt - ISO 8601 (OffsetDateTime)
 */

export {};