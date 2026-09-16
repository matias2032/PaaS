import { useEffect, useState } from 'react';
import { useOrganization } from '../hooks/useOrganization';

/**
 * Form to add a member by email + role. Fetches the roles catalog from
 * context (GET /api/organizations/roles) rather than hardcoding the 4
 * seeded roles — keeps a single source of truth with the backend (see
 * open question #4 in the handoff, resolved in favor of fetching).
 *
 * @param {{ organizationPublicUuid: string, onSuccess?: (member: import('../types/organization.types').OrganizationMemberResponse) => void }} props
 */
function AddMemberForm({ organizationPublicUuid, onSuccess }) {
  const { roles, fetchRoles, addMember, error } = useOrganization();

  const [userEmail, setUserEmail] = useState('');

  // Holds only an EXPLICIT user selection. The actual selected value
  // used by the <select> and on submit is derived below
  // (`selectedRoleCode`), defaulting to the first fetched role. This
  // avoids syncing state-from-state via a second useEffect (which
  // triggers an extra cascading render just to mirror `roles` into
  // `roleCode` on load) — derive during render instead.
  const [roleCode, setRoleCode] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [localError, setLocalError] = useState(null);

  // Legitimate useEffect: synchronizing with an external system (the
  // API), not deriving state from other state.
  useEffect(() => {
    if (roles.length === 0) {
      fetchRoles().catch(() => {
        // Surfaced via context `error` state already.
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const selectedRoleCode = roleCode || roles[0]?.code || '';

  async function handleSubmit(event) {
    event.preventDefault();
    setLocalError(null);

    if (!selectedRoleCode) {
      setLocalError('Roles are still loading — try again in a moment.');
      return;
    }

    setIsSubmitting(true);
    try {
      const member = await addMember(organizationPublicUuid, {
        userEmail,
        roleCode: selectedRoleCode,
      });
      onSuccess?.(member);
      setUserEmail('');
      setRoleCode('');
    } catch {
      // Error already captured in context state (`error`).
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="add-member-form" onSubmit={handleSubmit}>
      <div className="add-member-form__field">
        <label htmlFor="add-member-email">Email</label>
        <input
          id="add-member-email"
          type="email"
          value={userEmail}
          required
          onChange={(event) => setUserEmail(event.target.value)}
        />
      </div>

      <div className="add-member-form__field">
        <label htmlFor="add-member-role">Role</label>
        <select
          id="add-member-role"
          value={selectedRoleCode}
          required
          disabled={roles.length === 0}
          onChange={(event) => setRoleCode(event.target.value)}
        >
          {roles.map((role) => (
            <option key={role.code} value={role.code}>
              {role.name}
            </option>
          ))}
        </select>
      </div>

      {(localError || error) && (
        <p className="add-member-form__error">{localError || error}</p>
      )}

      <button type="submit" disabled={isSubmitting || roles.length === 0}>
        Add member
      </button>
    </form>
  );
}

export default AddMemberForm;