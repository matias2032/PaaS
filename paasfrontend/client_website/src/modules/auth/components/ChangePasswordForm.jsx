import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../../../shared/components/Button';
import TextField from '../../../shared/components/TextField';
import ErrorMessage from '../../../shared/components/ErrorMessage';
import Spinner from '../../../shared/components/Spinner';
import { useAuth } from '../hooks/useAuth';

function ChangePasswordForm({ onSuccess }) {
  const { changePassword, logout, isLoading, error } = useAuth();
  const [form, setForm] = useState({ currentPassword: '', newPassword: '' });
  const [successMessage, setSuccessMessage] = useState('');
  const [isRedirecting, setIsRedirecting] = useState(false);
  const navigate = useNavigate();

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSuccessMessage('');

    try {
      await changePassword(form);
      setForm({ currentPassword: '', newPassword: '' });
      
      setSuccessMessage('Password successfully changed! Redirecting in 3 seconds...');
      setIsRedirecting(true);

      // Só redireciona se entrar no try (sucesso na API)
      setTimeout(() => {
        logout();
        onSuccess?.();
        navigate('/login');
      }, 3000);

} catch {
  // Em caso de erro, apenas cancela o estado de redirecionamento.
  setIsRedirecting(false);
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
        disabled={isRedirecting}
      />
      <TextField
        label="New password"
        name="newPassword"
        type="password"
        value={form.newPassword}
        onChange={handleChange}
        required
        disabled={isRedirecting}
      />

      <ErrorMessage message={error} />

      {successMessage && (
        <div style={{ color: 'green', margin: '10px 0' }}>
          {successMessage}
        </div>
      )}

      <Button type="submit" disabled={isLoading || isRedirecting}>
        {isLoading ? <Spinner /> : 'Change password'}
      </Button>
    </form>
  );
}

export default ChangePasswordForm;