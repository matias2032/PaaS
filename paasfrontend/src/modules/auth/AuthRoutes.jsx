import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';

/**
 * Routes owned by the auth module. Aggregated into the central
 * router (router/AppRouter.jsx) — this module doesn't know about
 * routes from other modules, and vice versa.
 */
const authRoutes = [
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
];

export default authRoutes;