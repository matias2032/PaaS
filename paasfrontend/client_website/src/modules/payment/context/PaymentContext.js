import { createContext } from 'react';

/**
 * Holds: the payment methods catalog (GET /api/payment-methods) and the
 * loading/error state of the "submit payment" action.
 *
 * Invoices are intentionally NOT cached here (same reasoning as
 * BillingProvider not storing subscriptions): they are scoped to one
 * organization at a time, so each page owns its local state for them.
 *
 * `error` is not a single shared field, same as BillingContext: the
 * catalog can fail to load independently of a payment submission
 * failing, and neither should blank out the other's message.
 */
export const PaymentContext = createContext(undefined);