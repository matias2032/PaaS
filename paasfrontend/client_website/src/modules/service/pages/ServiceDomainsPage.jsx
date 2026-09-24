import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useOrganization } from '../../organization/hooks/useOrganization';
import { useService } from '../hooks/useService';
import BackButton from '../../../shared/components/BackButton';

/**
 * Both verificationStatus and sslStatus stay PENDING for every
 * domain right now (no real Coolify issuing certs or verifying DNS
 * yet) — the status badges below are worded as a stable state, not
 * as "processing" or "in progress", per handoff section 3.7.
 */
function ServiceDomainsPage() {
  const { publicUuid } = useParams();
  const { user } = useAuth();
  const { organizations, fetchOrganization, fetchMembers } = useOrganization();
  const { isDomainsLoading, domainsError, fetchService, fetchDomains, createDomain, deleteDomain } =
    useService();

  const [service, setService] = useState(null);
  const [organization, setOrganization] = useState(null);
  const [members, setMembers] = useState([]);
  const [domains, setDomains] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  const [form, setForm] = useState({ hostname: '', isPrimary: false });

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
        const [org, memberList, domainList] = await Promise.all([
          cachedOrg
            ? Promise.resolve(cachedOrg)
            : fetchOrganization(loadedService.organizationPublicUuid),
          fetchMembers(loadedService.organizationPublicUuid),
          fetchDomains(publicUuid),
        ]);

        if (!cancelled) {
          setService(loadedService);
          setOrganization(org);
          setMembers(memberList);
          setDomains(domainList);
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(err?.response?.data?.message || 'Failed to load domains');
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

  async function handleCreate(event) {
    event.preventDefault();
    try {
      const created = await createDomain(publicUuid, form);
      // Creating a new primary unsets the previous one server-side —
      // reflect that locally instead of refetching the whole list.
      setDomains((prev) =>
        created.isPrimary
          ? [...prev.map((d) => ({ ...d, isPrimary: false })), created]
          : [...prev, created]
      );
      setForm({ hostname: '', isPrimary: false });
    } catch {
      // Surfaced via context `domainsError` already.
    }
  }

  async function handleDelete(domain) {
    // eslint-disable-next-line no-alert
    const confirmed = window.confirm(`Delete "${domain.hostname}"? This can't be undone.`);
    if (!confirmed) return;

    try {
      await deleteDomain(domain.publicUuid);
      setDomains((prev) => prev.filter((d) => d.publicUuid !== domain.publicUuid));
    } catch {
      // Surfaced via context `domainsError` already.
    }
  }

  if (isLoading) return <p>Loading...</p>;
  if (loadError) return <p className="service-domains-page__error">{loadError}</p>;

  return (
    <div className="service-domains-page">
      <BackButton />
      <h1>Domains{service ? ` — ${service.name}` : ''}</h1>

      {domainsError && <p className="service-domains-page__error">{domainsError}</p>}

      {domains.length === 0 ? (
        <p>No domains yet.</p>
      ) : (
        <ul className="service-domains-page__list">
          {domains.map((domain) => (
            <li key={domain.publicUuid} className="service-domains-page__row">
              <span className="service-domains-page__hostname">{domain.hostname}</span>
              {domain.isPrimary && <span className="service-domains-page__primary-badge">Primary</span>}
              <span className="service-domains-page__status">
                Verification: {domain.verificationStatus}
              </span>
              <span className="service-domains-page__status">SSL: {domain.sslStatus}</span>
              {canManage && (
                <button type="button" onClick={() => handleDelete(domain)}>
                  Delete
                </button>
              )}
            </li>
          ))}
        </ul>
      )}

      {canManage && (
        <form className="service-domains-page__form" onSubmit={handleCreate}>
          <h2>Add a domain</h2>
          <label>
            Hostname
            <input
              name="hostname"
              value={form.hostname}
              maxLength={255}
              required
              onChange={handleFormChange}
            />
          </label>
          <label>
            <input
              type="checkbox"
              name="isPrimary"
              checked={form.isPrimary}
              onChange={handleFormChange}
            />
            Set as primary
          </label>
          <button type="submit" disabled={isDomainsLoading}>
            Add domain
          </button>
        </form>
      )}

      {!canManage && (
        <p className="service-domains-page__readonly-notice">
          You don't have permission to change this, or the organization is inactive.
        </p>
      )}
    </div>
  );
}

export default ServiceDomainsPage;