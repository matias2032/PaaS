import { Link } from 'react-router-dom';
import Button from '../../../shared/components/Button';
import { useAuth } from '../../auth/hooks/useAuth';
import { useOrganization } from '../../organization/hooks/useOrganization';

/**
 * Placeholder dashboard — first page shown after a successful login.
 * Structure only, no styling yet.
 */
function DashboardPage() {
  const { user, logout } = useAuth();
  const { activeOrgUuid } = useOrganization();

  // Billing has no organization-agnostic page (see BillingRoutes.jsx) —
  // it's always scoped to one organization. So this link only ever
  // points somewhere useful when an active org is already known;
  // otherwise it sends the user to pick one first, same as "My
  // organizations" does.
  const billingLink = activeOrgUuid
    ? `/organizations/${activeOrgUuid}/subscription`
    : '/organizations';

  return (
    <div className="dashboard-page">
      <h1>Dashboard</h1>
      <p>Welcome, {user?.firstName ?? 'user'}.</p>

      <Link to="/organizations">
        <Button variant="secondary">My organizations</Button>
      </Link>

      <Link to={billingLink}>
        <Button variant="secondary">Billing</Button>
      </Link>

      <Link to="/profile">
        <Button variant="secondary">Edit profile</Button>
      </Link>

      <Button variant="secondary" onClick={logout}>
        Log out
      </Button>
    </div>
  );
}

export default DashboardPage;