import { AuthProvider } from './modules/auth/context/AuthProvider';
import { OrganizationProvider } from './modules/organization/context/OrganizationProvider';
import { BillingProvider } from './modules/billing/context/BillingProvider';
import AppRouter from './router/AppRouter';

function App() {
  return (
    <AuthProvider>
      <OrganizationProvider>
        <BillingProvider>
          <AppRouter />
        </BillingProvider>
      </OrganizationProvider>
    </AuthProvider>
  );
}

export default App;