import { useState } from 'react';
import Button from '../../../shared/components/Button';
import TextField from '../../../shared/components/TextField';
import ErrorMessage from '../../../shared/components/ErrorMessage';
import Spinner from '../../../shared/components/Spinner';
import { useAuth } from '../hooks/useAuth';

/**
 * Forgot password form. Always shows the generic backend message on
 * success, regardless of whether the email exists (avoids account
 * enumeration — see AuthService.forgotPassword on the backend).
 */
function ForgotPasswordForm() {
  const { forgotPassword, isLoading, error } = useAuth();
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setMessage(null);
    try {
      const response = await forgotPassword({ email });
      setMessage(response.message);
    } catch {
      // Error is already captured in the auth context.
    }
  }

  return (
    <form className="forgot-password-form" onSubmit={handleSubmit}>
      <TextField
        label="Email"
        name="email"
        type="email"
        value={email}
        onChange={(event) => setEmail(event.target.value)}
        required
      />

      <ErrorMessage message={error} />
      {message && <p className="forgot-password-form__success">{message}</p>}

      <Button type="submit" disabled={isLoading}>
        {isLoading ? <Spinner /> : 'Send reset link'}
      </Button>
    </form>
  );
}

export default ForgotPasswordForm;