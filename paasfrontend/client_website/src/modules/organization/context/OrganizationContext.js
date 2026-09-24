import { createContext } from 'react';

/**
 * Holds: the list of organizations the current user belongs to, the
 * currently active/selected organization, and CRUD/member operations
 * over them. Mirrors the shape convention used by AuthContext.js.
 */
export const OrganizationContext = createContext(undefined);