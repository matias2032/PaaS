import { BrowserRouter, Routes, Route } from 'react-router-dom';
import authRoutes from '../modules/auth/AuthRoutes';
import ProtectedRoute from '../shared/layout/ProtectedRoute';
import DashboardPage from '../modules/common/pages/DashboardPage';
import ProfilePage from '../modules/auth/pages/ProfilePage';

const allRoutes = [
  ...authRoutes,
  // Future modules append their routes here, e.g. ...organizationRoutes
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