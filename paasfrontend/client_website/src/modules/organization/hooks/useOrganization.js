import { useContext } from 'react';
import { OrganizationContext } from '../context/OrganizationContext';

/**
 * Convenience hook over OrganizationContext. Mirrors modules/auth/hooks/useAuth.js.
 * Throws if used outside <OrganizationProvider>, same pattern as useAuth.
 */
export function useOrganization() {
  const context = useContext(OrganizationContext);
  if (context === undefined) {
    throw new Error('useOrganization must be used within an OrganizationProvider');
  }
  return context;
}

/**
 * Single decision point for "can this organization be written to".
 * Based on `status` only, never on backend error messages (the backend
 * throws the same OrganizationInactiveException for INACTIVE and
 * SUSPENDED). Whitelist on purpose: any unknown status is read-only.
 */
export function isOrganizationWritable(organization) {
  return organization?.status === 'ACTIVE';
}