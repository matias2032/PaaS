import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useOrganization } from '../../organization/hooks/useOrganization';
import { useService } from '../hooks/useService';
import BackButton from '../../../shared/components/BackButton';

const DEFAULT_FORM = {
  rootDirectory: '/',
  buildCommand: '',
  startCommand: '',
  dockerfilePath: '',
  healthCheckPath: '',
  port: '',
};

/**
 * GET returns 404 when never configured — treated as the default
 * empty form (rootDirectory "/"), not an error banner. All fields
 * are optional server-side.
 */
function ServiceBuildConfigPage() {
  const { publicUuid } = useParams();
  const { user } = useAuth();
  const { organizations, fetchOrganization, fetchMembers } = useOrganization();
  const {
    isBuildConfigLoading,
    buildConfigError,
    fetchService,
    fetchServiceBuildConfig,
    upsertServiceBuildConfig,
  } = useService();

  const [service, setService] = useState(null);
  const [organization, setOrganization] = useState(null);
  const [members, setMembers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [form, setForm] = useState(DEFAULT_FORM);

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
          existing = await fetchServiceBuildConfig(publicUuid);
        } catch (err) {
          if (err?.response?.status !== 404) throw err;
        }

        if (!cancelled) {
          setService(loadedService);
          setOrganization(org);
          setMembers(memberList);
          if (existing) {
            setForm({
              rootDirectory: existing.rootDirectory || '/',
              buildCommand: existing.buildCommand || '',
              startCommand: existing.startCommand || '',
              dockerfilePath: existing.dockerfilePath || '',
              healthCheckPath: existing.healthCheckPath || '',
              port: existing.port ?? '',
            });
          }
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(err?.response?.data?.message || 'Failed to load build config');
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
      await upsertServiceBuildConfig(publicUuid, {
        ...form,
        port: form.port === '' ? undefined : Number(form.port),
      });
    } catch {
      // Surfaced via context `buildConfigError` already.
    }
  }

  if (isLoading) return <p>Loading...</p>;
  if (loadError) return <p className="service-build-config-page__error">{loadError}</p>;

  return (
    <div className="service-build-config-page">
      <BackButton />
      <h1>Build config{service ? ` — ${service.name}` : ''}</h1>

      {buildConfigError && <p className="service-build-config-page__error">{buildConfigError}</p>}

      <form className="service-build-config-page__form" onSubmit={handleSave}>
        <label>
          Root directory
          <input
            name="rootDirectory"
            value={form.rootDirectory}
            disabled={!canManage}
            onChange={handleFormChange}
          />
        </label>
        <label>
          Build command
          <input
            name="buildCommand"
            value={form.buildCommand}
            disabled={!canManage}
            onChange={handleFormChange}
          />
        </label>
        <label>
          Start command
          <input
            name="startCommand"
            value={form.startCommand}
            disabled={!canManage}
            onChange={handleFormChange}
          />
        </label>
        <label>
          Dockerfile path
          <input
            name="dockerfilePath"
            value={form.dockerfilePath}
            disabled={!canManage}
            onChange={handleFormChange}
          />
        </label>
        <label>
          Health check path
          <input
            name="healthCheckPath"
            value={form.healthCheckPath}
            disabled={!canManage}
            onChange={handleFormChange}
          />
        </label>
        <label>
          Port
          <input
            name="port"
            type="number"
            min={1}
            max={65535}
            value={form.port}
            disabled={!canManage}
            onChange={handleFormChange}
          />
        </label>

        {canManage && (
          <button type="submit" disabled={isBuildConfigLoading}>
            Save
          </button>
        )}

        {!canManage && (
          <p className="service-build-config-page__readonly-notice">
            You don't have permission to change this, or the organization is inactive.
          </p>
        )}
      </form>
    </div>
  );
}

export default ServiceBuildConfigPage;