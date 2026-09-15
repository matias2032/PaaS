/**
 * Generic reusable button. Used across all modules — do not add
 * module-specific logic here (e.g. auth-only behavior belongs in
 * modules/auth/components instead).
 */
function Button({
  children,
  onClick,
  type = 'button',
  variant = 'primary',
  disabled = false,
}) {
  return (
    <button
      type={type}
      className={`btn btn-${variant}`}
      onClick={onClick}
      disabled={disabled}
    >
      {children}
    </button>
  );
}

export default Button;