import { useParams, useNavigate } from 'react-router-dom';
import ServiceForm from '../components/ServiceForm';
import BackButton from '../../../shared/components/BackButton';

/**
 * POST /api/projects/{projectPublicUuid}/services, via ServiceForm.
 * Mirrors CreateProjectPage exactly. On success, navigates to the
 * new service's detail page rather than back to the list — same
 * choice CreateProjectPage makes.
 */
function CreateServicePage() {
  const { projectPublicUuid } = useParams();
  const navigate = useNavigate();

  function handleSuccess(service) {
    navigate(`/services/${service.publicUuid}`);
  }

  return (
    <div className="create-service-page">
      <BackButton />
      <h1>New service</h1>
      <ServiceForm projectPublicUuid={projectPublicUuid} onSuccess={handleSuccess} />
    </div>
  );
}

export default CreateServicePage;