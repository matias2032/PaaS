/**
 * Presentational card for one service. Doesn't call useService() or
 * any API itself — the owning page (ServicesListPage) fetches the
 * list and passes data + callbacks down. Mirrors ProjectCard's
 * shape, with two differences: it also shows serviceTypeName, and
 * its status has 7 possible values instead of 2.
 *
 * `canManage` is an authorization gate the page computes (OWNER role
 * — update/archive/reactivate are OWNER-only backend-side, see
 * handoff section 3.9). When false, edit/archive/reactivate controls
 * are hidden entirely rather than shown-then-403 on click — same
 * reasoning as ProjectCard's canManage.
 *
 * Service.status: CREATED | PROVISIONING | RUNNING | STOPPED |
 * FAILED | SUSPENDED | ARCHIVED. Only CREATED and ARCHIVED are
 * reachable via the API for now (no real Coolify yet) — the archive/
 * reactivate toggle only renders for those two. The other 5 values
 * are rendered as a plain status badge with no actions attached,
 * since neither archive nor reactivate applies to them yet; this
 * avoids the card silently doing nothing on click for a status it
 * doesn't actually know how to act on.
 *
 * @param {{
 *   service: import('../types/service.types').ServiceResponse,
 *   canManage?: boolean,
 *   onOpen?: (service: import('../types/service.types').ServiceResponse) => void,
 *   onArchive?: (service: import('../types/service.types').ServiceResponse) => void,
 *   onReactivate?: (service: import('../types/service.types').ServiceResponse) => void,
 *   isBusy?: boolean,
 * }} props
 */
function ServiceCard({
  service,
  canManage = true,
  onOpen,
  onArchive,
  onReactivate,
  isBusy = false,
}) {
  const isArchived = service.status === 'ARCHIVED';
  const isCreated = service.status === 'CREATED';

  return (
    <div className="service-card">
      <div className="service-card__header">
        <h3 className="service-card__name">{service.name}</h3>
        <span
          className={
            isArchived
              ? 'service-card__status service-card__status--archived'
              : 'service-card__status'
          }
        >
          {service.status}
        </span>
      </div>

      <p className="service-card__type">{service.serviceTypeName}</p>
      <p className="service-card__slug">{service.slug}</p>

      {service.autoDeploy && (
        <span className="service-card__auto-deploy-badge">Auto-deploy on</span>
      )}

      <div className="service-card__actions">
        <button
          type="button"
          className="service-card__open"
          disabled={isArchived}
          onClick={() => onOpen?.(service)}
        >
          Open
        </button>

        {/* Edit is reached via the detail page (onOpen), not a
            separate button here — mirrors ProjectCard. Only the
            archive/reactivate toggle lives on the card itself, and
            only for the two statuses that actually support it. */}
        {canManage && isCreated && (
          <button
            type="button"
            className="service-card__archive"
            disabled={isBusy}
            onClick={() => onArchive?.(service)}
          >
            Archive
          </button>
        )}

        {canManage && isArchived && (
          <button
            type="button"
            className="service-card__reactivate"
            disabled={isBusy}
            onClick={() => onReactivate?.(service)}
          >
            Reactivate
          </button>
        )}
      </div>
    </div>
  );
}

export default ServiceCard;