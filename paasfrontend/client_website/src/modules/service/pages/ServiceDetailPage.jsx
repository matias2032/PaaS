import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useService } from '../hooks/useService';
import { useOrganization } from '../../organization/hooks/useOrganization';
import BackButton from '../../../shared/components/BackButton';

/**
 * Unlike ProjectDetailPage (which has to fetch the project's
 * organization by organizationPublicUuid off the project itself),
 * ServiceResponse now carries organizationPublicUuid directly
 * (Option B, decided when this module started) — so this page
 * resolves the organization in one hop, not via Project at all.
 *
 * Acts as a hub: shows name/slug/status/serviceTypeName/autoDeploy,
 * inline edit (name + autoDeploy only — slug and serviceTypeCode are
 * immutable after creation), a reactivate action when archived (no
 * archive button here, same as ProjectDetailPage — archiving happens
 * from the card on ServicesListPage), and links to the 6 sub-resource
 * pages (repository, build-config, resource-config,
 * environment-variables, domains, deployments).
 */
function ServiceDetailPage() {
  const { publicUuid } = useParams();
  const { user } = useAuth();
  const { organizations, fetchOrganization, fetchMembers } = useOrganization();
  const { isServicesLoading, servicesError, fetchService, updateService, reactivateService } =
    useService();

  const [service, setService] = useState(null);
  const [organization, setOrganization] = useState(null);
  const [members, setMembers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [actionError, setActionError] = useState(null);

  const [form, setForm] = useState({ name: '', autoDeploy: false });
  const [isEditing, setIsEditing] = useState(false);

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

        if (!cancelled) {
          setService(loadedService);
          setOrganization(org);
          setMembers(memberList);
          setForm({ name: loadedService.name, autoDeploy: loadedService.autoDeploy });
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(err?.response?.data?.message || 'Failed to load service');
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

  function handleFormChange(event) {
    const { name, value, type, checked } = event.target;
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  }

  async function handleSave(event) {
    event.preventDefault();
    setActionError(null);
    try {
      // slug and serviceTypeCode are sent but ignored/unused
      // server-side on update — see ServiceRequest typedef and
      // ServiceService.updateService — but included here since the
      // request DTO shape is shared with create.
      const updated = await updateService(publicUuid, {
        name: form.name,
        slug: service.slug,
        serviceTypeCode: service.serviceTypeCode,
        autoDeploy: form.autoDeploy,
      });
      setService(updated);
      setIsEditing(false);
    } catch (err) {
      setActionError(err?.response?.data?.message || 'Failed to update service');
    }
  }

  async function handleReactivate() {
    setActionError(null);
    try {
      const updated = await reactivateService(publicUuid);
      setService(updated);
    } catch (err) {
      setActionError(err?.response?.data?.message || 'Failed to reactivate service');
    }
  }

  if (isLoading) return <p>Loading...</p>;
  if (loadError) return <p className="service-detail-page__error">{loadError}</p>;
  if (!service) return null;

  const displayError = actionError || servicesError;
  const isArchived = service.status === 'ARCHIVED';
  const isCreated = service.status === 'CREATED';

  return (
    <div className="service-detail-page">
      <BackButton />
      <h1>{service.name}</h1>
      <span
        className={
          isArchived
            ? 'service-detail-page__status service-detail-page__status--archived'
            : 'service-detail-page__status'
        }
      >
        {service.status}
      </span>

      {displayError && <p className="service-detail-page__error">{displayError}</p>}

      {isEditing ? (
        <form className="service-detail-page__edit-form" onSubmit={handleSave}>
          <label>
            Name
            <input name="name" value={form.name} onChange={handleFormChange} required />
          </label>
          <label>
            <input
              type="checkbox"
              name="autoDeploy"
              checked={form.autoDeploy}
              onChange={handleFormChange}
            />
            Auto-deploy
          </label>
          <button type="submit" disabled={isServicesLoading}>
            Save
          </button>
          <button type="button" onClick={() => setIsEditing(false)}>
            Cancel
          </button>
        </form>
      ) : (
        <>
          <p className="service-detail-page__slug">{service.slug}</p>
          <p className="service-detail-page__type">{service.serviceTypeName}</p>
          <p className="service-detail-page__auto-deploy">
            Auto-deploy: {service.autoDeploy ? 'on' : 'off'}
          </p>

          {isOwner && isCreated && isOrganizationActive && (
            <div className="service-detail-page__actions">
              <button type="button" onClick={() => setIsEditing(true)}>
                Edit
              </button>
            </div>
          )}

          {isOwner && isArchived && isOrganizationActive && (
            <button type="button" disabled={isServicesLoading} onClick={handleReactivate}>
              Reactivate
            </button>
          )}

          {!isOrganizationActive && (
            <p className="service-detail-page__inactive-notice">
              This service's organization is inactive — no changes are allowed until it's
              reactivated.
            </p>
          )}
        </>
      )}

      {/* Sub-resource navigation — the hub's main job. Always shown
          (reads are allowed regardless of organization status), even
          for the 5 reserved statuses, since a viewer may still want
          to see e.g. previously logged deployments. */}
      <nav className="service-detail-page__sub-nav">
        <Link to={`/services/${publicUuid}/repository`}>Repository</Link>
        <Link to={`/services/${publicUuid}/build-config`}>Build config</Link>
        <Link to={`/services/${publicUuid}/resource-config`}>Resource config</Link>
        <Link to={`/services/${publicUuid}/environment-variables`}>Environment variables</Link>
        <Link to={`/services/${publicUuid}/domains`}>Domains</Link>
        <Link to={`/services/${publicUuid}/deployments`}>Deployments</Link>
      </nav>
    </div>
  );
}

export default ServiceDetailPage;