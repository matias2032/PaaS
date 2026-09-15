import { useState } from 'react';
import Button from '../../../shared/components/Button';
import TextField from '../../../shared/components/TextField';
import ErrorMessage from '../../../shared/components/ErrorMessage';
import Spinner from '../../../shared/components/Spinner';
import { useAuth } from '../hooks/useAuth';

/**
 * Change password form, for logged-in users. Structure only — styling
 * comes later.
 */
function ChangePasswordForm({ onSuccess }) {
  const { changePassword, isLoading, error } = useAuth();
  const [form, setForm] = useState({ currentPassword: '', newPassword: '' });

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    try {
      await changePassword(form);
      setForm({ currentPassword: '', newPassword: '' });
      onSuccess?.();
    } catch {
      // Error is already captured in the auth context.
    }
  }

  return (
    <form className="change-password-form" onSubmit={handleSubmit}>
      <TextField
        label="Current password"
        name="currentPassword"
        type="password"
        value={form.currentPassword}
        onChange={handleChange}
        required
      />
      <TextField
        label="New password"
        name="newPassword"
        type="password"
        value={form.newPassword}
        onChange={handleChange}
        required
      />

      <ErrorMessage message={error} />

      <Button type="submit" disabled={isLoading}>
        {isLoading ? <Spinner /> : 'Change password'}
      </Button>
    </form>
  );
}

export default ChangePasswordForm;