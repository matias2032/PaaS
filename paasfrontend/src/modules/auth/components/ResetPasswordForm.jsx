import { useState } from 'react';
import Button from '../../../shared/components/Button';
import TextField from '../../../shared/components/TextField';
import ErrorMessage from '../../../shared/components/ErrorMessage';
import Spinner from '../../../shared/components/Spinner';
import { useAuth } from '../hooks/useAuth';

/**
 * Reset password form. The token comes from the URL (?token=... —
 * see the link logged by AuthService.forgotPassword on the backend
 * in dev), not typed by the user, so it's received as a prop rather
 * than as a form field.
 */
function ResetPasswordForm({ token, onSuccess }) {
  const { resetPassword, isLoading, error } = useAuth();
  const [newPassword, setNewPassword] = useState('');

  async function handleSubmit(event) {
    event.preventDefault();
    try {
      await resetPassword({ token, newPassword });
      onSuccess?.();
    } catch {
      // Error is already captured in the auth context.
    }
  }

  return (
    <form className="reset-password-form" onSubmit={handleSubmit}>
      <TextField
        label="New password"
        name="newPassword"
        type="password"
        value={newPassword}
        onChange={(event) => setNewPassword(event.target.value)}
        required
      />

      <ErrorMessage message={error} />

      <Button type="submit" disabled={isLoading}>
        {isLoading ? <Spinner /> : 'Reset password'}
      </Button>
    </form>
  );
}

export default ResetPasswordForm;