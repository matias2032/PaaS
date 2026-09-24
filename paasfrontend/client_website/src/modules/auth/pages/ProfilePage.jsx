import ChangePasswordForm from '../components/ChangePasswordForm';
import { useAuth } from '../hooks/useAuth';
import BackButton from "../../../shared/components/BackButton";

/**
 * Skeleton only — no styling yet.
 *
 * Read-only profile info for now. Editing firstName/lastName/phone via
 * updateProfile() is already wired up in AuthProvider but has no form
 * yet — TODO once requested.
 */
function ProfilePage() {
  const { user } = useAuth();

  return (
    <div className="profile-page">
       <BackButton />
      <h1>Profile</h1>

      <section className="profile-page__info">
        <p>
          <strong>Name:</strong> {user?.firstName} {user?.lastName}
        </p>
        <p>
          <strong>Email:</strong> {user?.email}
        </p>
        <p>
          <strong>Status:</strong> {user?.status}
        </p>
      </section>

      <section className="profile-page__change-password">
        <h2>Change password</h2>
        <ChangePasswordForm />
      </section>
    </div>
  );
}

export default ProfilePage;