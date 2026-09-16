import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useOrganization } from '../hooks/useOrganization';
import OrganizationForm from '../components/OrganizationForm';
import MemberList from '../components/MemberList';
import AddMemberForm from '../components/AddMemberForm';
import BackButton from "../../../shared/components/BackButton";

/**
 * GET /api/organizations/{publicUuid} (via the org already in
 * `organizations` list — falls back to fetchOrganization() for
 * deep-link navigation, e.g. page refresh landing directly here
 * before the list has loaded) + GET .../members.
 *
 * This is the one page in the module allowed to import useAuth() —
 * it needs the current user's own publicUuid to determine
 * canManageMembers (OWNER/ADMIN), which MemberList itself doesn't
 * compute (see MemberList.jsx note). This does NOT violate the
 * module-isolation convention used for routes/contexts: reading the
 * current user id at the page level, from a hook the auth module
 * already exports publicly, is a normal cross-module read — it's
 * direct context/provider coupling between modules that's avoided.
 */
function OrganizationDetailPage() {
  const { publicUuid } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const {
    organizations,
    roles,
    fetchRoles,
    fetchOrganization,
    fetchMembers,
    removeMember,
    changeMemberRole,
    deactivateOrganization,
    error,
  } = useOrganization();

  const [organization, setOrganization] = useState(
    () => organizations.find((org) => org.publicUuid === publicUuid) || null
  );
  const [members, setMembers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      try {
        const cached = organizations.find((org) => org.publicUuid === publicUuid);
        const [org, memberList] = await Promise.all([
          cached ? Promise.resolve(cached) : fetchOrganization(publicUuid),
          fetchMembers(publicUuid),
          // Needed for the role <select> in MemberList when the current
          // user is OWNER. Cached at context level (see
          // OrganizationProvider) so this is a no-op after first load
          // on any organization page, not a per-page refetch.
          roles.length === 0 ? fetchRoles() : Promise.resolve(roles),
        ]);

        if (!cancelled) {
          setOrganization(org);
          setMembers(memberList);
        }
      } catch {
        // Surfaced via context `error` state already.
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [publicUuid]);

  const currentMembership = members.find(
    (member) => member.userPublicUuid === user?.publicUuid
  );
  // Single source of truth for the OWNER/ADMIN gate on this page — both
  // the settings form and the add-member form key off this same value,
  // so the permission rule can't drift between the two sections.
const canManageMembers =
  currentMembership?.roleCode === 'OWNER' || currentMembership?.roleCode === 'ADMIN';

// Only OWNER can remove other members.
// Every member can still leave the organization themselves.
const canRemoveOtherMembers = currentMembership?.roleCode === 'OWNER';

// Registration, role changes and deactivation remain OWNER-only.
const isOwner = currentMembership?.roleCode === 'OWNER';

async function handleRemove(member) {
  try {
    const isCurrentUser =
      member.userPublicUuid === user?.publicUuid;

    await removeMember(publicUuid, member.userPublicUuid);

    if (isCurrentUser) {
      navigate('/organizations');
      return;
    }

    setMembers((prev) =>
      prev.filter(
        (item) => item.userPublicUuid !== member.userPublicUuid
      )
    );
  } catch {
    // Surfaced via context `error` state already.
  }
}

  function handleMemberAdded(member) {
    setMembers((prev) => [...prev, member]);
  }

  function handleOrganizationUpdated(updated) {
    setOrganization(updated);
  }

  async function handleChangeRole(member, newRoleCode) {
    try {
      const updated = await changeMemberRole(publicUuid, member.userPublicUuid, newRoleCode);
      setMembers((prev) =>
        prev.map((item) => (item.userPublicUuid === updated.userPublicUuid ? updated : item))
      );
    } catch {
      // Surfaced via context `error` state already (e.g. "Cannot demote
      // the last remaining OWNER" from OrganizationService).
    }
  }

  async function handleDeactivate() {
    // eslint-disable-next-line no-alert
    const confirmed = window.confirm(
      `Deactivate "${organization.name}"? This cannot be undone from the UI.`
    );
    if (!confirmed) return;

    try {
      await deactivateOrganization(publicUuid);
      navigate('/organizations');
    } catch {
      // Surfaced via context `error` state already.
    }
  }

  if (isLoading) return <p>Loading...</p>;
  if (error && !organization) return <p className="organization-detail-page__error">{error}</p>;
  if (!organization) return <p>Organization not found.</p>;

  return (

    <div className="organization-detail-page">
      <BackButton />
      <h1>{organization.name}</h1>

      <section className="organization-detail-page__settings">
        <h2>Settings</h2>
        {canManageMembers ? (
          <OrganizationForm organization={organization} onSuccess={handleOrganizationUpdated} />
        ) : (
          // Read-only fallback for DEVELOPER/VIEWER — this page must
          // still be useful to them (they can reach it via
          // OrganizationListPage regardless of role), just without the
          // ability to mutate. Mirrors the fields OrganizationForm
          // would otherwise expose as editable.
          <dl className="organization-detail-page__readonly">
            <dt>Name</dt>
            <dd>{organization.name}</dd>
            <dt>Slug</dt>
            <dd>{organization.slug}</dd>
            <dt>Status</dt>
            <dd>{organization.status}</dd>
          </dl>
        )}

        {isOwner && organization.status !== 'INACTIVE' && (
          // OWNER-only, deliberately separate from canManageMembers —
          // deactivation is more destructive than the settings edits
          // above and is not delegated to ADMIN. Hidden once already
          // INACTIVE since there's no reactivation flow yet.
          <div className="organization-detail-page__danger-zone">
            <h3>Danger zone</h3>
            <button
              type="button"
              className="organization-detail-page__deactivate"
              onClick={handleDeactivate}
            >
              Deactivate organization
            </button>
          </div>
        )}

        {organization.status === 'INACTIVE' && (
          <p className="organization-detail-page__inactive-notice">
            This organization has been deactivated.
          </p>
        )}
      </section>

      <section className="organization-detail-page__members">
        <h2>Members</h2>
<MemberList
  members={members}
  currentUserPublicUuid={user?.publicUuid}
  canRemoveOtherMembers={canRemoveOtherMembers}
  canChangeRoles={isOwner}
  roles={roles}
  onRemove={handleRemove}
  onChangeRole={handleChangeRole}
/>
        {isOwner && (
          // Registration is OWNER-only — canManageMembers (OWNER+ADMIN)
          // is intentionally NOT used as the gate here, unlike before.
          <>
            <h3>Add member</h3>
            <AddMemberForm organizationPublicUuid={publicUuid} onSuccess={handleMemberAdded} />
          </>
        )}
      </section>
    </div>
  );
}

export default OrganizationDetailPage;