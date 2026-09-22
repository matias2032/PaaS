import { createContext } from 'react';

/**
 * Holds: the service types catalog (global, read-only — mirrors
 * ProjectContext's `gitProviders`), plus CRUD/lifecycle operations
 * over services and all 6 related sub-resources (repository,
 * build-config, resource-config, environment variables, domains,
 * deployments), each scoped to whichever servicePublicUuid (or
 * projectPublicUuid, for create/list) the caller passes in per-call.
 *
 * None of the sub-resource data is cached here, same reasoning as
 * ProjectContext not caching `projects`/`gitConnections`: they
 * belong to one service at a time, so the owning page keeps its own
 * local state instead of this context guessing which service is
 * "current". Only loading/error per domain live here — split into 8
 * independent pairs (serviceTypes, services, repository,
 * buildConfig, resourceConfig, environmentVariables, domains,
 * deployments) rather than one shared pair, so a failure in one
 * never blanks out an unrelated error shown elsewhere on the same
 * page (e.g. ServiceDetailPage's env-vars error shouldn't blank a
 * domains error on the same screen).
 */
export const ServiceContext = createContext(undefined);