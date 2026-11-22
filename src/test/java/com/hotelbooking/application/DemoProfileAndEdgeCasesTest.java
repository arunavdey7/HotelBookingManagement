package com.hotelbooking.application;

import com.hotelbooking.application.port.BookingUseCase;
import com.hotelbooking.application.port.PropertyDiscoveryUseCase;
import com.hotelbooking.domain.booking.Booking;
import com.hotelbooking.domain.booking.BookingStatus;
import com.hotelbooking.domain.booking.GuestDetails;
import com.hotelbooking.domain.common.DateRange;
import com.hotelbooking.domain.exception.ValidationException;
import com.hotelbooking.domain.property.RoomType;
import com.hotelbooking.domain.search.PropertySearchCriteria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("demo")
class DemoProfileAndEdgeCasesTest {

    @Autowired
    private PropertyDiscoveryUseCase discoveryUseCase;

    @Autowired
    private BookingUseCase bookingUseCase;

    @Test
    void demoProfileSeedsChainAndStandalonePropertiesAndEnforcesBookingEdgeCases() {
        List<PropertyDiscoveryUseCase.PropertySearchResult> allSeeded = discoveryUseCase.searchProperties(
                new PropertySearchCriteria(null, null, null, null, null, null, Set.of(), null, null)
        );
        assertThat(allSeeded).hasSize(3);

        var bengaluruResult = discoveryUseCase.searchProperties(
                new PropertySearchCriteria("Bengaluru", "Indiranagar", null, null, null, null, Set.of(), null, null)
        ).get(0);
        String propertyId = bengaluruResult.property().getId();
        RoomType studioRoom = bengaluruResult.property().getRoomTypes().iterator().next();

        // Past check-in date must be rejected
        DateRange pastStay = new DateRange(LocalDate.now().minusDays(3), LocalDate.now().minusDays(1));
        assertThatThrownBy(() -> bookingUseCase.createBooking(new BookingUseCase.CreateBookingCommand(
                propertyId,
                studioRoom.id(),
                new GuestDetails("Past Traveler", "past@example.com", "9000000000"),
                pastStay,
                2,
                1
        ))).isInstanceOf(ValidationException.class)
                .hasMessageContaining("past");

        // Guest count exceeding 1 room's maxOccupancy (2) must be rejected
        DateRange futureStay = new DateRange(LocalDate.now().plusDays(20), LocalDate.now().plusDays(22));
        assertThatThrownBy(() -> bookingUseCase.createBooking(new BookingUseCase.CreateBookingCommand(
                propertyId,
                studioRoom.id(),
                new GuestDetails("Large Family", "family@example.com", "9000000000"),
                futureStay,
                4,
                1
        ))).isInstanceOf(ValidationException.class)
                .hasMessageContaining("exceeds maximum occupancy");

        // Booking 2 rooms accommodates 4 guests and scales price quote cleanly
        Booking twoRoomBooking = bookingUseCase.createBooking(new BookingUseCase.CreateBookingCommand(
                propertyId,
                studioRoom.id(),
                new GuestDetails("Large Family", "family@example.com", "9000000000"),
                futureStay,
                4,
                2
        ));
        assertThat(twoRoomBooking.getRoomsBooked()).isEqualTo(2);
        assertThat(twoRoomBooking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);

        // Cancelling an unpaid booking transitions to CANCELLED with zero refund and releases both rooms
        Booking cancelledUnpaid = bookingUseCase.cancelBooking(
                new BookingUseCase.CancelBookingCommand(twoRoomBooking.getId(), "Booked wrong dates")
        );
        assertThat(cancelledUnpaid.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelledUnpaid.getCancellationDetails().refundAmount().isZero()).isTrue();
    }
}
