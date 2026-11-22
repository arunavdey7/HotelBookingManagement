package com.hotelbooking.domain.inventory;

import com.hotelbooking.domain.common.DateRange;
import com.hotelbooking.domain.property.RoomType;

/**
 * Port managing per-night room inventory and atomic reservation/release semantics.
 * Implementations guarantee thread safety against concurrent bookings for overlapping dates.
 */
public interface InventoryManager {

    void initializeRoomInventory(String propertyId, RoomType roomType);

    int getAvailableRooms(String propertyId, String roomTypeId, DateRange dateRange);

    boolean isAvailable(String propertyId, String roomTypeId, DateRange dateRange, int roomsNeeded);

    void reserve(String propertyId, String roomTypeId, DateRange dateRange, int roomsNeeded);

    void release(String propertyId, String roomTypeId, DateRange dateRange, int roomsToRelease);
}
