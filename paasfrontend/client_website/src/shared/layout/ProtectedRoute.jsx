import { Navigate } from 'react-router-dom';
import { useAuth } from '../../modules/auth/hooks/useAuth';

/**
 * Wraps a route element and redirects to /login when there's no
 * active session. Usage: <Route path="/" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
 */
function ProtectedRoute({ children }) {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

export default ProtectedRoute;