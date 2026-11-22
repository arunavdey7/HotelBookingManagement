package com.hotelbooking.application.service;

import com.hotelbooking.application.cancellation.CancellationPolicyRegistry;
import com.hotelbooking.application.port.PropertyOnboardingUseCase;
import com.hotelbooking.domain.cancellation.CancellationPolicy;
import com.hotelbooking.domain.exception.ResourceNotFoundException;
import com.hotelbooking.domain.exception.ValidationException;
import com.hotelbooking.domain.inventory.InventoryManager;
import com.hotelbooking.domain.owner.OwnerAccount;
import com.hotelbooking.domain.owner.OwnerRepository;
import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.property.PropertyRepository;
import com.hotelbooking.domain.property.RoomType;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class PropertyOnboardingService implements PropertyOnboardingUseCase {

    private final OwnerRepository ownerRepository;
    private final PropertyRepository propertyRepository;
    private final InventoryManager inventoryManager;
    private final CancellationPolicyRegistry cancellationPolicyRegistry;
    private final Clock clock;

    public PropertyOnboardingService(
            OwnerRepository ownerRepository,
            PropertyRepository propertyRepository,
            InventoryManager inventoryManager,
            CancellationPolicyRegistry cancellationPolicyRegistry,
            Clock clock
    ) {
        this.ownerRepository = ownerRepository;
        this.propertyRepository = propertyRepository;
        this.inventoryManager = inventoryManager;
        this.cancellationPolicyRegistry = cancellationPolicyRegistry;
        this.clock = clock;
    }

    @Override
    public OwnerAccount createOwner(CreateOwnerCommand command) {
        Objects.requireNonNull(command, "CreateOwnerCommand cannot be null");
        String ownerId = "own-" + shortUuid();
        OwnerAccount owner = new OwnerAccount(
                ownerId,
                command.name(),
                command.contactEmail(),
                command.phone(),
                Instant.now(clock)
        );
        return ownerRepository.save(owner);
    }

    @Override
    public OwnerAccount getOwner(String ownerId) {
        return ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("OwnerAccount", ownerId));
    }

    @Override
    public List<Property> getPropertiesByOwner(String ownerId) {
        OwnerAccount owner = getOwner(ownerId);
        return propertyRepository.findByOwnerId(owner.getId());
    }

    @Override
    public Property onboardProperty(OnboardPropertyCommand command) {
        Objects.requireNonNull(command, "OnboardPropertyCommand cannot be null");
        if (command.roomTypes() == null || command.roomTypes().isEmpty()) {
            throw new ValidationException("At least one room type is required when onboarding a property");
        }

        OwnerAccount owner = resolveOrCreateOwner(command);
        CancellationPolicy resolvedPolicy = cancellationPolicyRegistry.resolve(command.cancellationPolicyCode());

        List<RoomType> roomTypes = command.roomTypes().stream()
                .map(this::toDomainRoomType)
                .toList();

        String propertyId = "prop-" + shortUuid();
        Property property = new Property(
                propertyId,
                owner.getId(),
                command.name(),
                command.description(),
                command.propertyType(),
                command.location(),
                command.starRating(),
                command.amenities(),
                roomTypes,
                resolvedPolicy.policyCode(),
                Instant.now(clock)
        );

        for (RoomType roomType : property.getRoomTypes()) {
            inventoryManager.initializeRoomInventory(property.getId(), roomType);
        }

        propertyRepository.save(property);
        owner.registerProperty(property.getId());
        ownerRepository.save(owner);
        return property;
    }

    @Override
    public Property addRoomType(String propertyId, RoomTypeInput roomTypeInput) {
        Objects.requireNonNull(roomTypeInput, "roomTypeInput cannot be null");
        Property property = getProperty(propertyId);
        RoomType newRoomType = toDomainRoomType(roomTypeInput);
        property.addRoomType(newRoomType);
        inventoryManager.initializeRoomInventory(property.getId(), newRoomType);
        return propertyRepository.save(property);
    }

    @Override
    public Property getProperty(String propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId));
    }

    /**
     * Unifies standalone and multi-property onboarding:
     * if an ownerId is provided, the property joins that owner's portfolio;
     * otherwise a dedicated OwnerAccount is created and the exact same portfolio registration runs.
     */
    private OwnerAccount resolveOrCreateOwner(OnboardPropertyCommand command) {
        if (command.ownerId() != null && !command.ownerId().isBlank()) {
            return getOwner(command.ownerId());
        }
        if (command.inlineOwner() != null) {
            return createOwner(command.inlineOwner());
        }
        String fallbackName = (command.name() == null || command.name().isBlank())
                ? "Independent Property Host"
                : command.name().trim() + " Proprietorship";
        String slug = fallbackName.toLowerCase().replaceAll("[^a-z0-9]+", ".");
        return createOwner(new CreateOwnerCommand(
                fallbackName,
                "contact@" + slug + ".hotel",
                ""
        ));
    }

    private RoomType toDomainRoomType(RoomTypeInput input) {
        return new RoomType(
                "room-" + shortUuid(),
                input.name(),
                input.category(),
                input.maxOccupancy(),
                input.totalInventory(),
                input.basePricePerNight(),
                input.amenities()
        );
    }

    private static String shortUuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
