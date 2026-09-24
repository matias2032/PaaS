import { useCallback, useEffect, useState } from 'react';
import {
  createProject as createProjectApi,
  listProjects as listProjectsApi,
  getProject as getProjectApi,
  updateProject as updateProjectApi,
  archiveProject as archiveProjectApi,
  reactivateProject as reactivateProjectApi,
  listGitProviders as listGitProvidersApi,
  createGitConnection as createGitConnectionApi,
  listGitConnections as listGitConnectionsApi,
  revokeGitConnection as revokeGitConnectionApi,
} from '../api/projectApi';
import { ProjectContext } from './ProjectContext';

export function ProjectProvider({ children }) {
  // Git providers catalog (GET /api/git-providers) — global, read-only,
  // seeded server-side (GITHUB/GITLAB/BITBUCKET). Mirrors
  // BillingProvider's `plans`: fetched via refreshGitProviders() and
  // kept until the next refresh, since it practically never changes.
  const [gitProviders, setGitProviders] = useState([]);
  const [isGitProvidersLoading, setIsGitProvidersLoading] = useState(false);
  const [gitProvidersError, setGitProvidersError] = useState(null);

  // Covers createProject/listProjects/getProject/updateProject/
  // archiveProject/reactivateProject. No project list is cached here
  // — see ProjectContext.js for why.
  const [isProjectsLoading, setIsProjectsLoading] = useState(false);
  const [projectsError, setProjectsError] = useState(null);

  // Covers createGitConnection/listGitConnections/revokeGitConnection.
  // Kept separate from projectsError for the same reason billing keeps
  // plansError and subscriptionError apart — different operation
  // domain, can fail independently at the same time on the same page.
  const [isGitConnectionsLoading, setIsGitConnectionsLoading] = useState(false);
  const [gitConnectionsError, setGitConnectionsError] = useState(null);

  // Same global event every provider reacts to (dispatched by
  // httpClient on a real 401) — clears all project state so a
  // subsequent login never sees stale catalog/error data from a
  // previous session.
  useEffect(() => {
    function handleUnauthorized() {
      setGitProviders([]);
      setGitProvidersError(null);
      setProjectsError(null);
      setGitConnectionsError(null);
    }

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  // ---- Git providers (catalog) ----

  const refreshGitProviders = useCallback(async () => {
    setIsGitProvidersLoading(true);
    setGitProvidersError(null);
    try {
      const list = await listGitProvidersApi();
      setGitProviders(list);
      return list;
    } catch (err) {
      setGitProvidersError(err?.response?.data?.message || 'Failed to load git providers');
      throw err;
    } finally {
      setIsGitProvidersLoading(false);
    }
  }, []);

  // ---- Projects (org-scoped — caller owns the list) ----

  async function fetchProjects(orgPublicUuid) {
    setProjectsError(null);
    try {
      return await listProjectsApi(orgPublicUuid);
    } catch (err) {
      setProjectsError(err?.response?.data?.message || 'Failed to load projects');
      throw err;
    }
  }

  async function fetchProject(publicUuid) {
    setProjectsError(null);
    try {
      return await getProjectApi(publicUuid);
    } catch (err) {
      setProjectsError(err?.response?.data?.message || 'Failed to load project');
      throw err;
    }
  }

  async function createProject(orgPublicUuid, data) {
    setIsProjectsLoading(true);
    setProjectsError(null);
    try {
      return await createProjectApi(orgPublicUuid, data);
    } catch (err) {
      setProjectsError(err?.response?.data?.message || 'Failed to create project');
      throw err;
    } finally {
      setIsProjectsLoading(false);
    }
  }

  async function updateProject(publicUuid, data) {
    setIsProjectsLoading(true);
    setProjectsError(null);
    try {
      return await updateProjectApi(publicUuid, data);
    } catch (err) {
      setProjectsError(err?.response?.data?.message || 'Failed to update project');
      throw err;
    } finally {
      setIsProjectsLoading(false);
    }
  }

  async function archiveProject(publicUuid) {
    setIsProjectsLoading(true);
    setProjectsError(null);
    try {
      return await archiveProjectApi(publicUuid);
    } catch (err) {
      setProjectsError(err?.response?.data?.message || 'Failed to archive project');
      throw err;
    } finally {
      setIsProjectsLoading(false);
    }
  }

  // Mirrors archiveProject exactly (ARCHIVED -> ACTIVE only).
  async function reactivateProject(publicUuid) {
    setIsProjectsLoading(true);
    setProjectsError(null);
    try {
      return await reactivateProjectApi(publicUuid);
    } catch (err) {
      setProjectsError(err?.response?.data?.message || 'Failed to reactivate project');
      throw err;
    } finally {
      setIsProjectsLoading(false);
    }
  }

  // ---- Git connections (org-scoped — caller owns the list) ----

  async function fetchGitConnections(orgPublicUuid) {
    setGitConnectionsError(null);
    try {
      return await listGitConnectionsApi(orgPublicUuid);
    } catch (err) {
      setGitConnectionsError(err?.response?.data?.message || 'Failed to load git connections');
      throw err;
    }
  }

  async function createGitConnection(orgPublicUuid, data) {
    setIsGitConnectionsLoading(true);
    setGitConnectionsError(null);
    try {
      return await createGitConnectionApi(orgPublicUuid, data);
    } catch (err) {
      setGitConnectionsError(err?.response?.data?.message || 'Failed to create git connection');
      throw err;
    } finally {
      setIsGitConnectionsLoading(false);
    }
  }

  async function revokeGitConnection(publicUuid) {
    setIsGitConnectionsLoading(true);
    setGitConnectionsError(null);
    try {
      return await revokeGitConnectionApi(publicUuid);
    } catch (err) {
      setGitConnectionsError(err?.response?.data?.message || 'Failed to revoke git connection');
      throw err;
    } finally {
      setIsGitConnectionsLoading(false);
    }
  }

  const value = {
    gitProviders,
    isGitProvidersLoading,
    gitProvidersError,
    refreshGitProviders,
    isProjectsLoading,
    projectsError,
    fetchProjects,
    fetchProject,
    createProject,
    updateProject,
    archiveProject,
    reactivateProject,
    isGitConnectionsLoading,
    gitConnectionsError,
    fetchGitConnections,
    createGitConnection,
    revokeGitConnection,
  };

  return <ProjectContext.Provider value={value}>{children}</ProjectContext.Provider>;
}