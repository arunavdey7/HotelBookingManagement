package com.hotelbooking.infrastructure.config;

import com.hotelbooking.application.pricing.DynamicPricingStrategy;
import com.hotelbooking.application.pricing.FlatRatePricingStrategy;
import com.hotelbooking.application.pricing.PricingProperties;
import com.hotelbooking.application.pricing.PricingRule;
import com.hotelbooking.domain.pricing.PricingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

@Configuration
public class PlatformConfiguration {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    public PricingStrategy pricingStrategy(PricingProperties properties, List<PricingRule> pricingRules) {
        if ("flat".equalsIgnoreCase(properties.mode())) {
            return new FlatRatePricingStrategy();
        }
        return new DynamicPricingStrategy(pricingRules);
    }
}
