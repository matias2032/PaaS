import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { useBilling } from '../hooks/useBilling';
import { useOrganization } from '../../organization/hooks/useOrganization';
import PlanCard from '../components/PlanCard';
import BackButton from '../../../shared/components/BackButton';

function PlansPage() {
  const { orgPublicUuid } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const { organizations, fetchOrganization, fetchMembers } = useOrganization();
  const {
    plans,
    isPlansLoading,
    plansError,
    refreshPlans,
    isSubscriptionLoading,
    subscriptionError,
    subscribe,
    switchSubscription,
    fetchCurrentSubscription,
  } = useBilling();

  const [organization, setOrganization] = useState(
    () => organizations.find((org) => org.publicUuid === orgPublicUuid) || null
  );
  const [currentSubscription, setCurrentSubscription] = useState(null);
  const [members, setMembers] = useState([]);
  const [isLoadingSubscription, setIsLoadingSubscription] = useState(true);
  const [actionError, setActionError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoadingSubscription(true);
      setActionError(null);
      try {
        const cachedOrg = organizations.find((org) => org.publicUuid === orgPublicUuid);
        const [org, subscription, memberList] = await Promise.all([
          cachedOrg ? Promise.resolve(cachedOrg) : fetchOrganization(orgPublicUuid),
          fetchCurrentSubscription(orgPublicUuid).catch((err) => {
            if (err?.response?.status === 404) return null;
            throw err;
          }),
          fetchMembers(orgPublicUuid),
        ]);

        if (!cancelled) {
          setOrganization(org);
          setCurrentSubscription(subscription);
          setMembers(memberList);
        }
      } catch (err) {
        if (!cancelled) {
          setActionError(err?.response?.data?.message || 'Failed to load billing information');
        }
      } finally {
        if (!cancelled) setIsLoadingSubscription(false);
      }
    }

    refreshPlans().catch(() => {
      // Surfaced via context `plansError` already.
    });
    load();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orgPublicUuid]);

  const currentMembership = members.find(
    (member) => member.userPublicUuid === user?.publicUuid
  );
  const isOwner = currentMembership?.roleCode === 'OWNER';

  async function handleSubscribe(planPrice, plan) {
    setActionError(null);

    const isSwitchingPlan = !!currentSubscription;

    if (isSwitchingPlan) {
      // eslint-disable-next-line no-alert
      const confirmed = window.confirm(
        `Switch to "${plan.name}" (${planPrice.billingCycle})? Your current subscription will be replaced immediately.`
      );
      if (!confirmed) return;
    }

    try {
      const newSubscription = isSwitchingPlan
        ? await switchSubscription(orgPublicUuid, { planPricePublicUuid: planPrice.publicUuid })
        : await subscribe(orgPublicUuid, { planPricePublicUuid: planPrice.publicUuid });
      setCurrentSubscription(newSubscription);
      navigate(`/organizations/${orgPublicUuid}/subscription`);
    } catch (err) {
      setActionError(err?.response?.data?.message || 'Failed to update subscription');
    }
  }

  const isBusy = isPlansLoading || isSubscriptionLoading || isLoadingSubscription;
  const displayError = actionError || plansError || subscriptionError;

  const currentPlanPriceUuid = currentSubscription
    ? plans
        .find((plan) => plan.publicUuid === currentSubscription.planPublicUuid)
        ?.prices.find((price) => price.billingCycle === currentSubscription.billingCycle)
        ?.publicUuid ?? null
    : null;

  return (
    <div className="plans-page">
      <BackButton />
      <h1>Plans{organization ? ` for ${organization.name}` : ''}</h1>

      {!isOwner && !isBusy && (
        <p className="plans-page__current-notice">
          Only the organization owner can subscribe or change plans. You can still browse the
          catalog below.
        </p>
      )}

      {currentSubscription && (
        <p className="plans-page__current-notice">
          Currently subscribed to <strong>{currentSubscription.planName}</strong> (
          {currentSubscription.billingCycle}).
        </p>
      )}

      {displayError && <p className="plans-page__error">{displayError}</p>}

      {isBusy && plans.length === 0 && <p>Loading...</p>}

      {!isBusy && plans.length === 0 && !displayError && (
        <p>No plans are available right now.</p>
      )}

      <div className="plans-page__grid">
        {plans.map((plan) => (
          <PlanCard
            key={plan.publicUuid}
            plan={plan}
            currentPlanPriceUuid={currentPlanPriceUuid}
            hasActiveSubscription={!!currentSubscription}
            canSubscribe={isOwner}
            isSubscribing={isSubscriptionLoading}
            onSubscribe={handleSubscribe}
          />
        ))}
      </div>
    </div>
  );
}

export default PlansPage;