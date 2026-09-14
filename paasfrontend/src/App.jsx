import { useEffect } from 'react';
import { register } from './modules/auth/api/authApi';

function App() {
  useEffect(() => {
    register({
      email: `teste${Date.now()}@example.com`,
      password: 'password123',
      firstName: 'Teste',
      lastName: 'Frontend',
    })
      .then((data) => console.log('REGISTER OK:', data))
      .catch((err) => console.error('REGISTER ERRO:', err));
  }, []);

  return <h1>A testar ligação ao backend... vê a consola (F12)</h1>;
}

export default App;