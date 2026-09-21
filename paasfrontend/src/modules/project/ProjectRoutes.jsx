import ProtectedRoute from '../../shared/layout/ProtectedRoute';
import ProjectsListPage from './pages/ProjectsListPage';
import ProjectDetailPage from './pages/ProjectDetailPage';
import GitConnectionsPage from './pages/GitConnectionsPage';

const projectRoutes = [
  {
    path: '/organizations/:orgPublicUuid/projects',
    element: (
      <ProtectedRoute>
        <ProjectsListPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/projects/:publicUuid',
    element: (
      <ProtectedRoute>
        <ProjectDetailPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/organizations/:orgPublicUuid/git-connections',
    element: (
      <ProtectedRoute>
        <GitConnectionsPage />
      </ProtectedRoute>
    ),
  },
];

export default projectRoutes;