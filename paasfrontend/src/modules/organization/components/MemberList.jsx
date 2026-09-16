/**
 * Presentational list of an organization's members. Like
 * OrganizationCard, doesn't call useOrganization() or the API itself —
 * the owning page (OrganizationDetailPage) owns fetching and passes
 * data + callbacks down. This keeps the component testable without a
 * provider and reusable if a member list is ever needed elsewhere
 * (e.g. a compact widget).
 *
 * `canRemoveOtherMembers` (remove another member — OWNER only) and
 * `canChangeRoles` (change a member's role — OWNER only) are two
 * separate flags, passed in rather than computed here, because "what
 * can I do in this org" depends on the current user's own membership.
 *
 * `currentUserPublicUuid` is used only to determine whether the row
 * belongs to the authenticated user. Every member can leave the
 * organization themselves, regardless of role. The action is displayed
 * as "Leave" for the current user and "Remove" for another member
 * when the current user has OWNER permission.
 * row, which the page has (from the fetched member list) and this
 * component shouldn't need to re-derive. They're deliberately not the
 * same flag: role changes are more sensitive than removal and are
 * OWNER-exclusive (see OrganizationService.changeMemberRole).
 *
 * `roles` is only needed when `canChangeRoles` is true — pass the
 * roles catalog (context `roles`, GET /api/organizations/roles) so the
 * role <select> has options without this component fetching anything.
 *
 * @param {{ members: import('../types/organization.types').OrganizationMemberResponse[], currentUserPublicUuid?: string, canRemoveOtherMembers?: boolean, canChangeRoles?: boolean, roles?: import('../types/organization.types').OrganizationRole[], onRemove?: (member: import('../types/organization.types').OrganizationMemberResponse) => void, onChangeRole?: (member: import('../types/organization.types').OrganizationMemberResponse, newRoleCode: string) => void }} props
 */
function MemberList({
  members,
  currentUserPublicUuid,
  canRemoveOtherMembers = false,
  canChangeRoles = false,
  roles = [],
  onRemove,
  onChangeRole,
}) {
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

          {canChangeRoles ? (
            <select
              className="member-list__role-select"
              value={member.roleCode}
              onChange={(event) => onChangeRole?.(member, event.target.value)}
            >
              {roles.map((role) => (
                <option key={role.code} value={role.code}>
                  {role.name}
                </option>
              ))}
            </select>
          ) : (
            <span className="member-list__role">{member.roleName}</span>
          )}

{(member.userPublicUuid === currentUserPublicUuid ||
  canRemoveOtherMembers) && (
  <button
    type="button"
    className="member-list__remove"
    onClick={() => onRemove?.(member)}
  >
    {member.userPublicUuid === currentUserPublicUuid ? 'Leave' : 'Remove'}
  </button>
)}
        </li>
      ))}
    </ul>
  );
}

export default MemberList;