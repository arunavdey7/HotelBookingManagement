package com.hotelbooking.api.mapper;

import com.hotelbooking.api.dto.BookingDtos;
import com.hotelbooking.api.dto.DiscoveryDtos;
import com.hotelbooking.api.dto.MoneyDto;
import com.hotelbooking.api.dto.OnboardingDtos;
import com.hotelbooking.api.dto.PaymentDtos;
import com.hotelbooking.application.port.PaymentUseCase;
import com.hotelbooking.application.port.PropertyDiscoveryUseCase;
import com.hotelbooking.application.port.PropertyOnboardingUseCase;
import com.hotelbooking.domain.booking.Booking;
import com.hotelbooking.domain.booking.CancellationDetails;
import com.hotelbooking.domain.common.Location;
import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.owner.OwnerAccount;
import com.hotelbooking.domain.payment.Payment;
import com.hotelbooking.domain.pricing.PriceQuote;
import com.hotelbooking.domain.property.Amenity;
import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.property.PropertyType;
import com.hotelbooking.domain.property.RoomCategory;
import com.hotelbooking.domain.property.RoomType;
import com.hotelbooking.domain.search.AvailableRoomQuote;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class ApiMapper {

    private ApiMapper() {
    }

    public static MoneyDto toMoneyDto(Money money) {
        if (money == null) {
            return null;
        }
        return new MoneyDto(money.amount(), money.currency().getCurrencyCode());
    }

    public static OnboardingDtos.OwnerResponse toOwnerResponse(OwnerAccount owner) {
        return new OnboardingDtos.OwnerResponse(
                owner.getId(),
                owner.getName(),
                owner.getContactEmail(),
                owner.getPhone(),
                owner.getPropertyCount(),
                owner.isMultiPropertyPortfolio(),
                owner.getPortfolioPropertyIds(),
                owner.getCreatedAt()
        );
    }

    public static PropertyOnboardingUseCase.OnboardPropertyCommand toOnboardCommand(
            String overrideOwnerId,
            OnboardingDtos.OnboardPropertyRequest request
    ) {
        String effectiveOwnerId = (overrideOwnerId != null && !overrideOwnerId.isBlank())
                ? overrideOwnerId
                : request.ownerId();
        PropertyOnboardingUseCase.CreateOwnerCommand inlineOwner = request.owner() == null
                ? null
                : new PropertyOnboardingUseCase.CreateOwnerCommand(
                        request.owner().name(),
                        request.owner().contactEmail(),
                        request.owner().phone()
                );

        return new PropertyOnboardingUseCase.OnboardPropertyCommand(
                effectiveOwnerId,
                inlineOwner,
                request.name(),
                request.description(),
                PropertyType.fromString(request.propertyType()),
                toDomainLocation(request.location()),
                request.starRating(),
                toDomainAmenities(request.amenities()),
                request.roomTypes().stream().map(ApiMapper::toRoomTypeInput).toList(),
                request.cancellationPolicyCode()
        );
    }

    public static PropertyOnboardingUseCase.RoomTypeInput toRoomTypeInput(
            OnboardingDtos.RoomTypeRequest request
    ) {
        return new PropertyOnboardingUseCase.RoomTypeInput(
                request.name(),
                RoomCategory.fromString(request.category()),
                request.maxOccupancy(),
                request.totalInventory(),
                Money.of(request.basePricePerNight(), request.currency()),
                toDomainAmenities(request.amenities())
        );
    }

    public static Location toDomainLocation(OnboardingDtos.LocationDto dto) {
        return new Location(
                dto.addressLine(),
                dto.locality(),
                dto.city(),
                dto.state(),
                dto.country(),
                dto.postalCode()
        );
    }

    public static OnboardingDtos.LocationDto toLocationDto(Location location) {
        return new OnboardingDtos.LocationDto(
                location.addressLine(),
                location.locality(),
                location.city(),
                location.state(),
                location.country(),
                location.postalCode()
        );
    }

    public static Set<Amenity> toDomainAmenities(Set<String> rawAmenities) {
        if (rawAmenities == null || rawAmenities.isEmpty()) {
            return Set.of();
        }
        return rawAmenities.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(Amenity::of)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static Set<String> toAmenityStrings(Set<Amenity> amenities) {
        return amenities.stream()
                .map(Amenity::code)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static OnboardingDtos.PropertyResponse toPropertyResponse(Property property) {
        return new OnboardingDtos.PropertyResponse(
                property.getId(),
                property.getOwnerId(),
                property.getName(),
                property.getDescription(),
                property.getPropertyType().name(),
                toLocationDto(property.getLocation()),
                property.getStarRating(),
                toAmenityStrings(property.getAmenities()),
                property.getCancellationPolicyCode(),
                property.getRoomTypes().stream().map(ApiMapper::toRoomTypeResponse).toList(),
                property.getCreatedAt()
        );
    }

    public static OnboardingDtos.RoomTypeResponse toRoomTypeResponse(RoomType roomType) {
        return new OnboardingDtos.RoomTypeResponse(
                roomType.id(),
                roomType.name(),
                roomType.category().name(),
                roomType.maxOccupancy(),
                roomType.totalInventory(),
                toMoneyDto(roomType.basePricePerNight()),
                toAmenityStrings(roomType.amenities())
        );
    }

    public static DiscoveryDtos.PriceQuoteDto toPriceQuoteDto(PriceQuote quote) {
        return new DiscoveryDtos.PriceQuoteDto(
                toMoneyDto(quote.baseAmount()),
                quote.adjustments().stream()
                        .map(adj -> new DiscoveryDtos.PriceAdjustmentDto(adj.description(), toMoneyDto(adj.amount())))
                        .toList(),
                toMoneyDto(quote.totalAmount()),
                toMoneyDto(quote.averageNightlyRate()),
                quote.nights()
        );
    }

    public static DiscoveryDtos.PropertySearchItemDto toSearchItemDto(
            PropertyDiscoveryUseCase.PropertySearchResult result
    ) {
        Property property = result.property();
        return new DiscoveryDtos.PropertySearchItemDto(
                property.getId(),
                property.getOwnerId(),
                property.getName(),
                property.getPropertyType().name(),
                toLocationDto(property.getLocation()),
                property.getStarRating(),
                toAmenityStrings(property.getAmenities()),
                property.getCancellationPolicyCode(),
                toMoneyDto(result.startingFromPerNight()),
                result.availableRooms().stream().map(ApiMapper::toAvailableRoomDto).toList()
        );
    }

    public static DiscoveryDtos.AvailableRoomOptionDto toAvailableRoomDto(AvailableRoomQuote roomQuote) {
        RoomType room = roomQuote.roomType();
        return new DiscoveryDtos.AvailableRoomOptionDto(
                room.id(),
                room.name(),
                room.category().name(),
                room.maxOccupancy(),
                roomQuote.availableRooms(),
                toAmenityStrings(room.amenities()),
                toPriceQuoteDto(roomQuote.priceQuote())
        );
    }

    public static BookingDtos.BookingResponse toBookingResponse(Booking booking) {
        CancellationDetails cancel = booking.getCancellationDetails();
        BookingDtos.CancellationDetailsDto cancelDto = cancel == null
                ? null
                : new BookingDtos.CancellationDetailsDto(
                        cancel.cancelledAt(),
                        cancel.reason(),
                        cancel.policyApplied(),
                        toMoneyDto(cancel.refundAmount()),
                        cancel.refundReference()
                );

        return new BookingDtos.BookingResponse(
                booking.getId(),
                booking.getPropertyId(),
                booking.getRoomTypeId(),
                new BookingDtos.GuestDto(
                        booking.getGuest().fullName(),
                        booking.getGuest().email(),
                        booking.getGuest().phone()
                ),
                booking.getDateRange().checkIn(),
                booking.getDateRange().checkOut(),
                booking.getDateRange().nights(),
                booking.getNumberOfGuests(),
                booking.getRoomsBooked(),
                toPriceQuoteDto(booking.getPriceQuote()),
                booking.getStatus().name(),
                booking.getPaymentId(),
                cancelDto,
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }

    public static PaymentDtos.PaymentResponse toPaymentResponse(PaymentUseCase.PaymentOutcome outcome) {
        return toPaymentResponse(outcome.payment(), outcome.booking().getStatus().name(), outcome.idempotentReplay());
    }

    public static PaymentDtos.PaymentResponse toPaymentResponse(
            Payment payment,
            String bookingStatus,
            boolean idempotentReplay
    ) {
        return new PaymentDtos.PaymentResponse(
                payment.getId(),
                payment.getBookingId(),
                toMoneyDto(payment.getAmount()),
                payment.getMethodType(),
                payment.getStatus().name(),
                payment.getGatewayReference(),
                payment.getFailureReason(),
                toMoneyDto(payment.getRefundedAmount()),
                bookingStatus,
                idempotentReplay,
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
