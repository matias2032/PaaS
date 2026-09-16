import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useOrganization } from '../hooks/useOrganization';
import OrganizationForm from '../components/OrganizationForm';
import MemberList from '../components/MemberList';
import AddMemberForm from '../components/AddMemberForm';

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
  const { user } = useAuth();
  const {
    organizations,
    fetchOrganization,
    fetchMembers,
    removeMember,
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
        const org = cached || (await fetchOrganization(publicUuid));
        const memberList = await fetchMembers(publicUuid);

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

  async function handleRemove(member) {
    try {
      await removeMember(publicUuid, member.userPublicUuid);
      setMembers((prev) =>
        prev.filter((item) => item.userPublicUuid !== member.userPublicUuid)
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

  if (isLoading) return <p>Loading...</p>;
  if (error && !organization) return <p className="organization-detail-page__error">{error}</p>;
  if (!organization) return <p>Organization not found.</p>;

  return (
    <div className="organization-detail-page">
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
      </section>

      <section className="organization-detail-page__members">
        <h2>Members</h2>
        <MemberList
          members={members}
          canManageMembers={canManageMembers}
          onRemove={handleRemove}
        />

        {canManageMembers && (
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