import { useContext } from 'react';
import { BillingContext } from '../context/BillingContext';

/**
 * Mirrors useOrganization.js exactly: a thin context accessor that
 * throws if used outside <BillingProvider>, so a missing provider
 * fails loudly at the call site instead of silently returning
 * undefined.
 */
export function useBilling() {
  const context = useContext(BillingContext);

  if (context === undefined) {
    throw new Error('useBilling must be used within a BillingProvider');
  }

  return context;
}