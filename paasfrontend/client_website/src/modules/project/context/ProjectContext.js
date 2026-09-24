import { createContext } from 'react';

/**
 * Holds: the git providers catalog (global, read-only — mirrors
 * BillingContext's `plans`), plus CRUD/lifecycle operations over
 * projects and git connections, both scoped to whichever organization
 * the caller passes in per-call.
 *
 * Projects and git connections are NOT cached here, same reasoning as
 * BillingContext not caching `subscription`/`subscriptionHistory`:
 * they belong to one organization at a time, so the owning page keeps
 * its own local state instead of this context guessing which
 * organization is "current". Only loading/error per domain live here
 * — split into `projects` vs `gitConnections` rather than one shared
 * pair, so a failure in one never blanks out an unrelated error
 * shown elsewhere on the same page (e.g. GitConnectionsPage failing
 * to revoke a connection shouldn't blank a projects-list error).
 */
export const ProjectContext = createContext(undefined);