package com.hotelbooking.api.controller;

import com.hotelbooking.api.dto.BookingDtos;
import com.hotelbooking.api.mapper.ApiMapper;
import com.hotelbooking.application.port.BookingUseCase;
import com.hotelbooking.domain.booking.Booking;
import com.hotelbooking.domain.booking.GuestDetails;
import com.hotelbooking.domain.common.DateRange;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings & Cancellations", description = "Reserve room inventory, inspect booking lifecycle, and cancel bookings")
public class BookingController {

    private final BookingUseCase bookingUseCase;

    public BookingController(BookingUseCase bookingUseCase) {
        this.bookingUseCase = bookingUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book a property room type for a date range and guest count")
    public BookingDtos.BookingResponse createBooking(
            @Valid @RequestBody BookingDtos.CreateBookingRequest request
    ) {
        Booking booking = bookingUseCase.createBooking(new BookingUseCase.CreateBookingCommand(
                request.propertyId(),
                request.roomTypeId(),
                new GuestDetails(
                        request.guest().fullName(),
                        request.guest().email(),
                        request.guest().phone()
                ),
                new DateRange(request.checkIn(), request.checkOut()),
                request.numberOfGuests(),
                request.roomsRequested()
        ));
        return ApiMapper.toBookingResponse(booking);
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking details and current lifecycle status")
    public BookingDtos.BookingResponse getBooking(@PathVariable String bookingId) {
        return ApiMapper.toBookingResponse(bookingUseCase.getBooking(bookingId));
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel an existing booking, apply refund policy, and release held inventory")
    public BookingDtos.BookingResponse cancelBooking(
            @PathVariable String bookingId,
            @RequestBody(required = false) BookingDtos.CancelBookingRequest request
    ) {
        String reason = request == null ? null : request.reason();
        Booking cancelled = bookingUseCase.cancelBooking(
                new BookingUseCase.CancelBookingCommand(bookingId, reason)
        );
        return ApiMapper.toBookingResponse(cancelled);
    }
}
