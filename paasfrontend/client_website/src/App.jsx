import { AuthProvider } from './modules/auth/context/AuthProvider';
import { OrganizationProvider } from './modules/organization/context/OrganizationProvider';
import { BillingProvider } from './modules/billing/context/BillingProvider';
import { PaymentProvider } from './modules/payment/context/PaymentProvider';
import { ProjectProvider } from './modules/project/context/ProjectProvider';
import { ServiceProvider } from './modules/service/context/ServiceProvider';
import AppRouter from './router/AppRouter';

function App() {
  return (
    <AuthProvider>
      <OrganizationProvider>
        <BillingProvider>
          <PaymentProvider>
            <ProjectProvider>
              <ServiceProvider>
                <AppRouter />
              </ServiceProvider>
            </ProjectProvider>
          </PaymentProvider>
        </BillingProvider>
      </OrganizationProvider>
    </AuthProvider>
  );
}

export default App;