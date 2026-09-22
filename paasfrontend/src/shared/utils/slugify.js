/**
 * Converts a human-entered name into a URL/identifier-safe slug.
 * Used at creation time for both organizations and projects — the
 * slug is generated once from the name given at that moment and
 * never regenerated afterward (name stays editable, slug doesn't —
 * see OrganizationService.updateOrganization / ProjectService.updateProject,
 * which both ignore slug on update).
 *
 * @param {string} name
 * @param {number} [maxLength=150] - matches the backend's @Size(max = 150)
 *   on both OrganizationRequestDTO.slug and ProjectRequestDTO.slug
 * @returns {string}
 */
export function slugify(name, maxLength = 150) {
  if (!name) return '';

  return name
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '') // strip accents (á, ã, ç, etc.)
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, '-') // anything not alphanumeric becomes a hyphen
    .replace(/-+/g, '-') // collapse repeated hyphens
    .replace(/^-|-$/g, '') // trim leading/trailing hyphens
    .slice(0, maxLength)
    .replace(/-$/g, ''); // slice() above might cut mid-hyphen-run at the boundary
}