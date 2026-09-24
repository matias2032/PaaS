import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useOrganization } from '../../organization/hooks/useOrganization';
import { useProject } from '../../project/hooks/useProject';
import { useService } from '../hooks/useService';
import BackButton from '../../../shared/components/BackButton';

/**
 * GET returns 404 when the repository has never been configured —
 * that's treated as an empty form (defaults), not an error banner.
 * gitConnectionPublicUuid is picked from the project's organization's
 * existing git connections (via useProject().fetchGitConnections),
 * same listing GitConnectionsPage already uses — never typed
 * freehand, per handoff section 3.3.
 */
function ServiceRepositoryPage() {
  const { publicUuid } = useParams();
  const { user } = useAuth();
  const { organizations, fetchOrganization, fetchMembers } = useOrganization();
  const { fetchGitConnections } = useProject();
  const {
    isRepositoryLoading,
    repositoryError,
    fetchService,
    fetchServiceRepository,
    upsertServiceRepository,
  } = useService();

  const [service, setService] = useState(null);
  const [organization, setOrganization] = useState(null);
  const [members, setMembers] = useState([]);
  const [gitConnections, setGitConnections] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  const [form, setForm] = useState({
    gitConnectionPublicUuid: '',
    repositoryUrl: '',
    repositoryOwner: '',
    repositoryName: '',
    branch: 'main',
  });

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setLoadError(null);
      try {
        const loadedService = await fetchService(publicUuid);
        const cachedOrg = organizations.find(
          (org) => org.publicUuid === loadedService.organizationPublicUuid
        );
        const [org, memberList, connections] = await Promise.all([
          cachedOrg
            ? Promise.resolve(cachedOrg)
            : fetchOrganization(loadedService.organizationPublicUuid),
          fetchMembers(loadedService.organizationPublicUuid),
          fetchGitConnections(loadedService.organizationPublicUuid),
        ]);

        let existing = null;
        try {
          existing = await fetchServiceRepository(publicUuid);
        } catch (err) {
          if (err?.response?.status !== 404) throw err;
        }

        if (!cancelled) {
          setService(loadedService);
          setOrganization(org);
          setMembers(memberList);
          setGitConnections(connections);
          if (existing) {
            setForm({
              gitConnectionPublicUuid: existing.gitConnectionPublicUuid || '',
              repositoryUrl: existing.repositoryUrl,
              repositoryOwner: existing.repositoryOwner || '',
              repositoryName: existing.repositoryName || '',
              branch: existing.branch,
            });
          }
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(err?.response?.data?.message || 'Failed to load repository settings');
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [publicUuid]);

  const currentMembership = members.find(
    (member) => member.userPublicUuid === user?.publicUuid
  );
  const isOwner = currentMembership?.roleCode === 'OWNER';
  const isOrganizationActive = organization?.status !== 'INACTIVE';
  const canManage = isOwner && isOrganizationActive;

  function handleFormChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSave(event) {
    event.preventDefault();
    try {
      await upsertServiceRepository(publicUuid, {
        ...form,
        gitConnectionPublicUuid: form.gitConnectionPublicUuid || undefined,
      });
    } catch {
      // Surfaced via context `repositoryError` already.
    }
  }

  if (isLoading) return <p>Loading...</p>;
  if (loadError) return <p className="service-repository-page__error">{loadError}</p>;

  return (
    <div className="service-repository-page">
      <BackButton />
      <h1>Repository{service ? ` — ${service.name}` : ''}</h1>

      {repositoryError && <p className="service-repository-page__error">{repositoryError}</p>}

      <form className="service-repository-page__form" onSubmit={handleSave}>
        <label>
          Git connection (optional)
          <select
            name="gitConnectionPublicUuid"
            value={form.gitConnectionPublicUuid}
            onChange={handleFormChange}
            disabled={!canManage}
          >
            <option value="">None</option>
            {gitConnections.map((connection) => (
              <option key={connection.publicUuid} value={connection.publicUuid}>
                {connection.gitProviderCode} — {connection.externalAccountName || connection.externalAccountId}
              </option>
            ))}
          </select>
        </label>

        <label>
          Repository URL
          <input
            name="repositoryUrl"
            value={form.repositoryUrl}
            maxLength={1000}
            required
            disabled={!canManage}
            onChange={handleFormChange}
          />
        </label>

        <label>
          Repository owner
          <input
            name="repositoryOwner"
            value={form.repositoryOwner}
            disabled={!canManage}
            onChange={handleFormChange}
          />
        </label>

        <label>
          Repository name
          <input
            name="repositoryName"
            value={form.repositoryName}
            disabled={!canManage}
            onChange={handleFormChange}
          />
        </label>

        <label>
          Branch
          <input
            name="branch"
            value={form.branch}
            disabled={!canManage}
            onChange={handleFormChange}
          />
        </label>

        {canManage && (
          <button type="submit" disabled={isRepositoryLoading}>
            Save
          </button>
        )}

        {!canManage && (
          <p className="service-repository-page__readonly-notice">
            You don't have permission to change this, or the organization is inactive.
          </p>
        )}
      </form>
    </div>
  );
}

export default ServiceRepositoryPage;