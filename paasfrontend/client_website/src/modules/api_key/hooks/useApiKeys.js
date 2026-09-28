import { useCallback, useState } from 'react';
import {
  listApiKeys as listApiKeysApi,
  createApiKey as createApiKeyApi,
  revokeApiKey as revokeApiKeyApi,
} from '../api/apiKeyApi';

/**
 * Same "page owns its state" idea as members in OrganizationDetailPage,
 * packaged as a hook. Mutations re-throw so the calling component can
 * show its own message; only the load error lives here.
 *
 * SECURITY: `create` returns the full ApiKeyCreated (with rawKey) to the
 * caller only. The copy stored in `keys` has rawKey stripped, so the
 * secret never lives in hook state.
 */
export function useApiKeys(orgUuid) {
  const [keys, setKeys] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [loadError, setLoadError] = useState(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setLoadError(null);
    try {
      const list = await listApiKeysApi(orgUuid);
      setKeys(list);
      return list;
    } catch (err) {
      setLoadError(err?.response?.data?.message || 'Failed to load API keys');
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, [orgUuid]);

  const create = useCallback(
    async (data) => {
      const created = await createApiKeyApi(orgUuid, data);
      // eslint-disable-next-line no-unused-vars
      const { rawKey, ...safe } = created;
      setKeys((prev) => [
        { ...safe, organizationPublicUuid: orgUuid, lastUsedAt: null, revocationReason: null },
        ...prev,
      ]);
      return created;
    },
    [orgUuid]
  );

  const revoke = useCallback(
    async (keyUuid) => {
      const updated = await revokeApiKeyApi(orgUuid, keyUuid);
      setKeys((prev) => prev.map((k) => (k.publicUuid === keyUuid ? updated : k)));
      return updated;
    },
    [orgUuid]
  );

  return { keys, isLoading, loadError, load, create, revoke };
}

/**
 * "Expired" is derived client-side: the backend keeps an expired key as
 * ACTIVE. Returns "ACTIVE" | "EXPIRED" | "REVOKED".
 */
export function getEffectiveStatus(apiKey) {
  if (apiKey.status === 'REVOKED') return 'REVOKED';
  if (apiKey.expiresAt && new Date(apiKey.expiresAt) <= new Date()) return 'EXPIRED';
  return 'ACTIVE';
}