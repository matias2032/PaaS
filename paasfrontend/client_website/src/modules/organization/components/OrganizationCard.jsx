/**
 * Presentational item for the "my organizations" list. Renders as a
 * <div role="button"> instead of a native <button> so it can be safely
 * nested inside a react-router <Link> (a <button> inside an <a> is
 * invalid HTML and breaks keyboard/screen-reader semantics). Keeps
 * onSelect for callers that want a side-effect on click (e.g.
 * selectOrganization()) independent of the navigation itself, which
 * the wrapping <Link> handles.
 *
 * @param {{ organization: import('../types/organization.types').OrganizationResponse, isActive?: boolean, onSelect?: (organization: import('../types/organization.types').OrganizationResponse) => void }} props
 */
function OrganizationCard({ organization, isActive = false, onSelect }) {
  function handleClick() {
    onSelect?.(organization);
  }

  function handleKeyDown(event) {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      onSelect?.(organization);
    }
  }

  return (
    <div
      role="button"
      tabIndex={0}
      className={`organization-card${isActive ? ' organization-card--active' : ''}`}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
    >
      <span className="organization-card__name">{organization.name}</span>
      <span className="organization-card__slug">{organization.slug}</span>
      <span className="organization-card__members">
        {organization.memberCount} {organization.memberCount === 1 ? 'member' : 'members'}
      </span>
      <span className="organization-card__status">{organization.status}</span>
    </div>
  );
}

export default OrganizationCard;