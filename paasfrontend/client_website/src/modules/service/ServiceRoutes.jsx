import ProtectedRoute from '../../shared/layout/ProtectedRoute';
import ServicesListPage from './pages/ServicesListPage';
import CreateServicePage from './pages/CreateServicePage';
import ServiceDetailPage from './pages/ServiceDetailPage';
import ServiceRepositoryPage from './pages/ServiceRepositoryPage';
import ServiceBuildConfigPage from './pages/ServiceBuildConfigPage';
import ServiceResourceConfigPage from './pages/ServiceResourceConfigPage';
import ServiceEnvironmentVariablesPage from './pages/ServiceEnvironmentVariablesPage';
import ServiceDomainsPage from './pages/ServiceDomainsPage';
import ServiceDeploymentsPage from './pages/ServiceDeploymentsPage';

const serviceRoutes = [
  {
    path: '/projects/:projectPublicUuid/services',
    element: (
      <ProtectedRoute>
        <ServicesListPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/projects/:projectPublicUuid/services/new',
    element: (
      <ProtectedRoute>
        <CreateServicePage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/services/:publicUuid',
    element: (
      <ProtectedRoute>
        <ServiceDetailPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/services/:publicUuid/repository',
    element: (
      <ProtectedRoute>
        <ServiceRepositoryPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/services/:publicUuid/build-config',
    element: (
      <ProtectedRoute>
        <ServiceBuildConfigPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/services/:publicUuid/resource-config',
    element: (
      <ProtectedRoute>
        <ServiceResourceConfigPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/services/:publicUuid/environment-variables',
    element: (
      <ProtectedRoute>
        <ServiceEnvironmentVariablesPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/services/:publicUuid/domains',
    element: (
      <ProtectedRoute>
        <ServiceDomainsPage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/services/:publicUuid/deployments',
    element: (
      <ProtectedRoute>
        <ServiceDeploymentsPage />
      </ProtectedRoute>
    ),
  },
];

export default serviceRoutes;