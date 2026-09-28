import { getEffectiveStatus } from '../hooks/useApiKeys';

function formatDate(value) {
  return value ? new Date(value).toLocaleString() : 'Never';
}

/**
 * Presentational list. `canRevoke` is decided by the page (OWNER +
 * organization writable). Revoke is offered only for keys that aren't
 * already REVOKED (the backend rejects revoking twice).
 *
 * @param {{ keys: import('../types/apiKey.types').ApiKey[], canRevoke?: boolean, onRevoke?: (key: import('../types/apiKey.types').ApiKey) => void }} props
 */
function ApiKeyList({ keys, canRevoke = false, onRevoke }) {
  if (keys.length === 0) {
    return <p className="api-key-list__empty">No API keys yet.</p>;
  }

  return (
    <ul className="api-key-list">
      {keys.map((key) => {
        const status = getEffectiveStatus(key);
        return (
          <li key={key.publicUuid} className="api-key-list__item">
            <span className="api-key-list__name">{key.name}</span>
            <code className="api-key-list__prefix">{key.keyPrefix}…</code>
            <span className={`api-key-list__status api-key-list__status--${status.toLowerCase()}`}>
              {status}
            </span>
            <span className="api-key-list__meta">Created: {formatDate(key.createdAt)}</span>
            <span className="api-key-list__meta">Last used: {formatDate(key.lastUsedAt)}</span>
            <span className="api-key-list__meta">Expires: {formatDate(key.expiresAt)}</span>
            {key.revocationReason && (
              <span className="api-key-list__meta">Revoked by platform: {key.revocationReason}</span>
            )}
            {canRevoke && key.status !== 'REVOKED' && (
              <button
                type="button"
                className="api-key-list__revoke"
                onClick={() => onRevoke?.(key)}
              >
                Revoke
              </button>
            )}
          </li>
        );
      })}
    </ul>
  );
}

export default ApiKeyList;