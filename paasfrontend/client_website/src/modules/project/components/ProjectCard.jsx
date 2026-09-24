/**
 * Presentational card for one project. Doesn't call useProject() or
 * any API itself — the owning page (ProjectsListPage) fetches the
 * list and passes data + callbacks down. Mirrors PlanCard's shape
 * exactly.
 *
 * `canManage` is an authorization gate the page computes (OWNER role
 * — update/archive/reactivate are OWNER-only backend-side, see
 * ProjectService.requireOwner). When false, edit/archive/reactivate
 * controls are hidden entirely rather than shown-then-403 on click —
 * same reasoning as PlanCard's canSubscribe and
 * SubscriptionSummary's canCancel.
 *
 * @param {{
 *   project: import('../types/project.types').ProjectResponse,
 *   canManage?: boolean,
 *   onOpen?: (project: import('../types/project.types').ProjectResponse) => void,
 *   onArchive?: (project: import('../types/project.types').ProjectResponse) => void,
 *   onReactivate?: (project: import('../types/project.types').ProjectResponse) => void,
 *   isBusy?: boolean,
 * }} props
 */
function ProjectCard({
  project,
  canManage = true,
  onOpen,
  onArchive,
  onReactivate,
  isBusy = false,
}) {
  const isArchived = project.status === 'ARCHIVED';

  return (
    <div className="project-card">
      <div className="project-card__header">
        <h3 className="project-card__name">{project.name}</h3>
        <span
          className={
            isArchived ? 'project-card__status project-card__status--archived' : 'project-card__status'
          }
        >
          {project.status}
        </span>
      </div>

      <p className="project-card__slug">{project.slug}</p>

      {project.description && <p className="project-card__description">{project.description}</p>}

      <div className="project-card__actions">
        <button
          type="button"
          className="project-card__open"
          disabled={isArchived}
          onClick={() => onOpen?.(project)}
        >
          Open
        </button>

        {/* Edit is reached via the detail page (onOpen), not a
            separate button here — mirrors how PlanCard doesn't offer
            inline editing either. Only the archive/reactivate toggle
            lives on the card itself. */}
        {canManage && !isArchived && (
          <button
            type="button"
            className="project-card__archive"
            disabled={isBusy}
            onClick={() => onArchive?.(project)}
          >
            Archive
          </button>
        )}

        {canManage && isArchived && (
          <button
            type="button"
            className="project-card__reactivate"
            disabled={isBusy}
            onClick={() => onReactivate?.(project)}
          >
            Reactivate
          </button>
        )}
      </div>
    </div>
  );
}

export default ProjectCard;