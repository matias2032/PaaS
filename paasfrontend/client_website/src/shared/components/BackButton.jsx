import { useNavigate } from 'react-router-dom';

function BackButton({ label = 'Back' }) {
  const navigate = useNavigate();

  return (
    <button
      type="button"
      onClick={() => navigate(-1)}
      className="back-button"
    >
      ← {label}
    </button>
  );
}

export default BackButton;