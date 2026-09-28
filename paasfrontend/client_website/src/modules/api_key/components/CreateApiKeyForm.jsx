import { useState } from 'react';

/**
 * Create form + one-time reveal of the raw key.
 *
 * SECURITY: the rawKey exists ONLY in this component's local state
 * (`created`). Not in context, localStorage, hook state or logs. It is
 * cleared when the user dismisses the panel or the component unmounts.
 * While the panel is open the form is hidden, so it can't be dismissed
 * by accident by creating a second key.
 *
 * @param {{ onCreate: (data: import('../types/apiKey.types').ApiKeyRequest) => Promise<import('../types/apiKey.types').ApiKeyCreated>, onConflict?: () => Promise<boolean>, disabled?: boolean }} props
 *   `onConflict` is called on a 409; it should refresh the organization and
 *   return true if the org is still writable (=> the 409 was a duplicate name).
 */
function CreateApiKeyForm({ onCreate, onConflict, disabled = false }) {
  const [name, setName] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [created, setCreated] = useState(null);
  const [copied, setCopied] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);

    let expiresIso;
    if (expiresAt) {
      const date = new Date(expiresAt);
      if (Number.isNaN(date.getTime()) || date <= new Date()) {
        setError('Expiration must be a date in the future.');
        return;
      }
      expiresIso = date.toISOString();
    }

    setIsSubmitting(true);
    try {
      const result = await onCreate({
        name: name.trim(),
        ...(expiresIso ? { expiresAt: expiresIso } : {}),
      });
      setCreated(result);
      setName('');
      setExpiresAt('');
    } catch (err) {
      if (err?.response?.status === 409) {
        // 409 = duplicate name OR org became INACTIVE/SUSPENDED. Decide by
        // the (refreshed) organization status, never by the message.
        const stillWritable = (await onConflict?.()) ?? true;
        setError(
          stillWritable
            ? 'An API key with this name already exists in this organization.'
            : 'This organization can no longer be modified.'
        );
      } else {
        setError(err?.response?.data?.message || 'Failed to create API key');
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(created.rawKey);
      setCopied(true);
    } catch {
      setCopied(false);
      setError('Could not copy automatically — select the key and copy it manually.');
    }
  }

  function handleDismiss() {
    setCreated(null);
    setCopied(false);
  }

  if (created) {
    return (
      <div className="create-api-key-form__reveal" role="alert">
        <h3>Copy your API key now</h3>
        <p>
          This is the only time the full key will be shown. It can't be
          retrieved later — if you lose it, revoke it and create a new one.
        </p>
        <input type="text" readOnly value={created.rawKey} onFocus={(e) => e.target.select()} />
        <button type="button" onClick={handleCopy}>
          {copied ? 'Copied' : 'Copy'}
        </button>
        {error && <p className="create-api-key-form__error">{error}</p>}
        <button type="button" onClick={handleDismiss}>
          I've saved it — close
        </button>
      </div>
    );
  }

  return (
    <form className="create-api-key-form" onSubmit={handleSubmit}>
      <div className="create-api-key-form__field">
        <label htmlFor="api-key-name">Name</label>
        <input
          id="api-key-name"
          type="text"
          value={name}
          maxLength={120}
          required
          disabled={disabled}
          onChange={(event) => setName(event.target.value)}
        />
      </div>

      <div className="create-api-key-form__field">
        <label htmlFor="api-key-expires">Expires at (optional)</label>
        <input
          id="api-key-expires"
          type="datetime-local"
          value={expiresAt}
          disabled={disabled}
          onChange={(event) => setExpiresAt(event.target.value)}
        />
        <p className="create-api-key-form__hint">Leave empty for a key that never expires.</p>
      </div>

      {error && <p className="create-api-key-form__error">{error}</p>}

      <button type="submit" disabled={disabled || isSubmitting || name.trim() === ''}>
        Create API key
      </button>
    </form>
  );
}

export default CreateApiKeyForm;