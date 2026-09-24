import { createContext } from 'react';

/**
 * Holds: the ACTIVE plans catalog (client-facing pricing) plus CRUD/
 * lifecycle operations over plans (admin-facing, even though there is
 * no UI for it yet — see handoff §3), and subscription operations
 * scoped to a given organization.
 *
 * Unlike OrganizationContext, `error` is NOT a single shared field
 * here — deliberately, per the handoff's note on OrganizationProvider's
 * known tech debt. Plans and subscriptions are different operation
 * domains (admin catalog management vs. a given org's subscription
 * lifecycle) that can legitimately fail independently and at the same
 * time (e.g. PlansPage failing to load the catalog shouldn't blank out
 * a subscribe-button error on SubscriptionPage). See plansError /
 * subscriptionError below.
 */
export const BillingContext = createContext(undefined);