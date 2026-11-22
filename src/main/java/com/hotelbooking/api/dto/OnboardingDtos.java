package com.hotelbooking.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class OnboardingDtos {

    private OnboardingDtos() {
    }

    public record CreateOwnerRequest(
            @NotBlank(message = "Owner or group name is required")
            String name,
            @NotBlank(message = "Contact email is required")
            @Email(message = "Must be a valid email address")
            String contactEmail,
            String phone
    ) {
    }

    public record OwnerResponse(
            String id,
            String name,
            String contactEmail,
            String phone,
            int propertyCount,
            boolean multiPropertyPortfolio,
            Set<String> portfolioPropertyIds,
            Instant createdAt
    ) {
    }

    public record LocationDto(
            String addressLine,
            @NotBlank(message = "Locality is required")
            String locality,
            @NotBlank(message = "City is required")
            String city,
            String state,
            String country,
            String postalCode
    ) {
    }

    public record RoomTypeRequest(
            @NotBlank(message = "Room type name is required")
            String name,
            String category,
            @Min(value = 1, message = "maxOccupancy must be at least 1")
            int maxOccupancy,
            @Min(value = 1, message = "totalInventory must be at least 1")
            int totalInventory,
            @NotNull(message = "basePricePerNight is required")
            @Positive(message = "basePricePerNight must be positive")
            BigDecimal basePricePerNight,
            String currency,
            Set<String> amenities
    ) {
    }

    public record OnboardPropertyRequest(
            String ownerId,
            @Valid
            CreateOwnerRequest owner,
            @NotBlank(message = "Property name is required")
            String name,
            String description,
            String propertyType,
            @NotNull(message = "Property location is required")
            @Valid
            LocationDto location,
            @Min(value = 1, message = "starRating must be between 1 and 5")
            @Max(value = 5, message = "starRating must be between 1 and 5")
            int starRating,
            Set<String> amenities,
            @NotEmpty(message = "At least one room type is required")
            @Valid
            List<RoomTypeRequest> roomTypes,
            String cancellationPolicyCode
    ) {
    }

    public record RoomTypeResponse(
            String id,
            String name,
            String category,
            int maxOccupancy,
            int totalInventory,
            MoneyDto basePricePerNight,
            Set<String> amenities
    ) {
    }

    public record PropertyResponse(
            String id,
            String ownerId,
            String name,
            String description,
            String propertyType,
            LocationDto location,
            int starRating,
            Set<String> amenities,
            String cancellationPolicyCode,
            List<RoomTypeResponse> roomTypes,
            Instant createdAt
    ) {
    }
}
