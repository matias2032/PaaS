import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import ResetPasswordForm from '../components/ResetPasswordForm';

/**
 * Skeleton only — no styling yet.
 *
 * Reads the reset token from the URL query string (?token=...), the
 * same format AuthService.forgotPassword logs on the backend in dev.
 */
function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  if (!token) {
    return (
      <div className="reset-password-page">
        <h1>Reset password</h1>
        <p>Missing or invalid reset link.</p>
        <Link to="/forgot-password">Request a new one</Link>
      </div>
    );
  }

  return (
    <div className="reset-password-page">
      <h1>Reset password</h1>

      <ResetPasswordForm token={token} onSuccess={() => navigate('/login')} />
    </div>
  );
}

export default ResetPasswordPage;