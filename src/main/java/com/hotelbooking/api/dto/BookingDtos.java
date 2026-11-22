package com.hotelbooking.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

public final class BookingDtos {

    private BookingDtos() {
    }

    public record GuestDto(
            @NotBlank(message = "Guest full name is required")
            String fullName,
            @NotBlank(message = "Guest email is required")
            @Email(message = "Guest email must be valid")
            String email,
            String phone
    ) {
    }

    public record CreateBookingRequest(
            @NotBlank(message = "propertyId is required")
            String propertyId,
            @NotBlank(message = "roomTypeId is required")
            String roomTypeId,
            @NotNull(message = "guest details are required")
            @Valid
            GuestDto guest,
            @NotNull(message = "checkIn date is required")
            LocalDate checkIn,
            @NotNull(message = "checkOut date is required")
            LocalDate checkOut,
            @Min(value = 1, message = "numberOfGuests must be at least 1")
            int numberOfGuests,
            @Min(value = 1, message = "roomsRequested must be at least 1")
            Integer roomsRequested
    ) {
    }

    public record CancelBookingRequest(
            String reason
    ) {
    }

    public record CancellationDetailsDto(
            Instant cancelledAt,
            String reason,
            String policyApplied,
            MoneyDto refundAmount,
            String refundReference
    ) {
    }

    public record BookingResponse(
            String id,
            String propertyId,
            String roomTypeId,
            GuestDto guest,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            int numberOfGuests,
            int roomsBooked,
            DiscoveryDtos.PriceQuoteDto priceQuote,
            String status,
            String paymentId,
            CancellationDetailsDto cancellationDetails,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
