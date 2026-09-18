import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useOrganization } from '../hooks/useOrganization';
import OrganizationCard from '../components/OrganizationCard';
import BackButton from "../../../shared/components/BackButton";

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'All' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
  { value: 'SUSPENDED', label: 'Suspended' },
];

/**
 * GET /api/organizations, rendered as a filterable list of
 * OrganizationCard. Filtering is entirely client-side — the full list
 * is already fetched (no pagination on this endpoint), and the
 * expected number of organizations per user doesn't justify a
 * server-side filter/query-param contract for this.
 *
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

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [minMembers, setMinMembers] = useState('');
  const [maxMembers, setMaxMembers] = useState('');
  const [createdFrom, setCreatedFrom] = useState('');
  const [createdTo, setCreatedTo] = useState('');
  const [filtersOpen, setFiltersOpen] = useState(false);

  useEffect(() => {
    refreshOrganizations().catch(() => {
      // Surfaced via context `error` state already.
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleSelect(organization) {
    selectOrganization(organization.publicUuid);
  }

  function handleClearFilters() {
    setStatusFilter('ALL');
    setMinMembers('');
    setMaxMembers('');
    setCreatedFrom('');
    setCreatedTo('');
    // `search` intentionally not cleared here — it lives outside the
    // "Filters" panel and has its own visible clear affordance
    // (emptying the input), so it's not part of "advanced filters".
  }

  // Count only the fields inside the collapsible panel, so the toggle
  // button's badge reflects "advanced filters active", not the
  // always-visible search box.
  const activeAdvancedFilterCount = [
    statusFilter !== 'ALL',
    minMembers !== '',
    maxMembers !== '',
    createdFrom !== '',
    createdTo !== '',
  ].filter(Boolean).length;

  const filteredOrganizations = useMemo(() => {
    const term = search.trim().toLowerCase();
    const min = minMembers === '' ? null : Number(minMembers);
    const max = maxMembers === '' ? null : Number(maxMembers);
    const from = createdFrom ? new Date(createdFrom) : null;
    // Inclusive of the whole "to" day, not just 00:00.
    const to = createdTo ? new Date(`${createdTo}T23:59:59.999`) : null;

    return organizations.filter((org) => {
      if (term && !org.name.toLowerCase().includes(term)) return false;
      if (statusFilter !== 'ALL' && org.status !== statusFilter) return false;
      if (min !== null && org.memberCount < min) return false;
      if (max !== null && org.memberCount > max) return false;

      const createdAt = new Date(org.createdAt);
      if (from && createdAt < from) return false;
      if (to && createdAt > to) return false;

      return true;
    });
  }, [organizations, search, statusFilter, minMembers, maxMembers, createdFrom, createdTo]);

  const hasAnyOrganizations = organizations.length > 0;
  const hasVisibleResults = filteredOrganizations.length > 0;
  const isFiltering = search.trim() !== '' || activeAdvancedFilterCount > 0;

  return (
    <div className="organization-list-page">
      <header className="organization-list-page__header">
        <BackButton />
        <h1>My organizations</h1>
        <Link to="/organizations/new" className="organization-list-page__create-link">
          New organization
        </Link>
      </header>

      {hasAnyOrganizations && (
        <div className="organization-list-page__toolbar">
          <input
            type="search"
            className="organization-list-page__search"
            placeholder="Search by name..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            aria-label="Search organizations by name"
          />

          <button
            type="button"
            className="organization-list-page__filters-toggle"
            onClick={() => setFiltersOpen((open) => !open)}
            aria-expanded={filtersOpen}
          >
            Filters
            {activeAdvancedFilterCount > 0 && (
              <span className="organization-list-page__filters-badge">
                {activeAdvancedFilterCount}
              </span>
            )}
          </button>
        </div>
      )}

      {filtersOpen && hasAnyOrganizations && (
        <div className="organization-list-page__filters-panel">
          <div className="organization-list-page__filter-group">
            <label htmlFor="org-filter-status">Status</label>
            <select
              id="org-filter-status"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <div className="organization-list-page__filter-group">
            <span className="organization-list-page__filter-group-label">Members</span>
            <div className="organization-list-page__range-inputs">
              <input
                type="number"
                min="0"
                placeholder="Min"
                value={minMembers}
                onChange={(event) => setMinMembers(event.target.value)}
                aria-label="Minimum members"
              />
              <span aria-hidden="true">–</span>
              <input
                type="number"
                min="0"
                placeholder="Max"
                value={maxMembers}
                onChange={(event) => setMaxMembers(event.target.value)}
                aria-label="Maximum members"
              />
            </div>
          </div>

          <div className="organization-list-page__filter-group">
            <span className="organization-list-page__filter-group-label">Created</span>
            <div className="organization-list-page__range-inputs">
              <input
                type="date"
                value={createdFrom}
                onChange={(event) => setCreatedFrom(event.target.value)}
                aria-label="Created after"
              />
              <span aria-hidden="true">–</span>
              <input
                type="date"
                value={createdTo}
                onChange={(event) => setCreatedTo(event.target.value)}
                aria-label="Created before"
              />
            </div>
          </div>

          {activeAdvancedFilterCount > 0 && (
            <button
              type="button"
              className="organization-list-page__clear-filters"
              onClick={handleClearFilters}
            >
              Clear filters
            </button>
          )}
        </div>
      )}

      {isLoading && organizations.length === 0 && <p>Loading...</p>}
      {error && <p className="organization-list-page__error">{error}</p>}

      {!isLoading && !hasAnyOrganizations && !error && (
        <p>You don't belong to any organization yet.</p>
      )}

      {!isLoading && hasAnyOrganizations && !hasVisibleResults && (
        <p className="organization-list-page__no-results">
          No organizations match your filters.
          {isFiltering && (
            <>
              {' '}
              <button
                type="button"
                className="organization-list-page__no-results-clear"
                onClick={() => {
                  setSearch('');
                  handleClearFilters();
                }}
              >
                Clear all filters
              </button>
            </>
          )}
        </p>
      )}

      <ul className="organization-list-page__list">
        {filteredOrganizations.map((organization) => (
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