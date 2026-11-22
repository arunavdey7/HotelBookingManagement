package com.hotelbooking.infrastructure.inventory;

import com.hotelbooking.domain.common.DateRange;
import com.hotelbooking.domain.exception.InsufficientInventoryException;
import com.hotelbooking.domain.exception.ResourceNotFoundException;
import com.hotelbooking.domain.exception.ValidationException;
import com.hotelbooking.domain.inventory.InventoryManager;
import com.hotelbooking.domain.property.RoomType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe, fine-grained per-(propertyId, roomTypeId) room inventory ledger.
 * Uses a per-room-type ReentrantReadWriteLock so concurrent bookings on the same room type
 * execute atomic check-and-reserve across all nights of [checkIn, checkOut), while independent
 * properties and room types proceed concurrently without lock contention.
 */
@Component
public class InMemoryInventoryManager implements InventoryManager {

    private record RoomKey(String propertyId, String roomTypeId) {
    }

    private final Map<RoomKey, RoomPool> pools = new ConcurrentHashMap<>();

    @Override
    public void initializeRoomInventory(String propertyId, RoomType roomType) {
        Objects.requireNonNull(propertyId, "propertyId cannot be null");
        Objects.requireNonNull(roomType, "roomType cannot be null");
        RoomKey key = new RoomKey(propertyId.trim(), roomType.id());
        pools.put(key, new RoomPool(roomType.totalInventory()));
    }

    @Override
    public int getAvailableRooms(String propertyId, String roomTypeId, DateRange dateRange) {
        RoomPool pool = getPoolOrThrow(propertyId, roomTypeId);
        Objects.requireNonNull(dateRange, "dateRange cannot be null");
        return pool.availableAcross(dateRange.stayNights());
    }

    @Override
    public boolean isAvailable(String propertyId, String roomTypeId, DateRange dateRange, int roomsNeeded) {
        if (roomsNeeded < 1) {
            throw new ValidationException("roomsNeeded must be at least 1");
        }
        return getAvailableRooms(propertyId, roomTypeId, dateRange) >= roomsNeeded;
    }

    @Override
    public void reserve(String propertyId, String roomTypeId, DateRange dateRange, int roomsNeeded) {
        if (roomsNeeded < 1) {
            throw new ValidationException("roomsNeeded must be at least 1");
        }
        Objects.requireNonNull(dateRange, "dateRange cannot be null");
        RoomPool pool = getPoolOrThrow(propertyId, roomTypeId);
        pool.reserveAtomically(propertyId, roomTypeId, dateRange.stayNights(), roomsNeeded);
    }

    @Override
    public void release(String propertyId, String roomTypeId, DateRange dateRange, int roomsToRelease) {
        if (roomsToRelease < 1) {
            throw new ValidationException("roomsToRelease must be at least 1");
        }
        Objects.requireNonNull(dateRange, "dateRange cannot be null");
        RoomPool pool = getPoolOrThrow(propertyId, roomTypeId);
        pool.releaseAtomically(dateRange.stayNights(), roomsToRelease);
    }

    private RoomPool getPoolOrThrow(String propertyId, String roomTypeId) {
        if (propertyId == null || roomTypeId == null) {
            throw new ResourceNotFoundException("RoomInventory", String.valueOf(roomTypeId));
        }
        RoomPool pool = pools.get(new RoomKey(propertyId.trim(), roomTypeId.trim()));
        if (pool == null) {
            throw new ResourceNotFoundException("RoomInventory", propertyId + ":" + roomTypeId);
        }
        return pool;
    }

    private static final class RoomPool {
        private final int totalCapacity;
        private final Map<LocalDate, Integer> reservedByNight = new HashMap<>();
        private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

        private RoomPool(int totalCapacity) {
            this.totalCapacity = totalCapacity;
        }

        private int availableAcross(List<LocalDate> stayNights) {
            rwLock.readLock().lock();
            try {
                int minAvailable = totalCapacity;
                for (LocalDate night : stayNights) {
                    int occupied = reservedByNight.getOrDefault(night, 0);
                    minAvailable = Math.min(minAvailable, Math.max(0, totalCapacity - occupied));
                }
                return minAvailable;
            } finally {
                rwLock.readLock().unlock();
            }
        }

        private void reserveAtomically(
                String propertyId,
                String roomTypeId,
                List<LocalDate> stayNights,
                int roomsNeeded
        ) {
            rwLock.writeLock().lock();
            try {
                for (LocalDate night : stayNights) {
                    int occupied = reservedByNight.getOrDefault(night, 0);
                    int remaining = totalCapacity - occupied;
                    if (remaining < roomsNeeded) {
                        throw new InsufficientInventoryException(
                                "Insufficient inventory for property " + propertyId
                                        + ", roomType " + roomTypeId
                                        + " on night " + night
                                        + " (requested: " + roomsNeeded + ", available: " + Math.max(0, remaining) + ")"
                        );
                    }
                }
                for (LocalDate night : stayNights) {
                    reservedByNight.merge(night, roomsNeeded, Integer::sum);
                }
            } finally {
                rwLock.writeLock().unlock();
            }
        }

        private void releaseAtomically(List<LocalDate> stayNights, int roomsToRelease) {
            rwLock.writeLock().lock();
            try {
                for (LocalDate night : stayNights) {
                    reservedByNight.computeIfPresent(night, (date, occupied) -> {
                        int updated = occupied - roomsToRelease;
                        return updated <= 0 ? null : updated;
                    });
                }
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }
}
