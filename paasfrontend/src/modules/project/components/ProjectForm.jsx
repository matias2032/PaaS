import { useState } from 'react';
import { useProject } from '../hooks/useProject';
import { slugify } from '../../../shared/utils/slugify';

/**
 * Create-only form for a project — unlike OrganizationForm, this
 * doesn't handle edit mode, since ProjectDetailPage already has its
 * own inline name/description edit form and never touches slug there
 * either. Splitting create out of ProjectsListPage mirrors how
 * organizations already separate CreateOrganizationPage from
 * OrganizationListPage.
 *
 * Slug is never user-editable here, same reasoning as OrganizationForm:
 * computed from `name` via slugify() as the user types, shown as a
 * read-only preview. ProjectService.updateProject ignores slug on
 * update anyway, so there's no scenario where a manual slug field
 * would matter after creation.
 *
 * @param {{ orgPublicUuid: string, onSuccess?: (project: import('../types/project.types').ProjectResponse) => void }} props
 */
function ProjectForm({ orgPublicUuid, onSuccess }) {
  const { createProject, isProjectsLoading, projectsError } = useProject();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const previewSlug = slugify(name);

  async function handleSubmit(event) {
    event.preventDefault();

    try {
      const created = await createProject(orgPublicUuid, {
        name,
        slug: previewSlug,
        description,
      });
      onSuccess?.(created);
    } catch {
      // Error is already captured in context state (`projectsError`)
      // and rendered below — nothing else to do here.
    }
  }

  return (
    <form className="project-form" onSubmit={handleSubmit}>
      <div className="project-form__field">
        <label htmlFor="project-name">Name</label>
        <input
          id="project-name"
          type="text"
          value={name}
          maxLength={150}
          required
          onChange={(event) => setName(event.target.value)}
        />
      </div>

      <div className="project-form__field">
        <label htmlFor="project-slug">Slug</label>
        <input id="project-slug" type="text" value={previewSlug} readOnly disabled />
        <p className="project-form__hint">
          Generated automatically from the name — this will be the project's permanent
          identifier within this organization.
        </p>
      </div>

      <div className="project-form__field">
        <label htmlFor="project-description">Description</label>
        <textarea
          id="project-description"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
        />
      </div>

      {projectsError && <p className="project-form__error">{projectsError}</p>}

      <button type="submit" disabled={isProjectsLoading || previewSlug === ''}>
        Create project
      </button>
    </form>
  );
}

export default ProjectForm;