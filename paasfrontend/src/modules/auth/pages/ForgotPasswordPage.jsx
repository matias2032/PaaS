import { Link } from 'react-router-dom';
import ForgotPasswordForm from '../components/ForgotPasswordForm';

/**
 * Skeleton only — no styling yet.
 */
function ForgotPasswordPage() {
  return (
    <div className="forgot-password-page">
      <h1>Forgot password</h1>

      <ForgotPasswordForm />

      <p>
        Remembered your password? <Link to="/login">Log in</Link>
      </p>
    </div>
  );
}

export default ForgotPasswordPage;