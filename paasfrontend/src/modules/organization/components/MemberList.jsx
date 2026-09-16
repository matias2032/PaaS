/**
 * Presentational list of an organization's members. Like
 * OrganizationCard, doesn't call useOrganization() or the API itself —
 * the owning page (OrganizationDetailPage) owns fetching and passes
 * data + callbacks down. This keeps the component testable without a
 * provider and reusable if a member list is ever needed elsewhere
 * (e.g. a compact widget).
 *
 * `canManageMembers` is passed in rather than computed here, because
 * "am I OWNER/ADMIN in this org" depends on knowing the current user's
 * own membership row, which the page has (from the fetched member list)
 * and this component shouldn't need to re-derive.
 *
 * @param {{ members: import('../types/organization.types').OrganizationMemberResponse[], canManageMembers?: boolean, onRemove?: (member: import('../types/organization.types').OrganizationMemberResponse) => void }} props
 */
function MemberList({ members, canManageMembers = false, onRemove }) {
  if (members.length === 0) {
    return <p className="member-list__empty">No members yet.</p>;
  }

  return (
    <ul className="member-list">
      {members.map((member) => (
        <li key={member.userPublicUuid} className="member-list__item">
          <span className="member-list__name">
            {member.userFirstName} {member.userLastName}
          </span>
          <span className="member-list__email">{member.userEmail}</span>
          <span className="member-list__role">{member.roleName}</span>

          {canManageMembers && (
            <button
              type="button"
              className="member-list__remove"
              onClick={() => onRemove?.(member)}
            >
              Remove
            </button>
          )}
        </li>
      ))}
    </ul>
  );
}

export default MemberList;