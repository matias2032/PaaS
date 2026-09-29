package com.dev58.paasbackend.billing.scheduler;

import com.dev58.paasbackend.billing.entity.Subscription;
import com.dev58.paasbackend.billing.repository.SubscriptionRepository;
import com.dev58.paasbackend.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Daily renewal of ACTIVE, auto-renewing subscriptions whose period ended.
 *
 * Deliberately NOT @Transactional: each renewSubscription() call runs in
 * its own transaction (BillingService is a separate bean, so the proxy
 * applies). One failing subscription therefore rolls back only itself and
 * never blocks the others.
 *
 * TODO(multi-instance): assumes a single application instance. With more
 * than one, two jobs could pick the same subscription; the "not due yet"
 * guard in renewSubscription() only narrows that. Add a distributed lock
 * (e.g. ShedLock) or a row lock before scaling out.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionRenewalJob {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final SubscriptionRepository subscriptionRepository;
    private final BillingService billingService;

    // Default 03:00 every day; override with app.billing.renewal-cron.
    @Scheduled(cron = "${app.billing.renewal-cron:0 0 3 * * *}")
    public void renewDueSubscriptions() {
        List<Subscription> due = subscriptionRepository
                .findByStatusAndAutoRenewTrueAndCurrentPeriodEndLessThanEqual(STATUS_ACTIVE, OffsetDateTime.now());

        int renewed = 0;
        int failed = 0;

        for (Subscription subscription : due) {
            try {
                billingService.renewSubscription(subscription.getPublicUuid());
                renewed++;
            } catch (Exception ex) {
                failed++;
                log.error("Renewal failed for subscription {}", subscription.getPublicUuid(), ex);
            }
        }

        log.info("Subscription renewal run finished: {} due, {} processed, {} failed",
                due.size(), renewed, failed);
    }
}