/**
 * Mirrors api_key/dto/*.java from paasbackend. JSDoc typedefs only.
 */

/**
 * Request body for POST /api/organizations/{orgUuid}/api-keys (OWNER only).
 * @typedef {Object} ApiKeyRequest
 * @property {string} name - required, max 120 chars, unique per organization
 * @property {string} [expiresAt] - ISO 8601 (OffsetDateTime); omit = never expires
 */

/**
 * Response of list/revoke. Never contains rawKey or keyHash.
 * @typedef {Object} ApiKey
 * @property {string} publicUuid
 * @property {string} organizationPublicUuid
 * @property {string} name
 * @property {string} keyPrefix - "sk_live_" + first 8 chars of the secret
 * @property {string} status - "ACTIVE" | "REVOKED". Expiry does NOT change
 *   status: derive "expired" from expiresAt (see getEffectiveStatus).
 * @property {string|null} lastUsedAt - ISO 8601
 * @property {string|null} expiresAt - ISO 8601
 * @property {string} createdAt - ISO 8601
 * @property {string|null} revocationReason - only set when revoked by a platform admin
 */

/**
 * Response of POST only. The ONLY response in the whole API that carries
 * rawKey. It can never be retrieved again.
 * @typedef {Object} ApiKeyCreated
 * @property {string} publicUuid
 * @property {string} name
 * @property {string} rawKey
 * @property {string} keyPrefix
 * @property {string} status
 * @property {string|null} expiresAt
 * @property {string} createdAt
 */

export {};