import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useOrganization } from '../hooks/useOrganization';
import OrganizationCard from '../components/OrganizationCard';

/**
 * GET /api/organizations, rendered as a list of OrganizationCard.
 * Selecting a card sets it as the active organization (context) AND
 * navigates to its detail page — the two are kept in sync here rather
 * than left for OrganizationDetailPage to reconcile.
 */
function OrganizationListPage() {
  const {
    organizations,
    activeOrgUuid,
    isLoading,
    error,
    refreshOrganizations,
    selectOrganization,
  } = useOrganization();

  useEffect(() => {
    refreshOrganizations().catch(() => {
      // Surfaced via context `error` state already.
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleSelect(organization) {
    selectOrganization(organization.publicUuid);
  }

  return (
    <div className="organization-list-page">
      <header className="organization-list-page__header">
        <h1>My organizations</h1>
        <Link to="/organizations/new" className="organization-list-page__create-link">
          New organization
        </Link>
      </header>

      {isLoading && organizations.length === 0 && <p>Loading...</p>}
      {error && <p className="organization-list-page__error">{error}</p>}

      {!isLoading && organizations.length === 0 && !error && (
        <p>You don't belong to any organization yet.</p>
      )}

      <ul className="organization-list-page__list">
        {organizations.map((organization) => (
          <li key={organization.publicUuid}>
            <Link to={`/organizations/${organization.publicUuid}`}>
              <OrganizationCard
                organization={organization}
                isActive={organization.publicUuid === activeOrgUuid}
                onSelect={handleSelect}
              />
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default OrganizationListPage;