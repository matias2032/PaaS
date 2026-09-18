import { useEffect, useState } from 'react';
import { useParams, useNavigate,Link } from 'react-router-dom';
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
    reactivateOrganization,
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
const canRemoveOtherMembers = currentMembership?.roleCode === 'OWNER';

// Registration, role changes, deactivation and reactivation remain OWNER-only.
const isOwner = currentMembership?.roleCode === 'OWNER';

// Single source of truth for "is this org writable right now" — every
// write endpoint (update, add/remove member, change role) is rejected
// by the backend with 409 while status is INACTIVE. Any member can
// still leave the org themselves, but only while it's ACTIVE — see
// MemberList's canLeave note.
const isOrganizationActive = organization.status !== 'INACTIVE';
const canLeave = isOrganizationActive;

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
      `Deactivate "${organization.name}"? No changes will be allowed on this organization until it's reactivated.`
    );
    if (!confirmed) return;

    try {
      await deactivateOrganization(publicUuid);
      navigate('/organizations');
    } catch {
      // Surfaced via context `error` state already.
    }
  }

  async function handleReactivate() {
    try {
      const updated = await reactivateOrganization(publicUuid);
      setOrganization(updated);
    } catch {
      // Surfaced via context `error` state already (e.g. "Organization
      // is not inactive" if it was reactivated elsewhere in the
      // meantime).
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
        {canManageMembers && isOrganizationActive ? (
          <OrganizationForm organization={organization} onSuccess={handleOrganizationUpdated} />
        ) : (
          // Read-only fallback: for DEVELOPER/VIEWER regardless of
          // status, and for anyone (OWNER/ADMIN included) once the
          // organization is INACTIVE, since updateOrganization is
          // rejected by the backend in that state. Mirrors the fields
          // OrganizationForm would otherwise expose as editable.
          <dl className="organization-detail-page__readonly">
            <dt>Name</dt>
            <dd>{organization.name}</dd>
            <dt>Slug</dt>
            <dd>{organization.slug}</dd>
            <dt>Status</dt>
            <dd>{organization.status}</dd>
          </dl>
        )}

        {isOwner && isOrganizationActive && (
          // OWNER-only, deliberately separate from canManageMembers —
          // deactivation is more destructive than the settings edits
          // above and is not delegated to ADMIN.
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

        {!isOrganizationActive && (
          <div className="organization-detail-page__inactive-notice">
            <p>This organization has been deactivated. No changes are allowed until it's reactivated.</p>
            {isOwner && (
              <button
                type="button"
                className="organization-detail-page__reactivate"
                onClick={handleReactivate}
              >
                Reactivate organization
              </button>
            )}
          </div>
        )}
      </section>

      <section className="organization-detail-page__members">
        <h2>Members</h2>
<MemberList
  members={members}
  currentUserPublicUuid={user?.publicUuid}
  canLeave={canLeave}
  canRemoveOtherMembers={canRemoveOtherMembers && isOrganizationActive}
  canChangeRoles={isOwner && isOrganizationActive}
  roles={roles}
  onRemove={handleRemove}
  onChangeRole={handleChangeRole}
/>
        {isOwner && isOrganizationActive && (
          // Registration is OWNER-only — canManageMembers (OWNER+ADMIN)
          // is intentionally NOT used as the gate here, unlike before.
          // Also hidden while INACTIVE: addMember is rejected by the
          // backend in that state.
          <>
            <h3>Add member</h3>
            <AddMemberForm organizationPublicUuid={publicUuid} onSuccess={handleMemberAdded} />
          </>
        )}
      </section>

            <section className="organization-detail-page__billing">
        <h2>Billing</h2>
        <Link to={`/organizations/${publicUuid}/subscription`}>
          Manage subscription
        </Link>
      </section>
    </div>
  );
}

export default OrganizationDetailPage;