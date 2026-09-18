/**
 * Presentational, read-only list of an organization's full
 * subscription history (GET .../subscriptions — includes CANCELLED
 * entries, unlike SubscriptionSummary which only ever shows the
 * current live one). No actions here — history is inherently
 * past-tense, nothing to cancel or change from this view.
 *
 * @param {{ subscriptions: import('../types/billing.types').SubscriptionResponse[] }} props
 */
function SubscriptionHistoryList({ subscriptions }) {
  if (subscriptions.length === 0) {
    return <p className="subscription-history-list__empty">No subscription history yet.</p>;
  }

  return (
    <ul className="subscription-history-list">
      {subscriptions.map((subscription) => (
        <li key={subscription.publicUuid} className="subscription-history-list__item">
          <span className="subscription-history-list__plan-name">{subscription.planName}</span>
          <span className="subscription-history-list__cycle">{subscription.billingCycle}</span>
          <span className="subscription-history-list__status">{subscription.status}</span>
          <span className="subscription-history-list__period">
            {new Date(subscription.currentPeriodStart).toLocaleDateString()} –{' '}
            {new Date(subscription.currentPeriodEnd).toLocaleDateString()}
          </span>
        </li>
      ))}
    </ul>
  );
}

export default SubscriptionHistoryList;