import { AuthProvider } from './modules/auth/context/AuthProvider';
import { OrganizationProvider } from './modules/organization/context/OrganizationProvider';
import { BillingProvider } from './modules/billing/context/BillingProvider';
import { ProjectProvider } from './modules/project/context/ProjectProvider';
import AppRouter from './router/AppRouter';

function App() {
  return (
    <AuthProvider>
      <OrganizationProvider>
        <BillingProvider>
          <ProjectProvider>
            <AppRouter />
          </ProjectProvider>
        </BillingProvider>
      </OrganizationProvider>
    </AuthProvider>
  );
}

export default App;