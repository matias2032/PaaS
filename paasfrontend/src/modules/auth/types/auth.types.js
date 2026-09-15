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

export {};/**
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

/**
 * Espelha UpdateProfileRequestDTO do backend (auth/dto/UpdateProfileRequestDTO.java).
 *
 * @typedef {Object} UpdateProfileRequest
 * @property {string} firstName
 * @property {string} [lastName]
 * @property {string} [phone]
 */

/**
 * Espelha ChangePasswordRequestDTO do backend (auth/dto/ChangePasswordRequestDTO.java).
 *
 * @typedef {Object} ChangePasswordRequest
 * @property {string} currentPassword
 * @property {string} newPassword
 */

/**
 * Espelha ForgotPasswordRequestDTO do backend (auth/dto/ForgotPasswordRequestDTO.java).
 *
 * @typedef {Object} ForgotPasswordRequest
 * @property {string} email
 */

/**
 * Espelha ResetPasswordRequestDTO do backend (auth/dto/ResetPasswordRequestDTO.java).
 *
 * @typedef {Object} ResetPasswordRequest
 * @property {string} token
 * @property {string} newPassword
 */

/**
 * Espelha MessageResponseDTO do backend (auth/dto/MessageResponseDTO.java).
 * Devolvido por forgotPassword e resetPassword.
 *
 * @typedef {Object} MessageResponse
 * @property {string} message
 */

export {};