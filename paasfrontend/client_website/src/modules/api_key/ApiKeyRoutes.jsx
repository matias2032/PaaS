import ProtectedRoute from '../../shared/layout/ProtectedRoute';
import ApiKeysPage from './pages/ApiKeysPage';

/**
 * Routes owned by the api_key module. Self-wrapped in <ProtectedRoute>,
 * same pattern as organizationRoutes.
 */
const apiKeyRoutes = [
  {
    path: '/organizations/:publicUuid/api-keys',
    element: (
      <ProtectedRoute>
        <ApiKeysPage />
      </ProtectedRoute>
    ),
  },
];

export default apiKeyRoutes;