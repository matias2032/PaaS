import { useState } from 'react';
import Button from '../../../shared/components/Button';
import TextField from '../../../shared/components/TextField';
import ErrorMessage from '../../../shared/components/ErrorMessage';
import Spinner from '../../../shared/components/Spinner';
import { useAuth } from '../hooks/useAuth';

// Digits, spaces, +, - and parentheses; 7 to 30 characters (the backend
// column is 30 long). Same rule as the admin app.
const PHONE_PATTERN = /^\+?[0-9 ()-]{7,30}$/;
const NAME_MAX_LENGTH = 100;

/**
 * Edits firstName, lastName and phone (email is not editable: it is the
 * login). PATCH /api/auth/me overwrites all three with whatever it
 * receives, so the form always sends the three of them.
 *
 * `user` must be fresh (see ProfilePage): the initial values are read once,
 * when the form mounts.
 */
function EditProfileForm({ user }) {
  const { updateProfile, isProfileLoading, profileError } = useAuth();

  const [form, setForm] = useState({
    firstName: user.firstName ?? '',
    lastName: user.lastName ?? '',
    phone: user.phone ?? '',
  });
  const [validationError, setValidationError] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');

  const payload = {
    firstName: form.firstName.trim(),
    lastName: form.lastName.trim() || null,
    phone: form.phone.trim() || null,
  };

  const phoneChanged = payload.phone !== (user.phone || null);

  const isDirty =
    payload.firstName !== (user.firstName ?? '') ||
    payload.lastName !== (user.lastName || null) ||
    phoneChanged;

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setSuccessMessage('');
  }

  function validate() {
    if (!payload.firstName) return 'First name is required';
    if (payload.firstName.length > NAME_MAX_LENGTH) {
      return `First name must be at most ${NAME_MAX_LENGTH} characters`;
    }
    if (payload.lastName && payload.lastName.length > NAME_MAX_LENGTH) {
      return `Last name must be at most ${NAME_MAX_LENGTH} characters`;
    }
    // The phone is only validated when it changed, so a legacy number does
    // not block editing the name. Clearing it is not supported: an empty
    // field can only differ from the current value by erasing a number.
    if (phoneChanged) {
      if (!payload.phone) return 'Phone number cannot be empty';
      if (!PHONE_PATTERN.test(payload.phone)) {
        return 'Phone: digits, spaces, +, - and ( ) only (7 to 30 characters)';
      }
    }
    return null;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSuccessMessage('');

    const problem = validate();
    setValidationError(problem);
    if (problem) return;

    try {
      await updateProfile(payload);
      setSuccessMessage('Profile updated.');
    } catch {
      // Surfaced via context `profileError` already.
    }
  }

  return (
    <form className="edit-profile-form" onSubmit={handleSubmit}>
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
        label="Phone"
        name="phone"
        type="tel"
        value={form.phone}
        onChange={handleChange}
      />

      <ErrorMessage message={validationError || profileError} />

      {successMessage && (
        <div style={{ color: 'green', margin: '10px 0' }}>{successMessage}</div>
      )}

      <Button type="submit" disabled={isProfileLoading || !isDirty}>
        {isProfileLoading ? <Spinner /> : 'Save changes'}
      </Button>
    </form>
  );
}

export default EditProfileForm;