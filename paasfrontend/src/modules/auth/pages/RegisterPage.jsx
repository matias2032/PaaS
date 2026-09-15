import { Link, useNavigate } from 'react-router-dom';
import RegisterForm from '../components/RegisterForm';

/**
 * Skeleton only — no styling yet.
 */
function RegisterPage() {
  const navigate = useNavigate();

  return (
    <div className="register-page">
      <h1>Create account</h1>

      <RegisterForm onSuccess={() => navigate('/')} />

      <p>
        Already have an account? <Link to="/login">Log in</Link>
      </p>
    </div>
  );
}

export default RegisterPage;