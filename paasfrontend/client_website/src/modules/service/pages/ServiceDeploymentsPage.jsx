import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useOrganization } from '../../organization/hooks/useOrganization';
import { useService } from '../hooks/useService';
import BackButton from '../../../shared/components/BackButton';

const TRIGGER_TYPES = ['MANUAL', 'GIT_PUSH', 'WEBHOOK', 'API', 'SYSTEM'];

/**
 * This page logs a deployment record — it never triggers a real
 * deployment (no Coolify yet). The create button says "Log a
 * deployment", not "Deploy now", and every entry's status is always
 * QUEUED and never advances, per handoff section 3.8.
 */
function ServiceDeploymentsPage() {
  const { publicUuid } = useParams();
  const { user } = useAuth();
  const { organizations, fetchOrganization, fetchMembers } = useOrganization();
  const {
    isDeploymentsLoading,
    deploymentsError,
    fetchService,
    fetchDeployments,
    createDeployment,
  } = useService();

  const [service, setService] = useState(null);
  const [organization, setOrganization] = useState(null);
  const [members, setMembers] = useState([]);
  const [deployments, setDeployments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  const [form, setForm] = useState({
    triggerType: 'MANUAL',
    commitHash: '',
    commitMessage: '',
    branch: '',
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
        const [org, memberList, deploymentList] = await Promise.all([
          cachedOrg
            ? Promise.resolve(cachedOrg)
            : fetchOrganization(loadedService.organizationPublicUuid),
          fetchMembers(loadedService.organizationPublicUuid),
          fetchDeployments(publicUuid),
        ]);

        if (!cancelled) {
          setService(loadedService);
          setOrganization(org);
          setMembers(memberList);
          setDeployments(deploymentList);
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(err?.response?.data?.message || 'Failed to load deployments');
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

  async function handleCreate(event) {
    event.preventDefault();
    try {
      const created = await createDeployment(publicUuid, form);
      // List is ordered most-recent-first server-side — prepend
      // rather than append to keep that order without a refetch.
      setDeployments((prev) => [created, ...prev]);
      setForm({ triggerType: 'MANUAL', commitHash: '', commitMessage: '', branch: '' });
    } catch {
      // Surfaced via context `deploymentsError` already.
    }
  }

  if (isLoading) return <p>Loading...</p>;
  if (loadError) return <p className="service-deployments-page__error">{loadError}</p>;

  return (
    <div className="service-deployments-page">
      <BackButton />
      <h1>Deployments{service ? ` — ${service.name}` : ''}</h1>
      <p className="service-deployments-page__notice">
        This is a manual history log — it doesn't trigger a real deployment yet.
      </p>

      {deploymentsError && <p className="service-deployments-page__error">{deploymentsError}</p>}

      {deployments.length === 0 ? (
        <p>No deployments logged yet.</p>
      ) : (
        <ul className="service-deployments-page__list">
          {deployments.map((deployment) => (
            <li key={deployment.publicUuid} className="service-deployments-page__row">
              <span className="service-deployments-page__status">{deployment.status}</span>
              <span className="service-deployments-page__trigger">{deployment.triggerType}</span>
              {deployment.branch && (
                <span className="service-deployments-page__branch">{deployment.branch}</span>
              )}
              {deployment.commitHash && (
                <span className="service-deployments-page__commit">{deployment.commitHash}</span>
              )}
              {deployment.commitMessage && (
                <span className="service-deployments-page__message">{deployment.commitMessage}</span>
              )}
            </li>
          ))}
        </ul>
      )}

      {canManage && (
        <form className="service-deployments-page__form" onSubmit={handleCreate}>
          <h2>Log a deployment</h2>
          <label>
            Trigger type
            <select name="triggerType" value={form.triggerType} onChange={handleFormChange}>
              {TRIGGER_TYPES.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </label>
          <label>
            Branch
            <input name="branch" value={form.branch} onChange={handleFormChange} />
          </label>
          <label>
            Commit hash
            <input name="commitHash" value={form.commitHash} onChange={handleFormChange} />
          </label>
          <label>
            Commit message
            <input name="commitMessage" value={form.commitMessage} onChange={handleFormChange} />
          </label>
          <button type="submit" disabled={isDeploymentsLoading}>
            Log a deployment
          </button>
        </form>
      )}

      {!canManage && (
        <p className="service-deployments-page__readonly-notice">
          You don't have permission to log a deployment, or the organization is inactive.
        </p>
      )}
    </div>
  );
}

export default ServiceDeploymentsPage;