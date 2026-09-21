import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useProject } from '../hooks/useProject';
import { useOrganization } from '../../organization/hooks/useOrganization';
import GitConnectionCard from '../components/GitConnectionCard';
import BackButton from '../../../shared/components/BackButton';

/**
 * The backend exposes no "available providers for this org" endpoint
 * — this page computes it client-side from gitProviders (catalog)
 * minus whichever providers already have a non-revoked connection,
 * per the handoff's "one active connection per provider" rule.
 */
function GitConnectionsPage() {
  const { orgPublicUuid } = useParams();
  const { user } = useAuth();
  const { organizations, fetchOrganization, fetchMembers } = useOrganization();
  const {
    gitProviders,
    isGitProvidersLoading,
    gitProvidersError,
    refreshGitProviders,
    isGitConnectionsLoading,
    gitConnectionsError,
    fetchGitConnections,
    createGitConnection,
    revokeGitConnection,
  } = useProject();

  const [organization, setOrganization] = useState(
    () => organizations.find((org) => org.publicUuid === orgPublicUuid) || null
  );
  const [connections, setConnections] = useState([]);
  const [members, setMembers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [actionError, setActionError] = useState(null);

  const [form, setForm] = useState({
    gitProviderCode: '',
    externalAccountId: '',
    externalAccountName: '',
  });
  const [isCreating, setIsCreating] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setLoadError(null);
      try {
        const cachedOrg = organizations.find((org) => org.publicUuid === orgPublicUuid);
        const [org, connectionList, memberList] = await Promise.all([
          cachedOrg ? Promise.resolve(cachedOrg) : fetchOrganization(orgPublicUuid),
          fetchGitConnections(orgPublicUuid),
          fetchMembers(orgPublicUuid),
        ]);

        if (!cancelled) {
          setOrganization(org);
          setConnections(connectionList);
          setMembers(memberList);
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(err?.response?.data?.message || 'Failed to load git connections');
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    refreshGitProviders().catch(() => {
      // Surfaced via context `gitProvidersError` already.
    });
    load();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orgPublicUuid]);

  const currentMembership = members.find(
    (member) => member.userPublicUuid === user?.publicUuid
  );
  const isOwner = currentMembership?.roleCode === 'OWNER';
  // Mirrors OrganizationDetailPage — createGitConnection/revoke are
  // both requireActiveOrganization-gated server-side now.
  const isOrganizationActive = organization?.status !== 'INACTIVE';

  // Providers not yet connected (any non-REVOKED connection counts as
  // "taken") — see this page's top comment.
  const connectedProviderCodes = connections
    .filter((connection) => connection.status !== 'REVOKED')
    .map((connection) => connection.gitProviderCode);
  const availableProviders = gitProviders.filter(
    (provider) => !connectedProviderCodes.includes(provider.code)
  );

  function handleFormChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleCreate(event) {
    event.preventDefault();
    setActionError(null);
    setIsCreating(true);
    try {
      const created = await createGitConnection(orgPublicUuid, form);
      setConnections((prev) => [...prev, created]);
      setForm({ gitProviderCode: '', externalAccountId: '', externalAccountName: '' });
    } catch (err) {
      setActionError(err?.response?.data?.message || 'Failed to create git connection');
    } finally {
      setIsCreating(false);
    }
  }

  async function handleRevoke(connection) {
    // eslint-disable-next-line no-alert
    const confirmed = window.confirm(
      `Revoke the connection to ${connection.gitProviderCode}? This can't be undone — you'll need to create a new connection to restore it.`
    );
    if (!confirmed) return;

    setActionError(null);
    try {
      const updated = await revokeGitConnection(connection.publicUuid);
      setConnections((prev) =>
        prev.map((c) => (c.publicUuid === updated.publicUuid ? updated : c))
      );
    } catch (err) {
      setActionError(err?.response?.data?.message || 'Failed to revoke git connection');
    }
  }

  if (isLoading) return <p>Loading...</p>;
  if (loadError) return <p className="git-connections-page__error">{loadError}</p>;

  const displayError = actionError || gitConnectionsError || gitProvidersError;

  return (
    <div className="git-connections-page">
      <BackButton />
      <h1>Git connections{organization ? ` for ${organization.name}` : ''}</h1>

      {displayError && <p className="git-connections-page__error">{displayError}</p>}

      {isOwner && !isOrganizationActive && (
        <p className="git-connections-page__inactive-notice">
          This organization is inactive — no new connections can be created until it's
          reactivated.
        </p>
      )}

      {isOwner && isOrganizationActive && (
        <form className="git-connections-page__create-form" onSubmit={handleCreate}>
          <h2>Connect a Git provider</h2>
          <p className="git-connections-page__manual-notice">
            There&apos;s no automatic sign-in with your Git provider yet — enter your account
            details manually below.
          </p>

          <label>
            Provider
            <select
              name="gitProviderCode"
              value={form.gitProviderCode}
              onChange={handleFormChange}
              required
              disabled={isGitProvidersLoading || availableProviders.length === 0}
            >
              <option value="" disabled>
                Select a provider
              </option>
              {availableProviders.map((provider) => (
                <option key={provider.idGitProvider} value={provider.code}>
                  {provider.name}
                </option>
              ))}
            </select>
          </label>

          {availableProviders.length === 0 && !isGitProvidersLoading && (
            <p>All available providers are already connected.</p>
          )}

          <label>
            Account ID
            <input
              name="externalAccountId"
              value={form.externalAccountId}
              onChange={handleFormChange}
            />
          </label>
          <label>
            Account name
            <input
              name="externalAccountName"
              value={form.externalAccountName}
              onChange={handleFormChange}
            />
          </label>

          <button
            type="submit"
            disabled={isCreating || isGitConnectionsLoading || !form.gitProviderCode}
          >
            Connect
          </button>
        </form>
      )}

      {connections.length === 0 ? (
        <p>No git connections yet.</p>
      ) : (
        <div className="git-connections-page__grid">
          {connections.map((connection) => (
            <GitConnectionCard
              key={connection.publicUuid}
              connection={connection}
              canManage={isOwner && isOrganizationActive}
              isBusy={isGitConnectionsLoading}
              onRevoke={handleRevoke}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export default GitConnectionsPage;