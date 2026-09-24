import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useProject } from '../../project/hooks/useProject';
import { useOrganization } from '../../organization/hooks/useOrganization';
import { useService } from '../hooks/useService';
import ServiceCard from '../components/ServiceCard';
import BackButton from '../../../shared/components/BackButton';

/**
 * Unlike ProjectsListPage (nested under an organization, so
 * orgPublicUuid is already in the URL), ServicesListPage is nested
 * under a project — the URL only has projectPublicUuid. So this page
 * still needs the Project -> Organization hop via useProject().
 * fetchProject() to resolve organizationPublicUuid before it can
 * compute isOwner. ServiceDetailPage doesn't need this extra hop,
 * since ServiceResponse carries organizationPublicUuid directly.
 */
function ServicesListPage() {
  const { projectPublicUuid } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const { fetchProject } = useProject();
  const { organizations, fetchOrganization, fetchMembers } = useOrganization();
  const { isServicesLoading, servicesError, fetchServices, archiveService, reactivateService } =
    useService();

  const [project, setProject] = useState(null);
  const [organization, setOrganization] = useState(null);
  const [services, setServices] = useState([]);
  const [members, setMembers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [actionError, setActionError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setLoadError(null);
      try {
        const loadedProject = await fetchProject(projectPublicUuid);
        const cachedOrg = organizations.find(
          (org) => org.publicUuid === loadedProject.organizationPublicUuid
        );
        const [org, serviceList, memberList] = await Promise.all([
          cachedOrg
            ? Promise.resolve(cachedOrg)
            : fetchOrganization(loadedProject.organizationPublicUuid),
          fetchServices(projectPublicUuid),
          fetchMembers(loadedProject.organizationPublicUuid),
        ]);

        if (!cancelled) {
          setProject(loadedProject);
          setOrganization(org);
          setServices(serviceList);
          setMembers(memberList);
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(err?.response?.data?.message || 'Failed to load services');
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
  }, [projectPublicUuid]);

  const currentMembership = members.find(
    (member) => member.userPublicUuid === user?.publicUuid
  );
  const isOwner = currentMembership?.roleCode === 'OWNER';
  // Mirrors ProjectsListPage — createService/archive/reactivate are
  // all blocked (409) while the organization is INACTIVE.
  const isOrganizationActive = organization?.status !== 'INACTIVE';

  async function handleArchive(service) {
    setActionError(null);
    try {
      const updated = await archiveService(service.publicUuid);
      setServices((prev) => prev.map((s) => (s.publicUuid === updated.publicUuid ? updated : s)));
    } catch (err) {
      setActionError(err?.response?.data?.message || 'Failed to archive service');
    }
  }

  async function handleReactivate(service) {
    setActionError(null);
    try {
      const updated = await reactivateService(service.publicUuid);
      setServices((prev) => prev.map((s) => (s.publicUuid === updated.publicUuid ? updated : s)));
    } catch (err) {
      setActionError(err?.response?.data?.message || 'Failed to reactivate service');
    }
  }

  function handleOpen(service) {
    navigate(`/services/${service.publicUuid}`);
  }

  if (isLoading) return <p>Loading...</p>;
  if (loadError) return <p className="services-list-page__error">{loadError}</p>;

  const displayError = actionError || servicesError;

  return (
    <div className="services-list-page">
      <header className="services-list-page__header">
        <BackButton />
        <h1>Services{project ? ` for ${project.name}` : ''}</h1>
        {isOwner && isOrganizationActive && (
          <Link
            to={`/projects/${projectPublicUuid}/services/new`}
            className="services-list-page__create-link"
          >
            New service
          </Link>
        )}
      </header>

      {displayError && <p className="services-list-page__error">{displayError}</p>}

      {isOwner && !isOrganizationActive && (
        <p className="services-list-page__inactive-notice">
          This organization is inactive — no new services can be created until it's
          reactivated.
        </p>
      )}

      {services.length === 0 ? (
        <p>No services yet.</p>
      ) : (
        <div className="services-list-page__grid">
          {services.map((service) => (
            <ServiceCard
              key={service.publicUuid}
              service={service}
              canManage={isOwner && isOrganizationActive}
              isBusy={isServicesLoading}
              onOpen={handleOpen}
              onArchive={handleArchive}
              onReactivate={handleReactivate}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export default ServicesListPage;