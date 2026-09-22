import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useProject } from '../hooks/useProject';
import { useOrganization } from '../../organization/hooks/useOrganization';
import BackButton from '../../../shared/components/BackButton';

/**
 * GET /api/projects/{publicUuid} doesn't tell us the current user's
 * role directly — only organizationPublicUuid. So, like
 * SubscriptionPage, this page also fetches the organization's members
 * to resolve the current user's role (OWNER-only for update/archive/
 * reactivate, see ProjectService.requireOwner).
 */
function ProjectDetailPage() {
  const { publicUuid } = useParams();
  const { user } = useAuth();
  const { organizations, fetchOrganization, fetchMembers } = useOrganization();
  const {
    isProjectsLoading,
    projectsError,
    fetchProject,
    updateProject,
    reactivateProject,
  } = useProject();

  const [project, setProject] = useState(null);
  const [organization, setOrganization] = useState(null);
  const [members, setMembers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [actionError, setActionError] = useState(null);

  const [form, setForm] = useState({ name: '', slug: '', description: '' });
  const [isEditing, setIsEditing] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setLoadError(null);
      try {
        const loadedProject = await fetchProject(publicUuid);
        const cachedOrg = organizations.find(
          (org) => org.publicUuid === loadedProject.organizationPublicUuid
        );
        const [org, memberList] = await Promise.all([
          cachedOrg
            ? Promise.resolve(cachedOrg)
            : fetchOrganization(loadedProject.organizationPublicUuid),
          fetchMembers(loadedProject.organizationPublicUuid),
        ]);

        if (!cancelled) {
          setProject(loadedProject);
          setOrganization(org);
          setMembers(memberList);
          setForm({
            name: loadedProject.name,
            slug: loadedProject.slug,
            description: loadedProject.description || '',
          });
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(err?.response?.data?.message || 'Failed to load project');
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
  // Mirrors OrganizationDetailPage — update/archive/reactivate are
  // all requireActiveOrganization-gated server-side now.
  const isOrganizationActive = organization?.status !== 'INACTIVE';

  function handleFormChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSave(event) {
    event.preventDefault();
    setActionError(null);
    try {
      // slug is sent but ignored server-side on update — see
      // ProjectRequest typedef and ProjectService.updateProject.
      const updated = await updateProject(publicUuid, form);
      setProject(updated);
      setIsEditing(false);
    } catch (err) {
      setActionError(err?.response?.data?.message || 'Failed to update project');
    }
  }

  async function handleReactivate() {
    setActionError(null);
    try {
      const updated = await reactivateProject(publicUuid);
      setProject(updated);
    } catch (err) {
      setActionError(err?.response?.data?.message || 'Failed to reactivate project');
    }
  }

  if (isLoading) return <p>Loading...</p>;
  if (loadError) return <p className="project-detail-page__error">{loadError}</p>;
  if (!project) return null;

  const displayError = actionError || projectsError;
  const isArchived = project.status === 'ARCHIVED';

  return (
    <div className="project-detail-page">
      <BackButton />
      <h1>{project.name}</h1>
      <span
        className={
          isArchived
            ? 'project-detail-page__status project-detail-page__status--archived'
            : 'project-detail-page__status'
        }
      >
        {project.status}
      </span>

      {displayError && <p className="project-detail-page__error">{displayError}</p>}

      {isEditing ? (
        <form className="project-detail-page__edit-form" onSubmit={handleSave}>
          <label>
            Name
            <input name="name" value={form.name} onChange={handleFormChange} required />
          </label>
          <label>
            Description
            <textarea name="description" value={form.description} onChange={handleFormChange} />
          </label>
          <button type="submit" disabled={isProjectsLoading}>
            Save
          </button>
          <button type="button" onClick={() => setIsEditing(false)}>
            Cancel
          </button>
        </form>
      ) : (
        <>
          <p className="project-detail-page__slug">{project.slug}</p>
          {project.description && (
            <p className="project-detail-page__description">{project.description}</p>
          )}

          {isOwner && !isArchived && isOrganizationActive && (
            <div className="project-detail-page__actions">
              <button type="button" onClick={() => setIsEditing(true)}>
                Edit
              </button>
            </div>
          )}

          {isOwner && isArchived && isOrganizationActive && (
            <button type="button" disabled={isProjectsLoading} onClick={handleReactivate}>
              Reactivate
            </button>
          )}

          {!isOrganizationActive && (
            <p className="project-detail-page__inactive-notice">
              This project&apos;s organization is inactive — no changes are allowed until it&apos;s
              reactivated.
            </p>
          )}
        </>
      )}

      {isOrganizationActive ? (
        <Link
          to={`/organizations/${project.organizationPublicUuid}/git-connections`}
          className="project-detail-page__git-link"
        >
          Manage Git connections for this organization
        </Link>
      ) : (
        <p className="project-detail-page__git-link-locked">
          Git connections are unavailable while this organization is inactive.
        </p>
      )}
    </div>
  );
}

export default ProjectDetailPage;