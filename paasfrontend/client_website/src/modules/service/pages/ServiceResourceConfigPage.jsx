import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useOrganization } from '../../organization/hooks/useOrganization';
import { useService } from '../hooks/useService';
import BackButton from '../../../shared/components/BackButton';

/**
 * GET returns 404 when never configured — treated as an empty form.
 * Unlike build-config, all three fields are required server-side
 * (@NotNull) — the form can't submit with any of them blank.
 *
 * Not validated against PlanResourceLimit yet (BILLING not wired
 * in) — intentionally no "within your plan" copy anywhere here, per
 * handoff section 3.5, since that would be misleading right now.
 */
function ServiceResourceConfigPage() {
  const { publicUuid } = useParams();
  const { user } = useAuth();
  const { organizations, fetchOrganization, fetchMembers } = useOrganization();
  const {
    isResourceConfigLoading,
    resourceConfigError,
    fetchService,
    fetchServiceResourceConfig,
    upsertServiceResourceConfig,
  } = useService();

  const [service, setService] = useState(null);
  const [organization, setOrganization] = useState(null);
  const [members, setMembers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [form, setForm] = useState({ cpuLimit: '', memoryLimitMb: '', storageLimitMb: '' });

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
        const [org, memberList] = await Promise.all([
          cachedOrg
            ? Promise.resolve(cachedOrg)
            : fetchOrganization(loadedService.organizationPublicUuid),
          fetchMembers(loadedService.organizationPublicUuid),
        ]);

        let existing = null;
        try {
          existing = await fetchServiceResourceConfig(publicUuid);
        } catch (err) {
          if (err?.response?.status !== 404) throw err;
        }

        if (!cancelled) {
          setService(loadedService);
          setOrganization(org);
          setMembers(memberList);
          if (existing) {
            setForm({
              cpuLimit: existing.cpuLimit,
              memoryLimitMb: existing.memoryLimitMb,
              storageLimitMb: existing.storageLimitMb,
            });
          }
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(err?.response?.data?.message || 'Failed to load resource config');
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
      await upsertServiceResourceConfig(publicUuid, {
        cpuLimit: Number(form.cpuLimit),
        memoryLimitMb: Number(form.memoryLimitMb),
        storageLimitMb: Number(form.storageLimitMb),
      });
    } catch {
      // Surfaced via context `resourceConfigError` already.
    }
  }

  if (isLoading) return <p>Loading...</p>;
  if (loadError) return <p className="service-resource-config-page__error">{loadError}</p>;

  const isFormValid = form.cpuLimit !== '' && form.memoryLimitMb !== '' && form.storageLimitMb !== '';

  return (
    <div className="service-resource-config-page">
      <BackButton />
      <h1>Resource config{service ? ` — ${service.name}` : ''}</h1>

      {resourceConfigError && (
        <p className="service-resource-config-page__error">{resourceConfigError}</p>
      )}

      <form className="service-resource-config-page__form" onSubmit={handleSave}>
        <label>
          CPU limit (cores)
          <input
            name="cpuLimit"
            type="number"
            step="0.01"
            min="0.01"
            value={form.cpuLimit}
            required
            disabled={!canManage}
            onChange={handleFormChange}
          />
        </label>
        <label>
          Memory limit (MB)
          <input
            name="memoryLimitMb"
            type="number"
            min="1"
            value={form.memoryLimitMb}
            required
            disabled={!canManage}
            onChange={handleFormChange}
          />
        </label>
        <label>
          Storage limit (MB)
          <input
            name="storageLimitMb"
            type="number"
            min="1"
            value={form.storageLimitMb}
            required
            disabled={!canManage}
            onChange={handleFormChange}
          />
        </label>

        {canManage && (
          <button type="submit" disabled={isResourceConfigLoading || !isFormValid}>
            Save
          </button>
        )}

        {!canManage && (
          <p className="service-resource-config-page__readonly-notice">
            You don't have permission to change this, or the organization is inactive.
          </p>
        )}
      </form>
    </div>
  );
}

export default ServiceResourceConfigPage;