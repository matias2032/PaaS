import { useState } from 'react';
import Button from '../../../shared/components/Button';
import TextField from '../../../shared/components/TextField';
import ErrorMessage from '../../../shared/components/ErrorMessage';
import Spinner from '../../../shared/components/Spinner';
import { useAuth } from '../hooks/useAuth';
/**
 * Registration form. Structure only — styling comes later.
 * Mirrors AuthRequestDTO on the backend (firstName/lastName/phone
 * are only used here, on login only email/password matter).
 */
function RegisterForm({ onSuccess }) {
  const { register, isLoading, error } = useAuth();
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    password: '',
  });

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    try {
      await register(form);
      onSuccess?.();
    } catch {
      // Error is already captured in the auth context.
    }
  }

  return (
    <form className="register-form" onSubmit={handleSubmit}>
      <TextField
        label="First name"
        name="firstName"
        value={form.firstName}
        onChange={handleChange}
        required
      />
      <TextField
        label="Last name"
        name="lastName"
        value={form.lastName}
        onChange={handleChange}
      />
      <TextField
        label="Email"
        name="email"
        type="email"
        value={form.email}
        onChange={handleChange}
        required
      />
      <TextField
        label="Phone"
        name="phone"
        type="tel"
        value={form.phone}
        onChange={handleChange}
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
        {isLoading ? <Spinner /> : 'Create account'}
      </Button>
    </form>
  );
}

export default RegisterForm;