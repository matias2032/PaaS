import { useCallback, useEffect, useState } from 'react';
import {
  listInvoices as listInvoicesApi,
  getInvoice as getInvoiceApi,
  submitPayment as submitPaymentApi,
  listPaymentMethods as listPaymentMethodsApi,
} from '../api/paymentApi';
import { PaymentContext } from './PaymentContext';

export function PaymentProvider({ children }) {
  // Payment methods catalog (ACTIVE only, filtered server-side).
  const [paymentMethods, setPaymentMethods] = useState([]);
  const [isPaymentMethodsLoading, setIsPaymentMethodsLoading] = useState(false);
  const [paymentMethodsError, setPaymentMethodsError] = useState(null);

  // Covers submitPayment only. Invoice reads are pass-through (see
  // fetchInvoices/fetchInvoice below), so they have no shared state here.
  const [isPaymentSubmitting, setIsPaymentSubmitting] = useState(false);
  const [paymentError, setPaymentError] = useState(null);

  // Same global event the other providers react to (dispatched by
  // httpClient on a real 401) — clears all payment state so a
  // subsequent login never sees stale data from a previous session.
  useEffect(() => {
    function handleUnauthorized() {
      setPaymentMethods([]);
      setPaymentMethodsError(null);
      setPaymentError(null);
    }

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  // ---- Payment methods (catalog) ----

  const refreshPaymentMethods = useCallback(async () => {
    setIsPaymentMethodsLoading(true);
    setPaymentMethodsError(null);
    try {
      const list = await listPaymentMethodsApi();
      setPaymentMethods(list);
      return list;
    } catch (err) {
      setPaymentMethodsError(err?.response?.data?.message || 'Failed to load payment methods');
      throw err;
    } finally {
      setIsPaymentMethodsLoading(false);
    }
  }, []);

  // ---- Invoices (per organization — not cached here, pages own their state) ----

  // Pass-through on purpose: the calling pages already turn a failure
  // into their own load error, so mirroring it in context state would
  // only duplicate the same message.
  async function fetchInvoices(orgPublicUuid) {
    return listInvoicesApi(orgPublicUuid);
  }

  async function fetchInvoice(invoicePublicUuid) {
    return getInvoiceApi(invoicePublicUuid);
  }

  // ---- Payments ----

  // Errors worth surfacing to the user: 403 (not OWNER), 409 (already
  // paid / a payment is already awaiting confirmation), 400 (method
  // unavailable). The backend message is shown as-is.
  async function submitPayment(invoicePublicUuid, data) {
    setIsPaymentSubmitting(true);
    setPaymentError(null);
    try {
      return await submitPaymentApi(invoicePublicUuid, data);
    } catch (err) {
      setPaymentError(err?.response?.data?.message || 'Failed to submit payment');
      throw err;
    } finally {
      setIsPaymentSubmitting(false);
    }
  }

  // Lets a page reset a stale submit error on mount, so an error from
  // one invoice never shows up on another invoice's page.
  const clearPaymentError = useCallback(() => setPaymentError(null), []);

  const value = {
    paymentMethods,
    isPaymentMethodsLoading,
    paymentMethodsError,
    refreshPaymentMethods,
    fetchInvoices,
    fetchInvoice,
    isPaymentSubmitting,
    paymentError,
    submitPayment,
    clearPaymentError,
  };

  return <PaymentContext.Provider value={value}>{children}</PaymentContext.Provider>;
}