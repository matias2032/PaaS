import { Link, useNavigate } from 'react-router-dom';
import LoginForm from '../components/LoginForm';

/**
 * Skeleton only — no styling yet.
 */
function LoginPage() {
  const navigate = useNavigate();

  return (
    <div className="login-page">
      <h1>Log in</h1>

      <LoginForm onSuccess={() => navigate('/')} />

      <p>
        Don&apos;t have an account? <Link to="/register">Sign up</Link>
      </p>
    </div>
  );
}

export default LoginPage;