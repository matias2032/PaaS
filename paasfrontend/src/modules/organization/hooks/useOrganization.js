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