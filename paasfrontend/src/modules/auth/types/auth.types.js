/**
 * Espelha AuthRequestDTO do backend (auth/dto/AuthRequestDTO.java).
 * firstName, lastName e phone só são usados no registo.
 *
 * @typedef {Object} AuthRequest
 * @property {string} email
 * @property {string} password
 * @property {string} [firstName]
 * @property {string} [lastName]
 * @property {string} [phone]
 */

/**
 * Espelha AuthResponseDTO do backend (auth/dto/AuthResponseDTO.java).
 *
 * @typedef {Object} AuthResponse
 * @property {string} publicUuid
 * @property {string} firstName
 * @property {string} [lastName]
 * @property {string} email
 * @property {string} status
 * @property {string|null} emailVerifiedAt
 * @property {string} createdAt
 * @property {string|null} token
 */

export {};