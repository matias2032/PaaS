/**
 * Presentational card for one plan in the catalog. Doesn't call
 * useBilling() or any API itself — the owning page (PlansPage) fetches
 * the catalog and passes data + callbacks down.
 *
 * `canSubscribe` is an authorization gate the page computes (OWNER
 * role — subscribe/switchSubscription are OWNER-only backend-side,
 * see BillingService.requireOwner). When false, every row's button is
 * disabled and re-labelled "Owner only" instead of "Subscribe"/"Switch
 * to this plan" — makes the reason visible instead of a button that
 * silently does nothing or fails with a 403 on click.
 *
 * @param {{
 *   plan: import('../types/billing.types').PlanResponse,
 *   currentPlanPriceUuid?: string|null,
 *   hasActiveSubscription?: boolean,
 *   canSubscribe?: boolean,
 *   onSubscribe?: (planPrice: import('../types/billing.types').PlanPriceResponse, plan: import('../types/billing.types').PlanResponse) => void,
 *   isSubscribing?: boolean,
 * }} props
 */
function PlanCard({
  plan,
  currentPlanPriceUuid = null,
  hasActiveSubscription = false,
  canSubscribe = true,
  onSubscribe,
  isSubscribing = false,
}) {
  return (
    <div className="plan-card">
      <div className="plan-card__header">
        <h3 className="plan-card__name">{plan.name}</h3>
        {plan.description && <p className="plan-card__description">{plan.description}</p>}
      </div>

      {plan.resourceLimits && (
        <ul className="plan-card__limits">
          <li>{plan.resourceLimits.cpuLimit} CPU</li>
          <li>{plan.resourceLimits.memoryLimitMb} MB memory</li>
          <li>{plan.resourceLimits.storageLimitMb} MB storage</li>
          <li>{plan.resourceLimits.maxProjects} projects</li>
          <li>{plan.resourceLimits.maxServices} services</li>
          <li>{plan.resourceLimits.maxDomains} domains</li>
        </ul>
      )}

      <ul className="plan-card__prices">
        {plan.prices.map((price) => {
          const isCurrent = price.publicUuid === currentPlanPriceUuid;
          const label = isCurrent
            ? 'Current plan'
            : !canSubscribe
              ? 'Owner only'
              : hasActiveSubscription
                ? 'Switch to this plan'
                : 'Subscribe';

          return (
            <li key={price.publicUuid} className="plan-card__price-row">
              <span className="plan-card__price-cycle">{price.billingCycle}</span>
              <span className="plan-card__price-amount">
                {price.amount} {price.currency}
              </span>

              <button
                type="button"
                className="plan-card__subscribe"
                disabled={isCurrent || isSubscribing || !canSubscribe}
                onClick={() => onSubscribe?.(price, plan)}
              >
                {label}
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

export default PlanCard;