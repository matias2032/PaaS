import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useOrganization } from '../../organization/hooks/useOrganization';
import { useService } from '../hooks/useService';
import BackButton from '../../../shared/components/BackButton';

/**
 * EnvironmentVariableResponse never includes `value` (handoff section
 * 3.6) — this page treats the value field like a password: creating
 * a new key or overwriting an existing one both start from a blank
 * `value`, and the list never shows or pre-fills it. "Editing" an
 * existing key really means "write a brand-new value from scratch",
 * so the form doesn't distinguish create vs edit beyond pre-filling
 * variableKey/isSecret when a row's own edit action is used.
 */
function ServiceEnvironmentVariablesPage() {
  const { publicUuid } = useParams();
  const { user } = useAuth();
  const { organizations, fetchOrganization, fetchMembers } = useOrganization();
  const {
    isEnvironmentVariablesLoading,
    environmentVariablesError,
    fetchService,
    fetchEnvironmentVariables,
    upsertEnvironmentVariable,
    deleteEnvironmentVariable,
  } = useService();

  const [service, setService] = useState(null);
  const [organization, setOrganization] = useState(null);
  const [members, setMembers] = useState([]);
  const [variables, setVariables] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  const [form, setForm] = useState({ variableKey: '', value: '', isSecret: true });

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
        const [org, memberList, variableList] = await Promise.all([
          cachedOrg
            ? Promise.resolve(cachedOrg)
            : fetchOrganization(loadedService.organizationPublicUuid),
          fetchMembers(loadedService.organizationPublicUuid),
          fetchEnvironmentVariables(publicUuid),
        ]);

        if (!cancelled) {
          setService(loadedService);
          setOrganization(org);
          setMembers(memberList);
          setVariables(variableList);
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(err?.response?.data?.message || 'Failed to load environment variables');
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
    const { name, value, type, checked } = event.target;
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  }

  // Pre-fills key/isSecret only — value always starts blank, even
  // when overwriting an existing key. See the page's top comment.
  function handleEditKey(variable) {
    setForm({ variableKey: variable.variableKey, value: '', isSecret: variable.isSecret });
  }

  async function handleSave(event) {
    event.preventDefault();
    try {
      const saved = await upsertEnvironmentVariable(publicUuid, form);
      setVariables((prev) => {
        const withoutKey = prev.filter((v) => v.variableKey !== saved.variableKey);
        return [...withoutKey, saved];
      });
      setForm({ variableKey: '', value: '', isSecret: true });
    } catch {
      // Surfaced via context `environmentVariablesError` already.
    }
  }

  async function handleDelete(variable) {
    // eslint-disable-next-line no-alert
    const confirmed = window.confirm(`Delete "${variable.variableKey}"? This can't be undone.`);
    if (!confirmed) return;

    try {
      await deleteEnvironmentVariable(publicUuid, variable.variableKey);
      setVariables((prev) => prev.filter((v) => v.variableKey !== variable.variableKey));
    } catch {
      // Surfaced via context `environmentVariablesError` already.
    }
  }

  if (isLoading) return <p>Loading...</p>;
  if (loadError) return <p className="service-env-vars-page__error">{loadError}</p>;

  return (
    <div className="service-env-vars-page">
      <BackButton />
      <h1>Environment variables{service ? ` — ${service.name}` : ''}</h1>

      {environmentVariablesError && (
        <p className="service-env-vars-page__error">{environmentVariablesError}</p>
      )}

      {variables.length === 0 ? (
        <p>No environment variables yet.</p>
      ) : (
        <ul className="service-env-vars-page__list">
          {variables.map((variable) => (
            <li key={variable.variableKey} className="service-env-vars-page__row">
              <span className="service-env-vars-page__key">{variable.variableKey}</span>
              <span className="service-env-vars-page__secret-badge">
                {variable.isSecret ? 'secret' : 'plain'}
              </span>
              {canManage && (
                <>
                  <button type="button" onClick={() => handleEditKey(variable)}>
                    Overwrite value
                  </button>
                  <button type="button" onClick={() => handleDelete(variable)}>
                    Delete
                  </button>
                </>
              )}
            </li>
          ))}
        </ul>
      )}

      {canManage && (
        <form className="service-env-vars-page__form" onSubmit={handleSave}>
          <h2>Set a variable</h2>
          <p className="service-env-vars-page__value-notice">
            The value is write-only — it's never shown back to you once saved, even here.
          </p>
          <label>
            Key
            <input
              name="variableKey"
              value={form.variableKey}
              maxLength={255}
              required
              onChange={handleFormChange}
            />
          </label>
          <label>
            Value
            <input
              name="value"
              type="password"
              value={form.value}
              required
              onChange={handleFormChange}
            />
          </label>
          <label>
            <input
              type="checkbox"
              name="isSecret"
              checked={form.isSecret}
              onChange={handleFormChange}
            />
            Secret
          </label>
          <button type="submit" disabled={isEnvironmentVariablesLoading}>
            Save
          </button>
        </form>
      )}

      {!canManage && (
        <p className="service-env-vars-page__readonly-notice">
          You don't have permission to change this, or the organization is inactive.
        </p>
      )}
    </div>
  );
}

export default ServiceEnvironmentVariablesPage;