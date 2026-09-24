import { useContext } from 'react';
import { ServiceContext } from '../context/ServiceContext';

/**
 * Mirrors useProject.js exactly: a thin context accessor that throws
 * if used outside <ServiceProvider>, so a missing provider fails
 * loudly at the call site instead of silently returning undefined.
 */
export function useService() {
  const context = useContext(ServiceContext);

  if (context === undefined) {
    throw new Error('useService must be used within a ServiceProvider');
  }

  return context;
}