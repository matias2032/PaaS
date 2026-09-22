import { useState } from 'react';
import { useOrganization } from '../hooks/useOrganization';
import { slugify } from '../../../shared/utils/slugify';

/**
 * Shared form for both creating and editing an organization — same
 * shape as OrganizationRequestDTO (name + slug) either way.
 *
 * Mode is inferred from the `organization` prop: undefined/null = create,
 * an OrganizationResponse = edit. This mirrors the DTO comment in
 * OrganizationRequestDTO.java ("Used for both create and update").
 *
 * Slug is never user-editable through this form, in either mode:
 * - Create: computed from `name` via slugify() as the user types, shown
 *   as a read-only preview so they know what identifier they'll get.
 * - Edit: fixed to organization.slug — the backend ignores slug on
 *   update anyway (see OrganizationService.updateOrganization), so
 *   showing it as editable here would be misleading.
 * This removes the "slug" field as a thing the user has to understand
 * or fill in manually — the barrier this whole form used to have.
 *
 * @param {{ organization?: import('../types/organization.types').OrganizationResponse, onSuccess?: (org: import('../types/organization.types').OrganizationResponse) => void }} props
 */
function OrganizationForm({ organization, onSuccess }) {
  const isEditMode = !!organization;
  const { createOrganization, updateOrganization, isLoading, error } = useOrganization();

  const [name, setName] = useState(organization?.name ?? '');

  // Live preview only in create mode — in edit mode the slug is fixed
  // to whatever was generated at creation, regardless of the current
  // `name` value.
  const previewSlug = isEditMode ? organization.slug : slugify(name);

  async function handleSubmit(event) {
    event.preventDefault();

    const payload = { name, slug: isEditMode ? organization.slug : slugify(name) };

    try {
      const result = isEditMode
        ? await updateOrganization(organization.publicUuid, payload)
        : await createOrganization(payload);

      onSuccess?.(result);

      if (!isEditMode) {
        setName('');
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
          value={previewSlug}
          readOnly
          disabled
        />
        <p className="organization-form__hint">
          {isEditMode
            ? "Slug can't be changed after the organization is created."
            : 'Generated automatically from the name — this will be the organization’s permanent identifier.'}
        </p>
      </div>

      {error && <p className="organization-form__error">{error}</p>}

      <button type="submit" disabled={isLoading || (!isEditMode && previewSlug === '')}>
        {isEditMode ? 'Save changes' : 'Create organization'}
      </button>
    </form>
  );
}

export default OrganizationForm;