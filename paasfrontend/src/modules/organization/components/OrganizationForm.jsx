import { useState } from 'react';
import { useOrganization } from '../hooks/useOrganization';

/**
 * Shared form for both creating and editing an organization — same
 * shape as OrganizationRequestDTO (name + slug) either way.
 *
 * Mode is inferred from the `organization` prop: undefined/null = create,
 * an OrganizationResponse = edit. This mirrors the DTO comment in
 * OrganizationRequestDTO.java ("Used for both create and update").
 *
 * @param {{ organization?: import('../types/organization.types').OrganizationResponse, onSuccess?: (org: import('../types/organization.types').OrganizationResponse) => void }} props
 */
function OrganizationForm({ organization, onSuccess }) {
  const isEditMode = !!organization;
  const { createOrganization, updateOrganization, isLoading, error } = useOrganization();

  const [name, setName] = useState(organization?.name ?? '');
  const [slug, setSlug] = useState(organization?.slug ?? '');

  async function handleSubmit(event) {
    event.preventDefault();

    const payload = { name, slug };

    try {
      const result = isEditMode
        ? await updateOrganization(organization.publicUuid, payload)
        : await createOrganization(payload);

      onSuccess?.(result);

      if (!isEditMode) {
        setName('');
        setSlug('');
      }
    } catch {
      // Error is already captured in context state (`error`) and
      // rendered below — nothing else to do here.
    }
  }

  return (
    <form className="organization-form" onSubmit={handleSubmit}>
      <div className="organization-form__field">
        <label htmlFor="organization-name">Name</label>
        <input
          id="organization-name"
          type="text"
          value={name}
          maxLength={150}
          required
          onChange={(event) => setName(event.target.value)}
        />
      </div>

      <div className="organization-form__field">
        <label htmlFor="organization-slug">Slug</label>
        <input
          id="organization-slug"
          type="text"
          value={slug}
          maxLength={150}
          required
          // Slug mutability on edit is a backend Service decision, not
          // enforced client-side (see OrganizationRequestDTO.java note).
          // Left editable here; revisit if the backend starts rejecting
          // slug changes on PUT.
          onChange={(event) => setSlug(event.target.value)}
        />
      </div>

      {error && <p className="organization-form__error">{error}</p>}

      <button type="submit" disabled={isLoading}>
        {isEditMode ? 'Save changes' : 'Create organization'}
      </button>
    </form>
  );
}

export default OrganizationForm;