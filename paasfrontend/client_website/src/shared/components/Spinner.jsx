/**
 * Generic loading indicator. Used while waiting for API responses
 * (e.g. during login/register submission).
 */
function Spinner() {
  return <div className="spinner" role="status" aria-label="Loading" />;
}

export default Spinner;