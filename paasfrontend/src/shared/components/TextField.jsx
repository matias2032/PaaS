/**
 * Generic labeled text input with optional inline error message.
 * Used across all modules for any form field.
 */
function TextField({
  label,
  name,
  type = 'text',
  value,
  onChange,
  error,
  required = false,
}) {
  return (
    <div className="text-field">
      {label && (
        <label className="text-field-label" htmlFor={name}>
          {label}
        </label>
      )}
      <input
        className="text-field-input"
        id={name}
        name={name}
        type={type}
        value={value}
        onChange={onChange}
        required={required}
      />
      {error && <span className="text-field-error">{error}</span>}
    </div>
  );
}

export default TextField;