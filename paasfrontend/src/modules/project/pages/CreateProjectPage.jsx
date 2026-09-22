import { useParams, useNavigate } from 'react-router-dom';
import ProjectForm from '../components/ProjectForm';
import BackButton from '../../../shared/components/BackButton';

/**
 * POST /api/organizations/{orgPublicUuid}/projects, via ProjectForm.
 * Mirrors CreateOrganizationPage exactly. On success, navigates to the
 * new project's detail page rather than back to the list — same
 * choice CreateOrganizationPage makes.
 */
function CreateProjectPage() {
  const { orgPublicUuid } = useParams();
  const navigate = useNavigate();

  function handleSuccess(project) {
    navigate(`/projects/${project.publicUuid}`);
  }

  return (
    <div className="create-project-page">
      <BackButton />
      <h1>New project</h1>
      <ProjectForm orgPublicUuid={orgPublicUuid} onSuccess={handleSuccess} />
    </div>
  );
}

export default CreateProjectPage;