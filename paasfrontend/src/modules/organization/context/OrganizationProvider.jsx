import { useEffect, useState, useCallback } from 'react';
import {
  createOrganization as createOrganizationApi,
  getOrganization as getOrganizationApi,
  updateOrganization as updateOrganizationApi,
  deactivateOrganization as deactivateOrganizationApi,
  listMyOrganizations as listMyOrganizationsApi,
  listRoles as listRolesApi,
  addMember as addMemberApi,
  removeMember as removeMemberApi,
  changeMemberRole as changeMemberRoleApi,
  listMembers as listMembersApi,
} from '../api/organizationApi';
import { OrganizationContext } from './OrganizationContext';

const ACTIVE_ORG_STORAGE_KEY = 'activeOrganizationUuid';

function loadStoredActiveOrgUuid() {
  try {
    return localStorage.getItem(ACTIVE_ORG_STORAGE_KEY) || null;
  } catch {
    return null;
  }
}

function persistActiveOrgUuid(publicUuid) {
  if (publicUuid) {
    localStorage.setItem(ACTIVE_ORG_STORAGE_KEY, publicUuid);
  } else {
    localStorage.removeItem(ACTIVE_ORG_STORAGE_KEY);
  }
}

export function OrganizationProvider({ children }) {
  // List of organizations the current user belongs to (GET /api/organizations).
  const [organizations, setOrganizations] = useState([]);

  // Just the publicUuid is persisted; the full OrganizationResponse for
  // the active org is derived from `organizations` below, so it never
  // goes stale relative to the list (e.g. after a rename via updateOrganization).
  const [activeOrgUuid, setActiveOrgUuid] = useState(loadStoredActiveOrgUuid);

  // Roles catalog (GET /api/organizations/roles) — fetched once, not
  // organization-scoped. Cached here so AddMemberForm doesn't refetch
  // on every mount.
  const [roles, setRoles] = useState([]);

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    persistActiveOrgUuid(activeOrgUuid);
  }, [activeOrgUuid]);

  // Reacts to the same global event AuthProvider listens to (dispatched
  // by httpClient on a real 401). Keeps this module decoupled from the
  // auth module — no direct import of AuthContext/useAuth here. Clears
  // all organization state so a subsequent login (possibly as a
  // different user, same tab) never sees stale org data.
  //
  // NOTE: the initial fetch (refreshOrganizations() after a successful
  // login) is intentionally NOT triggered here — that's a composition
  // concern, wired in AppShell.jsx (shared/layout, still pending —
  // dívida técnica #2), the one place that legitimately depends on both
  // AuthProvider and OrganizationProvider being mounted together.
  useEffect(() => {
    function handleUnauthorized() {
      setOrganizations([]);
      setActiveOrgUuid(null);
      setRoles([]);
      setError(null);
    }

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  const activeOrganization =
    organizations.find((org) => org.publicUuid === activeOrgUuid) || null;

  // Fetches the user's organizations and, if there's no active org yet
  // (first load) or the persisted one is no longer in the list (e.g. user
  // was removed from it), falls back to the first one available.
  const refreshOrganizations = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const list = await listMyOrganizationsApi();
      setOrganizations(list);

      setActiveOrgUuid((prev) => {
        const stillValid = prev && list.some((org) => org.publicUuid === prev);
        if (stillValid) return prev;
        return list[0]?.publicUuid ?? null;
      });

      return list;
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load organizations');
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  async function fetchRoles() {
    setError(null);
    try {
      const list = await listRolesApi();
      setRoles(list);
      return list;
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load roles');
      throw err;
    }
  }

  async function createOrganization(data) {
    setIsLoading(true);
    setError(null);
    try {
      const created = await createOrganizationApi(data);
      setOrganizations((prev) => [...prev, created]);
      setActiveOrgUuid(created.publicUuid);
      return created;
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to create organization');
      throw err;
    } finally {
      setIsLoading(false);
    }
  }

  // Not currently used by refreshOrganizations (list endpoint already
  // returns full OrganizationResponse per item), but exposed for
  // OrganizationDetailPage to re-fetch a single org directly by UUID
  // (e.g. on deep-link navigation, without waiting for the full list).
  async function fetchOrganization(publicUuid) {
    setError(null);
    try {
      return await getOrganizationApi(publicUuid);
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load organization');
      throw err;
    }
  }

  async function updateOrganization(publicUuid, data) {
    setIsLoading(true);
    setError(null);
    try {
      const updated = await updateOrganizationApi(publicUuid, data);
      setOrganizations((prev) =>
        prev.map((org) => (org.publicUuid === publicUuid ? updated : org))
      );
      return updated;
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to update organization');
      throw err;
    } finally {
      setIsLoading(false);
    }
  }

    // Soft-delete. Keeps the org in `organizations` with status
  // "INACTIVE" rather than removing it from the list — callers (e.g.
  // OrganizationDetailPage) decide what to do next (navigate away,
  // show a banner, etc.). Does NOT clear activeOrgUuid automatically:
  // if the deactivated org was active, it stays selected but inactive
  // until the caller picks another one via selectOrganization.
  async function deactivateOrganization(publicUuid) {
    setIsLoading(true);
    setError(null);
    try {
      const updated = await deactivateOrganizationApi(publicUuid);
      setOrganizations((prev) =>
        prev.map((org) => (org.publicUuid === publicUuid ? updated : org))
      );
      return updated;
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to deactivate organization');
      throw err;
    } finally {
      setIsLoading(false);
    }
  }

  function selectOrganization(publicUuid) {
    setActiveOrgUuid(publicUuid);
  }

  // Members are intentionally NOT stored in this context's top-level
  // state (unlike organizations/roles): membership lists are per-org and
  // only needed on OrganizationDetailPage. Keeping them out avoids stale
  // member lists lingering after switching active org. Exposed as plain
  // async functions; the page owns its own local state for the list.
  async function fetchMembers(publicUuid) {
    setError(null);
    try {
      return await listMembersApi(publicUuid);
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load members');
      throw err;
    }
  }

  async function addMember(publicUuid, data) {
    setError(null);
    try {
      return await addMemberApi(publicUuid, data);
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to add member');
      throw err;
    }
  }

  async function removeMember(publicUuid, userPublicUuid) {
    setError(null);
    try {
      await removeMemberApi(publicUuid, userPublicUuid);
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to remove member');
      throw err;
    }
  }
  
    // Same non-storing pattern as addMember/removeMember: role changes
  // are per-org-detail-page, not cached at context level.
  async function changeMemberRole(publicUuid, userPublicUuid, roleCode) {
    setError(null);
    try {
      return await changeMemberRoleApi(publicUuid, userPublicUuid, { roleCode });
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to change member role');
      throw err;
    }
  }

  const value = {
    organizations,
    activeOrganization,
    activeOrgUuid,
    roles,
    isLoading,
    error,
    refreshOrganizations,
    fetchRoles,
    createOrganization,
    fetchOrganization,
    updateOrganization,
    deactivateOrganization,
    selectOrganization,
    fetchMembers,
    addMember,
    removeMember,
    changeMemberRole,
  };

  return (
    <OrganizationContext.Provider value={value}>
      {children}
    </OrganizationContext.Provider>
  );
}