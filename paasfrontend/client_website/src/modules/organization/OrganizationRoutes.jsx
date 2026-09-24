import ProtectedRoute from '../../shared/layout/ProtectedRoute';
import OrganizationListPage from './pages/OrganizationListPage';
import CreateOrganizationPage from './pages/CreateOrganizationPage';
import OrganizationDetailPage from './pages/OrganizationDetailPage';

/**
 * Routes owned by the organization module. Aggregated into the central
 * router (router/AppRouter.jsx) — this module doesn't know about
 * routes from other modules, and vice versa.
 *
 * Unlike AuthRoutes.jsx (which mixes public routes here with protected
 * ones declared by hand in AppRouter.jsx, e.g. /profile), every route
 * in this module requires an authenticated session — there is no
 * public organization route. So each element is wrapped in
 * <ProtectedRoute> right here, letting AppRouter.jsx simply spread
 * ...organizationRoutes into its route list without any special-casing,
 * the same way it already does for authRoutes. This is expected to be
 * the pattern for the next fully-protected modules too (billing,
 * project, service).
 *
 * Route order matters: '/organizations/new' must be declared before
 * '/organizations/:publicUuid', otherwise the dynamic segment would
 * greedily match the literal 'new' as a publicUuid.
 */
const organizationRoutes = [
  {
    path: '/organizations',
    element: (
      <ProtectedRoute>
        <OrganizationListPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/organizations/new',
    element: (
      <ProtectedRoute>
        <CreateOrganizationPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/organizations/:publicUuid',
    element: (
      <ProtectedRoute>
        <OrganizationDetailPage />
      </ProtectedRoute>
    ),
  },
];

export default organizationRoutes;