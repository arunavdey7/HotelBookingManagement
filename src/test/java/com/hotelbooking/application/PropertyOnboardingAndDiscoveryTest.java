package com.hotelbooking.application;

import com.hotelbooking.application.cancellation.CancellationPolicyRegistry;
import com.hotelbooking.application.cancellation.CancellationProperties;
import com.hotelbooking.application.cancellation.FlexibleCancellationPolicy;
import com.hotelbooking.application.cancellation.NonRefundableCancellationPolicy;
import com.hotelbooking.application.cancellation.TieredCancellationPolicy;
import com.hotelbooking.application.port.BookingUseCase;
import com.hotelbooking.application.port.PropertyDiscoveryUseCase;
import com.hotelbooking.application.port.PropertyOnboardingUseCase;
import com.hotelbooking.application.pricing.FlatRatePricingStrategy;
import com.hotelbooking.application.search.filter.AmenitiesSearchFilter;
import com.hotelbooking.application.search.filter.AvailabilitySearchFilter;
import com.hotelbooking.application.search.filter.GuestCapacitySearchFilter;
import com.hotelbooking.application.search.filter.LocationSearchFilter;
import com.hotelbooking.application.search.filter.PriceRangeSearchFilter;
import com.hotelbooking.application.search.filter.PropertyTypeSearchFilter;
import com.hotelbooking.application.search.filter.StarRatingSearchFilter;
import com.hotelbooking.application.service.BookingService;
import com.hotelbooking.application.service.PropertyDiscoveryService;
import com.hotelbooking.application.service.PropertyOnboardingService;
import com.hotelbooking.application.support.KeyedLockManager;
import com.hotelbooking.domain.booking.GuestDetails;
import com.hotelbooking.domain.common.DateRange;
import com.hotelbooking.domain.common.Location;
import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.owner.OwnerAccount;
import com.hotelbooking.domain.property.Amenity;
import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.property.PropertyType;
import com.hotelbooking.domain.property.RoomCategory;
import com.hotelbooking.domain.property.RoomType;
import com.hotelbooking.domain.search.PropertySearchCriteria;
import com.hotelbooking.domain.search.PropertySearchFilter;
import com.hotelbooking.infrastructure.inventory.InMemoryInventoryManager;
import com.hotelbooking.infrastructure.payment.CardPaymentProcessor;
import com.hotelbooking.infrastructure.payment.PaymentProcessorRegistry;
import com.hotelbooking.infrastructure.persistence.InMemoryBookingRepository;
import com.hotelbooking.infrastructure.persistence.InMemoryOwnerRepository;
import com.hotelbooking.infrastructure.persistence.InMemoryPaymentRepository;
import com.hotelbooking.infrastructure.persistence.InMemoryPropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyOnboardingAndDiscoveryTest {

    private PropertyOnboardingService onboardingService;
    private PropertyDiscoveryService discoveryService;
    private BookingService bookingService;
    private InMemoryPropertyRepository propertyRepository;
    private InMemoryInventoryManager inventoryManager;
    private List<PropertySearchFilter> standardFilters;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneOffset.UTC);
        InMemoryOwnerRepository ownerRepository = new InMemoryOwnerRepository();
        propertyRepository = new InMemoryPropertyRepository();
        inventoryManager = new InMemoryInventoryManager();
        CancellationProperties cancelProps = new CancellationProperties("MODERATE_TIERED", 14);
        CancellationPolicyRegistry policyRegistry = new CancellationPolicyRegistry(
                List.of(
                        new FlexibleCancellationPolicy(cancelProps),
                        new TieredCancellationPolicy(cancelProps),
                        new NonRefundableCancellationPolicy()
                ),
                cancelProps
        );

        onboardingService = new PropertyOnboardingService(
                ownerRepository,
                propertyRepository,
                inventoryManager,
                policyRegistry,
                fixedClock
        );

        standardFilters = List.of(
                new LocationSearchFilter(),
                new PropertyTypeSearchFilter(),
                new StarRatingSearchFilter(),
                new AmenitiesSearchFilter(),
                new GuestCapacitySearchFilter(),
                new PriceRangeSearchFilter(),
                new AvailabilitySearchFilter()
        );

        discoveryService = new PropertyDiscoveryService(
                propertyRepository,
                inventoryManager,
                new FlatRatePricingStrategy(),
                standardFilters
        );

        bookingService = new BookingService(
                new InMemoryBookingRepository(),
                propertyRepository,
                new InMemoryPaymentRepository(),
                inventoryManager,
                new FlatRatePricingStrategy(),
                policyRegistry,
                new PaymentProcessorRegistry(List.of(new CardPaymentProcessor())),
                new KeyedLockManager(),
                fixedClock
        );
    }

    @Test
    void supportsBothMultiPropertyHotelChainsAndStandalonePropertiesThroughUnifiedAccountStructure() {
        OwnerAccount chain = onboardingService.createOwner(
                new PropertyOnboardingUseCase.CreateOwnerCommand(
                        "Royal Orchid Group",
                        "ops@royalorchid.in",
                        "080-1234567"
                )
        );

        Property chainMumbai = onboardingService.onboardProperty(samplePropertyCommand(
                chain.getId(), "Royal Orchid Juhu", "Mumbai", "Juhu", 5, 9000, 2
        ));
        Property chainPune = onboardingService.onboardProperty(samplePropertyCommand(
                chain.getId(), "Royal Orchid Koregaon", "Pune", "Koregaon Park", 4, 5500, 3
        ));
        Property standalone = onboardingService.onboardProperty(samplePropertyCommand(
                null, "Hilltop Homestay", "Coorg", "Madikeri", 3, 3200, 1
        ));

        OwnerAccount updatedChain = onboardingService.getOwner(chain.getId());
        OwnerAccount standaloneOwner = onboardingService.getOwner(standalone.getOwnerId());

        assertThat(updatedChain.getPropertyCount()).isEqualTo(2);
        assertThat(updatedChain.isMultiPropertyPortfolio()).isTrue();
        assertThat(onboardingService.getPropertiesByOwner(chain.getId()))
                .extracting(Property::getId)
                .containsExactlyInAnyOrder(chainMumbai.getId(), chainPune.getId());

        assertThat(standaloneOwner.getPropertyCount()).isEqualTo(1);
        assertThat(standaloneOwner.isMultiPropertyPortfolio()).isFalse();
        assertThat(standaloneOwner.ownsProperty(standalone.getId())).isTrue();
    }

    @Test
    void discoveryExcludesPropertyWhenOnlyRemainingAvailableRoomExceedsMaxPriceFilter() {
        Property hotel = onboardingService.onboardProperty(new PropertyOnboardingUseCase.OnboardPropertyCommand(
                null,
                null,
                "Metro Grand",
                "Business & Luxury",
                PropertyType.HOTEL,
                new Location("10 Park St", "MG Road", "Bengaluru", "KA", "India", "560001"),
                5,
                Set.of(Amenity.WIFI, Amenity.GYM),
                List.of(
                        new PropertyOnboardingUseCase.RoomTypeInput(
                                "Standard Room",
                                RoomCategory.STANDARD,
                                2,
                                1,
                                Money.inr(3500),
                                Set.of()
                        ),
                        new PropertyOnboardingUseCase.RoomTypeInput(
                                "Presidential Suite",
                                RoomCategory.SUITE,
                                4,
                                2,
                                Money.inr(18000),
                                Set.of(Amenity.of("PRIVATE_PLUNGE_POOL"))
                        )
                ),
                "FLEXIBLE"
        ));

        DateRange dates = new DateRange(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12));

        // Before booking: Standard Room (3500 INR) matches maxPrice <= 5000 INR
        PropertySearchCriteria budgetSearch = new PropertySearchCriteria(
                "Bengaluru", null, dates, 2, null, Money.inr(5000), Set.of(Amenity.WIFI), 4, null
        );
        assertThat(discoveryService.searchProperties(budgetSearch)).hasSize(1);

        // Book the sole Standard Room for those dates
        RoomType standardRoom = hotel.getRoomTypes().stream()
                .filter(r -> r.name().equals("Standard Room"))
                .findFirst()
                .orElseThrow();
        bookingService.createBooking(new BookingUseCase.CreateBookingCommand(
                hotel.getId(),
                standardRoom.id(),
                new GuestDetails("Rिया Sharma", "riya@example.com", "9111111111"),
                dates,
                2,
                1
        ));

        // Now Standard Room is unavailable, and Presidential Suite (18000 INR) exceeds maxPrice 5000 INR!
        assertThat(discoveryService.searchProperties(budgetSearch)).isEmpty();

        // Searching without the 5000 cap returns the hotel with ONLY the Presidential Suite
        PropertySearchCriteria luxurySearch = new PropertySearchCriteria(
                "Bengaluru", null, dates, 2, null, Money.inr(25000), Set.of(Amenity.of("PRIVATE_PLUNGE_POOL")), 4, null
        );
        List<PropertyDiscoveryUseCase.PropertySearchResult> luxuryResults = discoveryService.searchProperties(luxurySearch);
        assertThat(luxuryResults).hasSize(1);
        assertThat(luxuryResults.get(0).availableRooms())
                .extracting(rq -> rq.roomType().name())
                .containsExactly("Presidential Suite");
    }

    @Test
    void newSearchFilterCanBePluggedInWithoutModifyingDiscoveryService() {
        onboardingService.onboardProperty(new PropertyOnboardingUseCase.OnboardPropertyCommand(
                null, null, "Flexible Stay", "Desc", PropertyType.HOTEL,
                new Location("1 Rd", "Bandra", "Mumbai", "MH", "India", "400050"),
                4, Set.of(Amenity.WIFI),
                List.of(new PropertyOnboardingUseCase.RoomTypeInput(
                        "Standard", RoomCategory.STANDARD, 2, 2, Money.inr(4000), Set.of()
                )),
                "FLEXIBLE"
        ));
        onboardingService.onboardProperty(new PropertyOnboardingUseCase.OnboardPropertyCommand(
                null, null, "Strict Stay", "Desc", PropertyType.HOTEL,
                new Location("2 Rd", "Bandra", "Mumbai", "MH", "India", "400050"),
                4, Set.of(Amenity.WIFI),
                List.of(new PropertyOnboardingUseCase.RoomTypeInput(
                        "Standard", RoomCategory.STANDARD, 2, 2, Money.inr(4000), Set.of()
                )),
                "NON_REFUNDABLE"
        ));

        // Custom filter added externally without altering PropertyDiscoveryService
        PropertySearchFilter freeCancellationOnlyFilter = (context, criteria) ->
                "FLEXIBLE".equals(context.getProperty().getCancellationPolicyCode());

        List<PropertySearchFilter> extendedFilters = new ArrayList<>(standardFilters);
        extendedFilters.add(freeCancellationOnlyFilter);

        PropertyDiscoveryService extendedDiscovery = new PropertyDiscoveryService(
                propertyRepository,
                inventoryManager,
                new FlatRatePricingStrategy(),
                extendedFilters
        );

        PropertySearchCriteria criteria = new PropertySearchCriteria(
                "Mumbai", "Bandra", null, 2, null, null, Set.of(), null, null
        );
        List<PropertyDiscoveryUseCase.PropertySearchResult> results = extendedDiscovery.searchProperties(criteria);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).property().getName()).isEqualTo("Flexible Stay");
    }

    private PropertyOnboardingUseCase.OnboardPropertyCommand samplePropertyCommand(
            String ownerId,
            String name,
            String city,
            String locality,
            int stars,
            double price,
            int inventory
    ) {
        return new PropertyOnboardingUseCase.OnboardPropertyCommand(
                ownerId,
                null,
                name,
                name + " description",
                PropertyType.HOTEL,
                new Location("Main St", locality, city, "State", "India", "000001"),
                stars,
                Set.of(Amenity.WIFI, Amenity.PARKING),
                List.of(new PropertyOnboardingUseCase.RoomTypeInput(
                        "Deluxe",
                        RoomCategory.DELUXE,
                        2,
                        inventory,
                        Money.inr(price),
                        Set.of(Amenity.AIR_CONDITIONING)
                )),
                "MODERATE_TIERED"
        );
    }
}
