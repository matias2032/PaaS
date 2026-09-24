import { useCallback, useEffect, useState } from 'react';
import {
  createPlan as createPlanApi,
  getPlan as getPlanApi,
  updatePlan as updatePlanApi,
  deactivatePlan as deactivatePlanApi,
  reactivatePlan as reactivatePlanApi,
  archivePlan as archivePlanApi,
  listActivePlans as listActivePlansApi,
  setResourceLimits as setResourceLimitsApi,
  addPrice as addPriceApi,
  subscribe as subscribeApi,
  getCurrentSubscription as getCurrentSubscriptionApi,
  listSubscriptionHistory as listSubscriptionHistoryApi,
  cancelSubscription as cancelSubscriptionApi,
  switchSubscription as switchSubscriptionApi,
} from '../api/billingApi';
import { BillingContext } from './BillingContext';

export function BillingProvider({ children }) {
  // ACTIVE plans catalog (GET /api/plans) — client-facing pricing.
  // Mirrors OrganizationProvider's `organizations` list: fetched via
  // refreshPlans() and kept in sync locally after mutations, instead
  // of refetching the whole list on every admin action.
  const [plans, setPlans] = useState([]);
  const [isPlansLoading, setIsPlansLoading] = useState(false);
  const [plansError, setPlansError] = useState(null);

  // Covers subscribe/getCurrentSubscription/listSubscriptionHistory/
  // cancelSubscription. Subscription data itself is intentionally NOT
  // stored here (same reasoning as OrganizationProvider not storing
  // members at context level): it's scoped to one organization at a
  // time, so the page owns its own local state for it. Only the
  // loading/error of the last subscription action live here, so a
  // SubscriptionPage error never bleeds into PlansPage and vice versa.
  const [isSubscriptionLoading, setIsSubscriptionLoading] = useState(false);
  const [subscriptionError, setSubscriptionError] = useState(null);

  // Same global event OrganizationProvider reacts to (dispatched by
  // httpClient on a real 401) — clears all billing state so a
  // subsequent login never sees stale catalog/error data from a
  // previous session.
  useEffect(() => {
    function handleUnauthorized() {
      setPlans([]);
      setPlansError(null);
      setSubscriptionError(null);
    }

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  // ---- Plans (catalog) ----

  const refreshPlans = useCallback(async () => {
    setIsPlansLoading(true);
    setPlansError(null);
    try {
      const list = await listActivePlansApi();
      setPlans(list);
      return list;
    } catch (err) {
      setPlansError(err?.response?.data?.message || 'Failed to load plans');
      throw err;
    } finally {
      setIsPlansLoading(false);
    }
  }, []);

  // Not backed by `plans` state — GET /api/plans/{publicUuid} returns a
  // plan regardless of status (ACTIVE/INACTIVE/ARCHIVED), unlike the
  // catalog list which is ACTIVE-only. Exposed for pages that need a
  // single plan by uuid directly (e.g. a detail/admin view, or
  // resolving a plan referenced by an old subscription).
  async function fetchPlan(publicUuid) {
    setPlansError(null);
    try {
      return await getPlanApi(publicUuid);
    } catch (err) {
      setPlansError(err?.response?.data?.message || 'Failed to load plan');
      throw err;
    }
  }

  // ---- Plans (admin lifecycle — no UI yet, see handoff §3, but wired
  // here so PlansPage/subscription UI can already read the live
  // catalog while admin UI catches up later) ----

  async function createPlan(data) {
    setIsPlansLoading(true);
    setPlansError(null);
    try {
      const created = await createPlanApi(data);
      setPlans((prev) => [...prev, created]);
      return created;
    } catch (err) {
      setPlansError(err?.response?.data?.message || 'Failed to create plan');
      throw err;
    } finally {
      setIsPlansLoading(false);
    }
  }

  async function updatePlan(publicUuid, data) {
    setIsPlansLoading(true);
    setPlansError(null);
    try {
      const updated = await updatePlanApi(publicUuid, data);
      setPlans((prev) => prev.map((plan) => (plan.publicUuid === publicUuid ? updated : plan)));
      return updated;
    } catch (err) {
      setPlansError(err?.response?.data?.message || 'Failed to update plan');
      throw err;
    } finally {
      setIsPlansLoading(false);
    }
  }

  // Sets status -> "INACTIVE". Kept in the `plans` list (mirrors
  // OrganizationProvider.deactivateOrganization) rather than removed —
  // note the catalog is ACTIVE-only server-side, so in practice a
  // deactivated plan should be dropped from a subsequent refreshPlans()
  // call; this local update just keeps the returned object in sync
  // until that happens.
  async function deactivatePlan(publicUuid) {
    setIsPlansLoading(true);
    setPlansError(null);
    try {
      const updated = await deactivatePlanApi(publicUuid);
      setPlans((prev) => prev.map((plan) => (plan.publicUuid === publicUuid ? updated : plan)));
      return updated;
    } catch (err) {
      setPlansError(err?.response?.data?.message || 'Failed to deactivate plan');
      throw err;
    } finally {
      setIsPlansLoading(false);
    }
  }

  // Mirrors deactivatePlan exactly (INACTIVE -> ACTIVE only).
  async function reactivatePlan(publicUuid) {
    setIsPlansLoading(true);
    setPlansError(null);
    try {
      const updated = await reactivatePlanApi(publicUuid);
      setPlans((prev) => prev.map((plan) => (plan.publicUuid === publicUuid ? updated : plan)));
      return updated;
    } catch (err) {
      setPlansError(err?.response?.data?.message || 'Failed to reactivate plan');
      throw err;
    } finally {
      setIsPlansLoading(false);
    }
  }

  // Terminal — unlike deactivate/reactivate, there is no reverse call.
  // Still merged into `plans` like the others rather than removed, so
  // callers can decide what to do next (e.g. show an "archived" badge)
  // instead of the item just vanishing.
  async function archivePlan(publicUuid) {
    setIsPlansLoading(true);
    setPlansError(null);
    try {
      const updated = await archivePlanApi(publicUuid);
      setPlans((prev) => prev.map((plan) => (plan.publicUuid === publicUuid ? updated : plan)));
      return updated;
    } catch (err) {
      setPlansError(err?.response?.data?.message || 'Failed to archive plan');
      throw err;
    } finally {
      setIsPlansLoading(false);
    }
  }

  // Resource limits and prices are sub-resources of a plan, not stored
  // separately here — on success we merge the returned sub-resource
  // back into the matching plan in `plans` (resourceLimits replaced
  // wholesale; prices appended, matching the backend's "never mutate a
  // price in force, only supersede it" rule from addPrice below).
  async function setResourceLimits(publicUuid, data) {
    setIsPlansLoading(true);
    setPlansError(null);
    try {
      const limits = await setResourceLimitsApi(publicUuid, data);
      setPlans((prev) =>
        prev.map((plan) =>
          plan.publicUuid === publicUuid ? { ...plan, resourceLimits: limits } : plan
        )
      );
      return limits;
    } catch (err) {
      setPlansError(err?.response?.data?.message || 'Failed to update resource limits');
      throw err;
    } finally {
      setIsPlansLoading(false);
    }
  }

  // The backend closes the previous current price of the same
  // billingCycle server-side; mirroring that exactly on the client
  // would require knowing which price it replaced, which the response
  // doesn't tell us. So the local merge here drops any existing price
  // of the same billingCycle and appends the new one — same end state
  // (one current price per cycle) without guessing at the closed
  // price's new effectiveUntil.
  async function addPrice(publicUuid, data) {
    setIsPlansLoading(true);
    setPlansError(null);
    try {
      const newPrice = await addPriceApi(publicUuid, data);
      setPlans((prev) =>
        prev.map((plan) => {
          if (plan.publicUuid !== publicUuid) return plan;
          const otherCyclePrices = plan.prices.filter(
            (price) => price.billingCycle !== newPrice.billingCycle
          );
          return { ...plan, prices: [...otherCyclePrices, newPrice] };
        })
      );
      return newPrice;
    } catch (err) {
      setPlansError(err?.response?.data?.message || 'Failed to add price');
      throw err;
    } finally {
      setIsPlansLoading(false);
    }
  }

  // ---- Subscriptions (per organization — see note on state above:
  // intentionally not cached here, callers own their own local state) ----

  async function subscribe(orgPublicUuid, data) {
    setIsSubscriptionLoading(true);
    setSubscriptionError(null);
    try {
      return await subscribeApi(orgPublicUuid, data);
    } catch (err) {
      setSubscriptionError(err?.response?.data?.message || 'Failed to subscribe');
      throw err;
    } finally {
      setIsSubscriptionLoading(false);
    }
  }

  async function fetchCurrentSubscription(orgPublicUuid) {
    setSubscriptionError(null);
    // Deliberately not surfaced as subscriptionError here: a 404 ("no
    // active subscription") is an expected, common response for this
    // call — SubscriptionPage/PlansPage treat it as "show the plans
    // catalog", not as an error banner. Genuine failures (network,
    // 403) propagate to the caller as-is; no try/catch needed since
    // there's nothing to transform on the way out.
    return getCurrentSubscriptionApi(orgPublicUuid);
  }

  async function fetchSubscriptionHistory(orgPublicUuid) {
    setSubscriptionError(null);
    try {
      return await listSubscriptionHistoryApi(orgPublicUuid);
    } catch (err) {
      setSubscriptionError(err?.response?.data?.message || 'Failed to load subscription history');
      throw err;
    }
  }

  async function cancelSubscription(orgPublicUuid) {
    setIsSubscriptionLoading(true);
    setSubscriptionError(null);
    try {
      return await cancelSubscriptionApi(orgPublicUuid);
    } catch (err) {
      setSubscriptionError(err?.response?.data?.message || 'Failed to cancel subscription');
      throw err;
    } finally {
      setIsSubscriptionLoading(false);
    }
  }

  async function switchSubscription(orgPublicUuid, data) {
  setIsSubscriptionLoading(true);
  setSubscriptionError(null);
  try {
    return await switchSubscriptionApi(orgPublicUuid, data);
  } catch (err) {
    setSubscriptionError(err?.response?.data?.message || 'Failed to switch plan');
    throw err;
  } finally {
    setIsSubscriptionLoading(false);
  }
}

  const value = {
    plans,
    isPlansLoading,
    plansError,
    refreshPlans,
    fetchPlan,
    createPlan,
    updatePlan,
    deactivatePlan,
    reactivatePlan,
    archivePlan,
    setResourceLimits,
    addPrice,
    isSubscriptionLoading,
    subscriptionError,
    subscribe,
    fetchCurrentSubscription,
    fetchSubscriptionHistory,
    cancelSubscription,
    switchSubscription,
  };

  return <BillingContext.Provider value={value}>{children}</BillingContext.Provider>;
}