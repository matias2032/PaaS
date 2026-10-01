import ProtectedRoute from '../../shared/layout/ProtectedRoute';
import InvoicesPage from './pages/InvoicesPage';
import InvoiceDetailPage from './pages/InvoiceDetailPage';

/**
 * Routes owned by the payment module. Aggregated into the central
 * router (router/AppRouter.jsx), same pattern as billingRoutes: every
 * route requires an authenticated session, so each element
 * self-wraps in <ProtectedRoute> right here.
 *
 * Both routes are nested under /organizations/:orgPublicUuid, like
 * billing: invoices are listed per organization (see paymentApi.js).
 * The detail route is nested too, even though the backend reads an
 * invoice by its own uuid (GET /api/invoices/{uuid}), because
 * InvoiceResponse doesn't carry the organization uuid and the page
 * needs it to look up the user's role (OWNER gate for paying).
 *
 * No route-order hazard: 'invoices' is a static literal segment and the
 * detail route only adds one more segment after it.
 */
const paymentRoutes = [
  {
    path: '/organizations/:orgPublicUuid/invoices',
    element: (
      <ProtectedRoute>
        <InvoicesPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/organizations/:orgPublicUuid/invoices/:invoicePublicUuid',
    element: (
      <ProtectedRoute>
        <InvoiceDetailPage />
      </ProtectedRoute>
    ),
  },
];

export default paymentRoutes;