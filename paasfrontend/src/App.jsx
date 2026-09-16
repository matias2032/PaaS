import { AuthProvider } from './modules/auth/context/AuthProvider';
import { OrganizationProvider } from './modules/organization/context/OrganizationProvider';
import AppRouter from './router/AppRouter';

function App() {
  return (
    <AuthProvider>
      <OrganizationProvider>
        <AppRouter />
      </OrganizationProvider>
    </AuthProvider>
  );
}

export default App;