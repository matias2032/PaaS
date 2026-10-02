import { useEffect, useState } from 'react';
import ChangePasswordForm from '../components/ChangePasswordForm';
import EditProfileForm from '../components/EditProfileForm';
import { useAuth } from '../hooks/useAuth';
import BackButton from "../../../shared/components/BackButton";

/**
 * Skeleton only — no styling yet.
 *
 * Profile info plus two forms: edit profile (name and phone; email is the
 * login and is not editable) and change password.
 */
function ProfilePage() {
  const { user, refreshProfile } = useAuth();
  const [isLoadingProfile, setIsLoadingProfile] = useState(true);
  const [loadError, setLoadError] = useState(null);

  // The stored session can be out of date (e.g. it predates the phone
  // field), and the edit form overwrites whatever it sends — so the form
  // is only shown once the fresh user is loaded. If loading fails it stays
  // hidden rather than risk wiping a stored value.
  useEffect(() => {
    let cancelled = false;

    refreshProfile()
      .catch((err) => {
        if (!cancelled) {
          setLoadError(err?.response?.data?.message || 'Failed to load profile');
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoadingProfile(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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
          <strong>Phone:</strong> {user?.phone || '—'}
        </p>
        <p>
          <strong>Status:</strong> {user?.status}
        </p>
      </section>

      <section className="profile-page__edit">
        <h2>Edit profile</h2>
        {isLoadingProfile && <p>Loading...</p>}
        {loadError && <p className="profile-page__error">{loadError}</p>}
        {!isLoadingProfile && !loadError && <EditProfileForm user={user} />}
      </section>

      <section className="profile-page__change-password">
        <h2>Change password</h2>
        <ChangePasswordForm />
      </section>
    </div>
  );
}

export default ProfilePage;