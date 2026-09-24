/**
 * Presentational card for one git connection. Mirrors ProjectCard's
 * shape — no API calls or useProject() here, the owning page
 * (GitConnectionsPage) owns the fetch and decides what "revoke" does
 * next.
 *
 * `canManage` is the same OWNER-role gate as ProjectCard (revoke is
 * OWNER-only backend-side, see ProjectService.requireOwner).
 *
 * The "manual entry, no real OAuth yet" notice is shown here rather
 * than only on the create form, since a viewer of an EXISTING
 * connection should also understand why externalAccountId/Name look
 * like free text instead of data pulled from GitHub/GitLab/Bitbucket.
 *
 * @param {{
 *   connection: import('../types/project.types').GitConnectionResponse,
 *   canManage?: boolean,
 *   onRevoke?: (connection: import('../types/project.types').GitConnectionResponse) => void,
 *   isBusy?: boolean,
 * }} props
 */
function GitConnectionCard({ connection, canManage = true, onRevoke, isBusy = false }) {
  const isRevoked = connection.status === 'REVOKED';

  return (
    <div className="git-connection-card">
      <div className="git-connection-card__header">
        <h3 className="git-connection-card__provider">{connection.gitProviderCode}</h3>
        <span
          className={
            isRevoked
              ? 'git-connection-card__status git-connection-card__status--revoked'
              : 'git-connection-card__status'
          }
        >
          {connection.status}
        </span>
      </div>

      <dl className="git-connection-card__details">
        <dt>Account ID</dt>
        <dd>{connection.externalAccountId || '—'}</dd>

        <dt>Account name</dt>
        <dd>{connection.externalAccountName || '—'}</dd>
      </dl>

      <span
        className={
          connection.hasAccessToken
            ? 'git-connection-card__access-badge git-connection-card__access-badge--configured'
            : 'git-connection-card__access-badge git-connection-card__access-badge--not-configured'
        }
      >
        {connection.hasAccessToken ? 'API access configured' : 'No API access token'}
      </span>

      {/* Always visible, not just on the create form — explains why
          this looks like manually-typed text instead of data fetched
          from the actual provider. */}
      <p className="git-connection-card__manual-notice">
        Connected manually — automatic sign-in with your Git provider isn&apos;t available yet.
      </p>

      {canManage && !isRevoked && (
        <button
          type="button"
          className="git-connection-card__revoke"
          disabled={isBusy}
          onClick={() => onRevoke?.(connection)}
        >
          Revoke
        </button>
      )}
    </div>
  );
}

export default GitConnectionCard;