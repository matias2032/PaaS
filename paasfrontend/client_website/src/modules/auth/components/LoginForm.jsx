import { useState } from 'react';
import Button from '../../../shared/components/Button';
import TextField from '../../../shared/components/TextField';
import ErrorMessage from '../../../shared/components/ErrorMessage';
import Spinner from '../../../shared/components/Spinner';
import { useAuth } from '../hooks/useAuth';

/**
 * Login form. Structure only — styling comes later.
 */
function LoginForm({ onSuccess }) {
  const { login, isLoading, error } = useAuth();
  const [form, setForm] = useState({ email: '', password: '' });

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    try {
      await login(form);
      onSuccess?.();
    } catch {
      // Error is already captured in the auth context.
    }
  }

  return (
    <form className="login-form" onSubmit={handleSubmit}>
      <TextField
        label="Email"
        name="email"
        type="email"
        value={form.email}
        onChange={handleChange}
        required
      />
      <TextField
        label="Password"
        name="password"
        type="password"
        value={form.password}
        onChange={handleChange}
        required
      />

      <ErrorMessage message={error} />

      <Button type="submit" disabled={isLoading}>
        {isLoading ? <Spinner /> : 'Log in'}
      </Button>
    </form>
  );
}

export default LoginForm;