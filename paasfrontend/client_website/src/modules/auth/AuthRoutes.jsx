import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';

/**
 * Routes owned by the auth module. Aggregated into the central
 * router (router/AppRouter.jsx) — this module doesn't know about
 * routes from other modules, and vice versa.
 *
 * Note: /profile is NOT listed here even though it belongs to the
 * auth module conceptually — it requires an authenticated session,
 * so it's wrapped in <ProtectedRoute> directly in AppRouter.jsx,
 * the same way DashboardPage is for "/".
 */
const authRoutes = [
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/forgot-password', element: <ForgotPasswordPage /> },
  { path: '/reset-password', element: <ResetPasswordPage /> },
];

export default authRoutes;