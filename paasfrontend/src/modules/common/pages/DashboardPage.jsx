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
      <button type="button" onClick={logout}>
        Log out
      </button>
    </div>
  );
}

export default DashboardPage;