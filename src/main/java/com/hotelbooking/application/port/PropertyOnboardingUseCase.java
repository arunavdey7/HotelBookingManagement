package com.hotelbooking.application.port;

import com.hotelbooking.domain.common.Location;
import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.owner.OwnerAccount;
import com.hotelbooking.domain.property.Amenity;
import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.property.PropertyType;
import com.hotelbooking.domain.property.RoomCategory;

import java.util.List;
import java.util.Set;

public interface PropertyOnboardingUseCase {

    OwnerAccount createOwner(CreateOwnerCommand command);

    OwnerAccount getOwner(String ownerId);

    List<Property> getPropertiesByOwner(String ownerId);

    Property onboardProperty(OnboardPropertyCommand command);

    Property addRoomType(String propertyId, RoomTypeInput roomTypeInput);

    Property getProperty(String propertyId);

    record CreateOwnerCommand(
            String name,
            String contactEmail,
            String phone
    ) {
    }

    record RoomTypeInput(
            String name,
            RoomCategory category,
            int maxOccupancy,
            int totalInventory,
            Money basePricePerNight,
            Set<Amenity> amenities
    ) {
    }

    record OnboardPropertyCommand(
            String ownerId,
            CreateOwnerCommand inlineOwner,
            String name,
            String description,
            PropertyType propertyType,
            Location location,
            int starRating,
            Set<Amenity> amenities,
            List<RoomTypeInput> roomTypes,
            String cancellationPolicyCode
    ) {
    }
}
