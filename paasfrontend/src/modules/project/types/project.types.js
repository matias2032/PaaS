/**
 * Mirrors project/dto/*.java from paasbackend.
 * No TypeScript — JSDoc typedefs only, for editor autocomplete and
 * as documentation of the exact JSON shape exchanged with the backend.
 */

/**
 * Request body for POST /api/organizations/{orgPublicUuid}/projects
 * and PATCH /api/projects/{publicUuid}. Same shape for create and
 * update (see ProjectRequestDTO.java) — slug is accepted on create
 * but intentionally ignored server-side on update
 * (ProjectService.updateProject never touches it).
 * @typedef {Object} ProjectRequest
 * @property {string} name - max 150 chars
 * @property {string} slug - max 150 chars, required
 * @property {string} [description]
 */

/**
 * Response body for project endpoints (create, get, update, list,
 * archive, reactivate).
 * @typedef {Object} ProjectResponse
 * @property {string} publicUuid - UUID
 * @property {string} organizationPublicUuid - UUID
 * @property {string} name
 * @property {string} slug
 * @property {string} description
 * @property {string} status - "ACTIVE" | "ARCHIVED".
 *   ARCHIVED via archiveProject, reversible via reactivateProject
 *   (unlike Plan.status, there's no third terminal state here).
 * @property {string} createdAt - ISO 8601 (OffsetDateTime)
 * @property {string} updatedAt - ISO 8601 (OffsetDateTime)
 */

/**
 * Response body for GET /api/git-providers. Read-only catalog, no
 * request DTO — the set of providers is fixed server-side (seeded:
 * GITHUB, GITLAB, BITBUCKET), not something a client creates.
 * @typedef {Object} GitProviderResponse
 * @property {number} idGitProvider
 * @property {string} code - e.g. "GITHUB" | "GITLAB" | "BITBUCKET"
 * @property {string} name
 */

/**
 * Request body for POST /api/organizations/{orgPublicUuid}/git-connections.
 * externalAccountId/externalAccountName are typed by the user manually
 * for now — there is no real OAuth flow yet (see handoff §note on
 * createGitConnection). The UI must make this clear rather than imply
 * a "Login with GitHub"-style button.
 * @typedef {Object} GitConnectionRequest
 * @property {string} gitProviderCode - must match a GitProviderResponse.code
 * @property {string} [externalAccountId]
 * @property {string} [externalAccountName]
 */

/**
 * Response body for git-connection endpoints (create, list, revoke).
 * @typedef {Object} GitConnectionResponse
 * @property {string} publicUuid - UUID
 * @property {string} organizationPublicUuid - UUID
 * @property {string} gitProviderCode
 * @property {string} externalAccountId
 * @property {string} externalAccountName
 * @property {string} status - "ACTIVE" | "EXPIRED" | "REVOKED" | "INACTIVE"
 * @property {string|null} tokenExpiresAt - ISO 8601 (OffsetDateTime), null for now
 *   since token fields aren't actually populated without real OAuth
 * @property {string} createdAt - ISO 8601 (OffsetDateTime)
 * @property {string} updatedAt - ISO 8601 (OffsetDateTime)
 */

export {};