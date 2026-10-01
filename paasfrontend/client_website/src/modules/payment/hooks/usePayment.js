import { useContext } from 'react';
import { PaymentContext } from '../context/PaymentContext';

/**
 * Mirrors useBilling.js exactly: a thin context accessor that throws
 * if used outside <PaymentProvider>, so a missing provider fails
 * loudly at the call site instead of silently returning undefined.
 */
export function usePayment() {
  const context = useContext(PaymentContext);

  if (context === undefined) {
    throw new Error('usePayment must be used within a PaymentProvider');
  }

  return context;
}