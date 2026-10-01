import httpClient from '../../../lib/api/httpClient';

/**
 * Mirrors the CLIENT-FACING half of PaymentController from paasbackend
 * (payment/controller/PaymentController.java). Each function
 * corresponds 1:1 to an endpoint.
 *
 * The admin-facing routes (GET /api/admin/organizations/{uuid}/invoices,
 * PATCH /api/admin/invoices/{uuid}/mark-paid,
 * PATCH /api/admin/payments/{uuid}/refund) are intentionally NOT here:
 * they belong to the platform admin app (Flutter), not to this client.
 *
 * Like billingApi.js, the controller has no class-level resource root:
 * invoices are listed per organization (/organizations/{uuid}/invoices)
 * but read and paid by their own uuid (/invoices/{uuid}...).
 */

// ---- Invoices ----

/**
 * GET /api/organizations/{orgPublicUuid}/invoices
 * Requires membership in the organization (any role). Newest first
 * (sorted server-side by createdAt desc).
 * @param {string} orgPublicUuid
 * @returns {Promise<import('../types/payment.types').InvoiceResponse[]>}
 */
export async function listInvoices(orgPublicUuid) {
  const response = await httpClient.get(`/organizations/${orgPublicUuid}/invoices`);
  return response.data;
}

/**
 * GET /api/invoices/{publicUuid}
 * Requires membership in the invoice's organization (any role).
 * Comes back with items and payments already embedded.
 * @param {string} publicUuid
 * @returns {Promise<import('../types/payment.types').InvoiceResponse>}
 */
export async function getInvoice(publicUuid) {
  const response = await httpClient.get(`/invoices/${publicUuid}`);
  return response.data;
}

// ---- Payments ----

/**
 * POST /api/invoices/{publicUuid}/payments
 * Requires OWNER role in the invoice's organization. The amount is NOT
 * sent: the backend always charges the invoice total. Returns 201 with
 * the new payment (status PENDING until an admin confirms it).
 * Errors worth knowing about: 409 if the invoice is already paid or
 * already has a payment awaiting confirmation.
 * @param {string} invoicePublicUuid
 * @param {import('../types/payment.types').SubmitPaymentRequest} data
 * @returns {Promise<import('../types/payment.types').PaymentResponse>}
 */
export async function submitPayment(invoicePublicUuid, data) {
  const response = await httpClient.post(`/invoices/${invoicePublicUuid}/payments`, data);
  return response.data;
}

// ---- Payment methods (catalog) ----

/**
 * GET /api/payment-methods
 * Any authenticated user. Only ACTIVE methods are returned.
 * @returns {Promise<import('../types/payment.types').PaymentMethodResponse[]>}
 */
export async function listPaymentMethods() {
  const response = await httpClient.get('/payment-methods');
  return response.data;
}