/**
 * Presentational summary of an organization's current subscription.
 * Like PlanCard, takes data + a callback rather than fetching anything
 * itself — SubscriptionPage owns the fetch (getCurrentSubscription)
 * and decides what "cancel" does next.
 *
 * `canCancel` is an authorization gate the page computes (OWNER role
 * in the organization — cancelSubscription is OWNER-only backend-side,
 * see BillingService.requireOwner) and ANDs with the status-based
 * check below. Without it, a DEVELOPER/VIEWER would see a "Cancel
 * subscription" button that always fails with a 403 on click — the
 * component doesn't know about roles itself, so this stays a plain
 * boolean handed down rather than the component reaching for auth
 * state.
 *
 * @param {{
 *   subscription: import('../types/billing.types').SubscriptionResponse|null,
 *   onCancel?: (subscription: import('../types/billing.types').SubscriptionResponse) => void,
 *   isCancelling?: boolean,
 *   canCancel?: boolean,
 * }} props
 */
function SubscriptionSummary({ subscription, onCancel, isCancelling = false, canCancel = true }) {
  if (!subscription) {
    return <p className="subscription-summary__empty">No active subscription.</p>;
  }

  // Cancel only makes sense while the subscription is still "live"
  // (mirrors the backend's LIVE_SUBSCRIPTION_STATUSES) AND the current
  // user has permission to do it.
  const showCancelButton = subscription.status !== 'CANCELLED' && canCancel;

  return (
    <div className="subscription-summary">
      <h3 className="subscription-summary__plan-name">{subscription.planName}</h3>

      <dl className="subscription-summary__details">
        <dt>Billing cycle</dt>
        <dd>{subscription.billingCycle}</dd>

        <dt>Status</dt>
        <dd>{subscription.status}</dd>

        <dt>Current period</dt>
        <dd>
          {new Date(subscription.currentPeriodStart).toLocaleDateString()} –{' '}
          {new Date(subscription.currentPeriodEnd).toLocaleDateString()}
        </dd>

        <dt>Auto-renew</dt>
        <dd>{subscription.autoRenew ? 'Yes' : 'No'}</dd>

        {subscription.cancelledAt && (
          <>
            <dt>Cancelled at</dt>
            <dd>{new Date(subscription.cancelledAt).toLocaleDateString()}</dd>
          </>
        )}
      </dl>

      {showCancelButton && (
        <button
          type="button"
          className="subscription-summary__cancel"
          disabled={isCancelling}
          onClick={() => onCancel?.(subscription)}
        >
          Cancel subscription
        </button>
      )}
    </div>
  );
}

export default SubscriptionSummary;