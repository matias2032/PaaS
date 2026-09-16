import { useNavigate } from 'react-router-dom';
import OrganizationForm from '../components/OrganizationForm';

/**
 * POST /api/organizations, via OrganizationForm in create mode
 * (no `organization` prop passed = create, per OrganizationForm's
 * own convention). On success, navigates to the new org's detail page.
 */
function CreateOrganizationPage() {
  const navigate = useNavigate();

  function handleSuccess(organization) {
    navigate(`/organizations/${organization.publicUuid}`);
  }

  return (
    <div className="create-organization-page">
      <h1>New organization</h1>
      <OrganizationForm onSuccess={handleSuccess} />
    </div>
  );
}

export default CreateOrganizationPage;