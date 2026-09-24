import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useBilling } from '../hooks/useBilling';
import { useOrganization } from '../../organization/hooks/useOrganization';
import SubscriptionSummary from '../components/SubscriptionSummary';
import SubscriptionHistoryList from '../components/SubscriptionHistoryList';
import BackButton from '../../../shared/components/BackButton';

/**
 * GET .../subscription (current, may be null — 404 means "none yet")
 * + GET .../subscriptions (full history) + GET .../members (to
 * determine the current user's role in this org — cancelSubscription
 * is OWNER-only backend-side, and the "Cancel" button should never be
 * offered to someone who can't use it).
 *
 * Like OrganizationDetailPage, this is one of the few billing pages
 * allowed to import useAuth() — it needs the current user's own
 * publicUuid to resolve their membership/role, same cross-module read
 * pattern (a public hook from another module, not direct context
 * coupling).
 */
function SubscriptionPage() {
  const { orgPublicUuid } = useParams();
  const { user } = useAuth();
  const { organizations, fetchOrganization, fetchMembers } = useOrganization();
  const {
    isSubscriptionLoading,
    subscriptionError,
    fetchCurrentSubscription,
    fetchSubscriptionHistory,
    cancelSubscription,
  } = useBilling();

  const [organization, setOrganization] = useState(
    () => organizations.find((org) => org.publicUuid === orgPublicUuid) || null
  );
  const [subscription, setSubscription] = useState(null);
  const [history, setHistory] = useState([]);
  const [members, setMembers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setLoadError(null);
      try {
        const cachedOrg = organizations.find((org) => org.publicUuid === orgPublicUuid);
        const [org, current, historyList, memberList] = await Promise.all([
          cachedOrg ? Promise.resolve(cachedOrg) : fetchOrganization(orgPublicUuid),
          fetchCurrentSubscription(orgPublicUuid).catch((err) => {
            if (err?.response?.status === 404) return null;
            throw err;
          }),
          fetchSubscriptionHistory(orgPublicUuid),
          fetchMembers(orgPublicUuid),
        ]);

        if (!cancelled) {
          setOrganization(org);
          setSubscription(current);
          setHistory(historyList);
          setMembers(memberList);
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(err?.response?.data?.message || 'Failed to load subscription');
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orgPublicUuid]);

  const currentMembership = members.find(
    (member) => member.userPublicUuid === user?.publicUuid
  );
  // Single source of truth for the OWNER gate on this page — mirrors
  // BillingService.requireOwner, which subscribe/switchSubscription/
  // cancelSubscription all call.
  const isOwner = currentMembership?.roleCode === 'OWNER';

  async function handleCancel() {
    // eslint-disable-next-line no-alert
    const confirmed = window.confirm(
      `Cancel the subscription to "${subscription.planName}"? This can't be undone from here — you'll need to subscribe again to restore it.`
    );
    if (!confirmed) return;

    try {
      const updated = await cancelSubscription(orgPublicUuid);
      setSubscription(updated);
      const refreshedHistory = await fetchSubscriptionHistory(orgPublicUuid);
      setHistory(refreshedHistory);
    } catch {
      // Surfaced via context `subscriptionError` already.
    }
  }

  if (isLoading) return <p>Loading...</p>;
  if (loadError) return <p className="subscription-page__error">{loadError}</p>;

  return (
    <div className="subscription-page">
      <BackButton />
      <h1>Billing{organization ? ` for ${organization.name}` : ''}</h1>

      {subscriptionError && <p className="subscription-page__error">{subscriptionError}</p>}

      <section className="subscription-page__current">
        <h2>Current subscription</h2>
        <SubscriptionSummary
          subscription={subscription}
          onCancel={handleCancel}
          isCancelling={isSubscriptionLoading}
          canCancel={isOwner}
        />
        {isOwner && (
          // Browsing/subscribing is also OWNER-only backend-side (see
          // BillingService.requireOwner on subscribe/switchSubscription)
          // — hidden here rather than shown-then-blocked on PlansPage,
          // consistent with how the cancel button above is handled.
          <Link
            to={`/organizations/${orgPublicUuid}/plans`}
            className="subscription-page__plans-link"
          >
            {subscription ? 'Change plan' : 'Browse plans'}
          </Link>
        )}
      </section>

      <section className="subscription-page__history">
        <h2>History</h2>
        <SubscriptionHistoryList subscriptions={history} />
      </section>
    </div>
  );
}

export default SubscriptionPage;