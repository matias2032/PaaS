/**
 * Generic error banner for form-level or request-level errors
 * (e.g. "Invalid email or password" coming back from the API).
 * For field-level errors, use TextField's own `error` prop instead.
 */
function ErrorMessage({ message }) {
  if (!message) return null;

  return <div className="error-message">{message}</div>;
}

export default ErrorMessage;