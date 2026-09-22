import { BrowserRouter, Routes, Route } from 'react-router-dom';
import authRoutes from '../modules/auth/AuthRoutes';
import organizationRoutes from '../modules/organization/OrganizationRoutes';
import billingRoutes from '../modules/billing/BillingRoutes';
import projectRoutes from '../modules/project/ProjectRoutes';
import serviceRoutes from '../modules/service/ServiceRoutes';
import ProtectedRoute from '../shared/layout/ProtectedRoute';
import DashboardPage from '../modules/common/pages/DashboardPage';
import ProfilePage from '../modules/auth/pages/ProfilePage';

const allRoutes = [
  ...authRoutes,
  ...organizationRoutes,
  ...billingRoutes,
  ...projectRoutes,
  ...serviceRoutes,
  // Future modules append their routes here. Fully-protected modules
  // should follow organizationRoutes/billingRoutes' pattern of
  // self-wrapping each element in <ProtectedRoute>.
];

function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        {allRoutes.map((route) => (
          <Route key={route.path} path={route.path} element={route.element} />
        ))}

        <Route
          path="/"
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}

export default AppRouter;