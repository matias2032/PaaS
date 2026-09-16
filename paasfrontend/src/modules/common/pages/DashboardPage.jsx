import { Link } from 'react-router-dom';
import Button from '../../../shared/components/Button';
import { useAuth } from '../../auth/hooks/useAuth';

/**
 * Placeholder dashboard — first page shown after a successful login.
 * Structure only, no styling yet.
 */
function DashboardPage() {
  const { user, logout } = useAuth();

  return (
    <div className="dashboard-page">
      <h1>Dashboard</h1>
      <p>Welcome, {user?.firstName ?? 'user'}.</p>

      <Link to="/organizations">
        <Button variant="secondary">My organizations</Button>
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