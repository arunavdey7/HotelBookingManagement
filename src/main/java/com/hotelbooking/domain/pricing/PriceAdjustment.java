package com.hotelbooking.domain.pricing;

import com.hotelbooking.domain.common.Money;

public record PriceAdjustment(String description, Money amount) {
}
