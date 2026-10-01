/**
 * Small helpers shared by the payment components/pages. Kept inside the
 * module (not in shared/utils) because the rules below mirror
 * PaymentService's constants and only make sense for payments.
 */

// Mirrors PaymentService.PAYABLE_INVOICE_STATUSES: an invoice can only
// receive a payment while it is still owed.
export const PAYABLE_INVOICE_STATUSES = ['PENDING', 'OVERDUE'];

// Mirrors PaymentService.IN_FLIGHT_PAYMENT_STATUSES: submitted but not
// yet settled or failed. The backend rejects a second payment (409)
// while one of these exists.
export const IN_FLIGHT_PAYMENT_STATUSES = ['PENDING', 'PROCESSING'];

/**
 * @param {number} amount
 * @param {string} currency - ISO 4217 code
 * @returns {string}
 */
export function formatMoney(amount, currency) {
  try {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(amount);
  } catch {
    // Unknown/invalid currency code: fall back to a plain rendering
    // instead of crashing the whole page.
    return `${amount} ${currency}`;
  }
}

/**
 * @param {string|null|undefined} iso - ISO 8601 date-time
 * @returns {string}
 */
export function formatDate(iso) {
  return iso ? new Date(iso).toLocaleDateString() : '—';
}

/**
 * @param {import('../types/payment.types').InvoiceResponse} invoice
 * @returns {boolean}
 */
export function isInvoicePayable(invoice) {
  return PAYABLE_INVOICE_STATUSES.includes(invoice.status);
}

/**
 * @param {import('../types/payment.types').InvoiceResponse} invoice
 * @returns {boolean}
 */
export function hasPaymentInFlight(invoice) {
  return invoice.payments.some((payment) => IN_FLIGHT_PAYMENT_STATUSES.includes(payment.status));
}