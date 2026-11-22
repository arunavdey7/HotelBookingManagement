package com.hotelbooking.api.dto;

import java.util.List;
import java.util.Set;

public final class DiscoveryDtos {

    private DiscoveryDtos() {
    }

    public record PriceAdjustmentDto(
            String description,
            MoneyDto amount
    ) {
    }

    public record PriceQuoteDto(
            MoneyDto baseAmount,
            List<PriceAdjustmentDto> adjustments,
            MoneyDto totalAmount,
            MoneyDto averageNightlyRate,
            int nights
    ) {
    }

    public record AvailableRoomOptionDto(
            String roomTypeId,
            String name,
            String category,
            int maxOccupancy,
            int availableRooms,
            Set<String> amenities,
            PriceQuoteDto priceQuote
    ) {
    }

    public record PropertySearchItemDto(
            String propertyId,
            String ownerId,
            String name,
            String propertyType,
            OnboardingDtos.LocationDto location,
            int starRating,
            Set<String> amenities,
            String cancellationPolicyCode,
            MoneyDto startingFromPerNight,
            List<AvailableRoomOptionDto> availableRooms
    ) {
    }
}
