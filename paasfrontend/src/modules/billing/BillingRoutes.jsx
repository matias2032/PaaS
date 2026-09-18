import ProtectedRoute from '../../shared/layout/ProtectedRoute';
import PlansPage from './pages/PlansPage';
import SubscriptionPage from './pages/SubscriptionPage';

/**
 * Routes owned by the billing module. Aggregated into the central
 * router (router/AppRouter.jsx) — this module doesn't know about
 * routes from other modules, and vice versa.
 *
 * Like organizationRoutes, every route here requires an authenticated
 * session (there is no public billing route), so each element
 * self-wraps in <ProtectedRoute> right here, letting AppRouter.jsx
 * simply spread ...billingRoutes into its route list without any
 * special-casing — same pattern organizationRoutes established and
 * this module was expected to follow (see organizationRoutes.jsx's
 * own comment).
 *
 * Routes are nested under /organizations/:orgPublicUuid rather than a
 * standalone /billing root: billing is always scoped to one
 * organization (every endpoint in BillingController's subscription
 * half takes orgPublicUuid as a path variable — see billingApi.js),
 * and nesting here mirrors that URL shape 1:1 instead of inventing a
 * parallel one. It also makes both pages directly linkable from
 * OrganizationDetailPage (e.g. a future "Manage billing" link) without
 * that page needing to first set anything as the "active organization"
 * — billing reads orgPublicUuid straight from the URL, not from
 * OrganizationContext.
 *
 * No route-order hazard here (unlike organizationRoutes' '/new' vs
 * '/:publicUuid' case): 'plans' and 'subscription' are both static
 * literal segments after the same dynamic :orgPublicUuid, so neither
 * can shadow the other regardless of declaration order.
 */
const billingRoutes = [
  {
    path: '/organizations/:orgPublicUuid/plans',
    element: (
      <ProtectedRoute>
        <PlansPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/organizations/:orgPublicUuid/subscription',
    element: (
      <ProtectedRoute>
        <SubscriptionPage />
      </ProtectedRoute>
    ),
  },
];

export default billingRoutes;