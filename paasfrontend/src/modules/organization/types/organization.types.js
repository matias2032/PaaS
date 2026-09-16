/**
 * Mirrors organization/dto/*.java from paasbackend.
 * No TypeScript — JSDoc typedefs only, for editor autocomplete and
 * as documentation of the exact JSON shape exchanged with the backend.
 */

/**
 * Request body for POST /api/organizations and PUT /api/organizations/{publicUuid}.
 * Same shape for create and update (see OrganizationRequestDTO.java).
 * @typedef {Object} OrganizationRequest
 * @property {string} name - max 150 chars
 * @property {string} slug - max 150 chars
 */

/**
 * Response body for organization endpoints (create, get, update, list).
 * @typedef {Object} OrganizationResponse
 * @property {string} publicUuid - UUID
 * @property {string} name
 * @property {string} slug
 * @property {string} status - "ACTIVE" | "SUSPENDED" | "INACTIVE" (INACTIVE = soft-deleted, set via deactivateOrganization; no reactivation flow yet)
 * @property {string} createdAt - ISO 8601 (OffsetDateTime)
 * @property {string} updatedAt - ISO 8601 (OffsetDateTime)
 */

/**
 * Request body for POST /api/organizations/{publicUuid}/members (add
 * member — both fields required) and for PATCH
 * /api/organizations/{publicUuid}/members/{userPublicUuid} (change role
 * — only roleCode is read by the backend; userEmail is ignored/unused
 * for that call, safe to omit). Both endpoints are OWNER-only.
 * @typedef {Object} OrganizationMemberRequest
 * @property {string} [userEmail] - required for add, ignored for role change
 * @property {string} roleCode
 */

/**
 * Response body for member endpoints (add member, list members).
 * @typedef {Object} OrganizationMemberResponse
 * @property {string} userPublicUuid - UUID
 * @property {string} userFirstName
 * @property {string} userLastName
 * @property {string} userEmail
 * @property {string} roleCode
 * @property {string} roleName
 * @property {string} joinedAt - ISO 8601 (OffsetDateTime)
 */

/**
 * Response body for GET /api/organizations/roles.
 * @typedef {Object} OrganizationRole
 * @property {number} idOrganizationRole
 * @property {string} code
 * @property {string} name
 */

export {};