import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useProject } from '../hooks/useProject';
import { useOrganization } from '../../organization/hooks/useOrganization';
import ProjectCard from '../components/ProjectCard';
import BackButton from '../../../shared/components/BackButton';

function ProjectsListPage() {
  const { orgPublicUuid } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const { organizations, fetchOrganization, fetchMembers } = useOrganization();
  const {
    isProjectsLoading,
    projectsError,
    fetchProjects,
    archiveProject,
    reactivateProject,
  } = useProject();

  const [organization, setOrganization] = useState(
    () => organizations.find((org) => org.publicUuid === orgPublicUuid) || null
  );
  const [projects, setProjects] = useState([]);
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
        const cachedOrg = organizations.find((org) => org.publicUuid === orgPublicUuid);
        const [org, projectList, memberList] = await Promise.all([
          cachedOrg ? Promise.resolve(cachedOrg) : fetchOrganization(orgPublicUuid),
          fetchProjects(orgPublicUuid),
          fetchMembers(orgPublicUuid),
        ]);

        if (!cancelled) {
          setOrganization(org);
          setProjects(projectList);
          setMembers(memberList);
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(err?.response?.data?.message || 'Failed to load projects');
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
  }, [orgPublicUuid]);

  const currentMembership = members.find(
    (member) => member.userPublicUuid === user?.publicUuid
  );
  const isOwner = currentMembership?.roleCode === 'OWNER';
  // Mirrors OrganizationDetailPage — createProject/archive/reactivate
  // are all requireActiveOrganization-gated server-side now.
  const isOrganizationActive = organization?.status !== 'INACTIVE';

  async function handleArchive(project) {
    setActionError(null);
    try {
      const updated = await archiveProject(project.publicUuid);
      setProjects((prev) => prev.map((p) => (p.publicUuid === updated.publicUuid ? updated : p)));
    } catch (err) {
      setActionError(err?.response?.data?.message || 'Failed to archive project');
    }
  }

  async function handleReactivate(project) {
    setActionError(null);
    try {
      const updated = await reactivateProject(project.publicUuid);
      setProjects((prev) => prev.map((p) => (p.publicUuid === updated.publicUuid ? updated : p)));
    } catch (err) {
      setActionError(err?.response?.data?.message || 'Failed to reactivate project');
    }
  }

  function handleOpen(project) {
    navigate(`/projects/${project.publicUuid}`);
  }

  if (isLoading) return <p>Loading...</p>;
  if (loadError) return <p className="projects-list-page__error">{loadError}</p>;

  const displayError = actionError || projectsError;

  return (
    <div className="projects-list-page">
      <header className="projects-list-page__header">
        <BackButton />
        <h1>Projects{organization ? ` for ${organization.name}` : ''}</h1>
        {isOwner && isOrganizationActive && (
          <Link
            to={`/organizations/${orgPublicUuid}/projects/new`}
            className="projects-list-page__create-link"
          >
            New project
          </Link>
        )}
      </header>

      {displayError && <p className="projects-list-page__error">{displayError}</p>}

      {isOwner && !isOrganizationActive && (
        <p className="projects-list-page__inactive-notice">
          This organization is inactive — no new projects can be created until it's reactivated.
        </p>
      )}

      {projects.length === 0 ? (
        <p>No projects yet.</p>
      ) : (
        <div className="projects-list-page__grid">
          {projects.map((project) => (
            <ProjectCard
              key={project.publicUuid}
              project={project}
              canManage={isOwner && isOrganizationActive}
              isBusy={isProjectsLoading}
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

export default ProjectsListPage;