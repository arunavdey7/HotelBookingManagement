package com.hotelbooking.domain.search;

import com.hotelbooking.domain.pricing.PriceQuote;
import com.hotelbooking.domain.property.RoomType;

public record AvailableRoomQuote(
        RoomType roomType,
        int availableRooms,
        PriceQuote priceQuote
) {
}
