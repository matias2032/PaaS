package com.dev58.paasbackend.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Enables @Scheduled across the application (currently only
// SubscriptionRenewalJob). Kept out of PaasbackendApplication on purpose.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}