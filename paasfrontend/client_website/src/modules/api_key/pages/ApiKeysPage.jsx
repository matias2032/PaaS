import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useOrganization, isOrganizationWritable } from '../../organization/hooks/useOrganization';
import { useApiKeys } from '../hooks/useApiKeys';
import ApiKeyList from '../components/ApiKeyList';
import CreateApiKeyForm from '../components/CreateApiKeyForm';
import BackButton from '../../../shared/components/BackButton';

/**
 * API keys of one organization. Any member sees the list; only an OWNER
 * of an ACTIVE organization can create/revoke. The organization is
 * always re-fetched on mount (refreshOrganization) because its status
 * may have changed platform-side since it was cached.
 */
function ApiKeysPage() {
  const { publicUuid } = useParams();
  const { user } = useAuth();
  const { organizations, refreshOrganization, fetchMembers } = useOrganization();
  const { keys, isLoading, loadError, load, create, revoke } = useApiKeys(publicUuid);

  const [members, setMembers] = useState([]);
  const [isBootstrapping, setIsBootstrapping] = useState(true);
  const [bootError, setBootError] = useState(null);
  const [actionError, setActionError] = useState(null);

  // Read from the context list so refreshOrganization() re-renders this page.
  const organization = organizations.find((org) => org.publicUuid === publicUuid) || null;

  useEffect(() => {
    let cancelled = false;
    async function bootstrap() {
      setIsBootstrapping(true);
      setBootError(null);
      try {
        const [, memberList] = await Promise.all([
          refreshOrganization(publicUuid),
          fetchMembers(publicUuid),
          load(),
        ]);
        if (!cancelled) setMembers(memberList);
      } catch (err) {
        if (!cancelled) {
          setBootError(err?.response?.data?.message || 'Failed to load this page');
        }
      } finally {
        if (!cancelled) setIsBootstrapping(false);
      }
    }
    bootstrap();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [publicUuid]);

  const currentMembership = members.find((m) => m.userPublicUuid === user?.publicUuid);
  const isOwner = currentMembership?.roleCode === 'OWNER';
  const writable = isOrganizationWritable(organization);
  const canManage = isOwner && writable;

  // Called after a 409: refreshes the org and reports whether it is still
  // writable (=> the 409 was something else, e.g. a duplicate name).
  async function handleConflict() {
    try {
      const fresh = await refreshOrganization(publicUuid);
      return isOrganizationWritable(fresh);
    } catch {
      return true;
    }
  }

  async function handleRevoke(apiKey) {
    // eslint-disable-next-line no-alert
    const confirmed = window.confirm(
      `Revoke "${apiKey.name}"? Anything using this key will stop working immediately. This can't be undone.`
    );
    if (!confirmed) return;

    setActionError(null);
    try {
      await revoke(apiKey.publicUuid);
    } catch (err) {
      setActionError(err?.response?.data?.message || 'Failed to revoke API key');
      // The state may have changed between load and action (org suspended,
      // key already revoked elsewhere): resync both.
      handleConflict();
      load().catch(() => {});
    }
  }

  if (isBootstrapping) return <p>Loading...</p>;
  if (bootError && !organization) return <p className="api-keys-page__error">{bootError}</p>;
  if (!organization) return <p>Organization not found.</p>;

  return (
    <div className="api-keys-page">
      <BackButton />
      <h1>API keys — {organization.name}</h1>

      {organization.status === 'SUSPENDED' && (
        <div className="api-keys-page__suspended-notice" role="alert">
          <p>
            This organization is suspended. API keys can't be created or revoked
            until the suspension is lifted. Contact support for help.
          </p>
          {organization.suspensionReason && <p>Reason: {organization.suspensionReason}</p>}
        </div>
      )}
      {organization.status === 'INACTIVE' && (
        <p className="api-keys-page__inactive-notice">
          This organization is inactive. API keys can't be created or revoked
          until it's reactivated.
        </p>
      )}
      {!isOwner && writable && (
        <p className="api-keys-page__readonly-hint">Only owners can create or revoke API keys.</p>
      )}

      {isOwner && (
        <section className="api-keys-page__create">
          <h2>Create API key</h2>
          <CreateApiKeyForm onCreate={create} onConflict={handleConflict} disabled={!writable} />
        </section>
      )}

      <section className="api-keys-page__list">
        <h2>Keys</h2>
        {actionError && <p className="api-keys-page__error">{actionError}</p>}
        {loadError && <p className="api-keys-page__error">{loadError}</p>}
        {isLoading && keys.length === 0 ? (
          <p>Loading...</p>
        ) : (
          <ApiKeyList keys={keys} canRevoke={canManage} onRevoke={handleRevoke} />
        )}
      </section>
    </div>
  );
}

export default ApiKeysPage;