/**
 * Mirrors payment/dto/*.java from paasbackend (client-facing DTOs only).
 * No TypeScript — JSDoc typedefs only, for editor autocomplete and
 * as documentation of the exact JSON shape exchanged with the backend.
 *
 * Not mirrored on purpose: AdminMarkPaidRequestDTO / AdminRefundRequestDTO
 * (admin app only) and CreateInvoiceRequestDTO / InvoiceItemRequestDTO
 * (dead code backend-side, no endpoint uses them yet).
 */

/**
 * Request body for POST /api/invoices/{publicUuid}/payments. The amount
 * is deliberately absent: it is always the invoice total, computed
 * server-side, so a client can never underpay.
 * @typedef {Object} SubmitPaymentRequest
 * @property {string} paymentMethodCode - required, max 30 chars
 *   (a `code` from PaymentMethodResponse, e.g. "MPESA")
 * @property {string} [transactionReference] - optional, max 255 chars
 */

/**
 * Response body for GET /api/payment-methods. Only ACTIVE methods are
 * ever returned by that endpoint.
 * @typedef {Object} PaymentMethodResponse
 * @property {string} code
 * @property {string} name
 * @property {string} status
 */

/**
 * One line of an invoice. lineTotal is computed by the backend
 * (quantity * unitPrice) so the client never redoes the arithmetic.
 * @typedef {Object} InvoiceItemResponse
 * @property {string} description
 * @property {number} quantity
 * @property {number} unitPrice
 * @property {number} lineTotal
 */

/**
 * Response body for a payment (submit, and embedded in
 * InvoiceResponse.payments, newest first).
 * @typedef {Object} PaymentResponse
 * @property {string} publicUuid - UUID
 * @property {string} invoicePublicUuid - UUID
 * @property {string} paymentMethodCode
 * @property {number} amount
 * @property {string} currency
 * @property {string|null} transactionReference
 * @property {string|null} externalPaymentId - always null until a real
 *   gateway is wired in backend-side. Not shown in the UI for now.
 * @property {string} status - "PENDING" | "PROCESSING" | "PAID" |
 *   "FAILED" | "CANCELLED" | "REFUNDED". PENDING and PROCESSING count
 *   as "in flight" (submitted, not yet settled or failed).
 * @property {string|null} failureReason - only present when status is FAILED
 * @property {string|null} paidAt - ISO 8601 (OffsetDateTime)
 * @property {string} createdAt - ISO 8601 (OffsetDateTime)
 */

/**
 * Response body for invoice endpoints (list and get). Items and
 * payments come embedded. NOTE: it carries subscriptionPublicUuid but
 * NOT the organization uuid — which is why the invoice detail route is
 * nested under /organizations/:orgPublicUuid (see PaymentRoutes.jsx).
 * @typedef {Object} InvoiceResponse
 * @property {string} publicUuid - UUID
 * @property {string} subscriptionPublicUuid - UUID
 * @property {string} invoiceNumber - "INV-{year}-{6 digits}"
 * @property {string} currency - ISO 4217, 3 letters
 * @property {string} status - "DRAFT" | "PENDING" | "PAID" | "OVERDUE" |
 *   "CANCELLED" | "VOID". Only PENDING and OVERDUE can receive a payment.
 * @property {number} totalAmount - sum of items' lineTotal
 * @property {string} issuedAt - ISO 8601 (OffsetDateTime)
 * @property {string} dueAt - ISO 8601 (OffsetDateTime)
 * @property {string|null} paidAt - ISO 8601 (OffsetDateTime)
 * @property {string} createdAt - ISO 8601 (OffsetDateTime)
 * @property {InvoiceItemResponse[]} items
 * @property {PaymentResponse[]} payments
 */

export {};