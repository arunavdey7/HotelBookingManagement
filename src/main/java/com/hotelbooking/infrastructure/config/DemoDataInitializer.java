package com.hotelbooking.infrastructure.config;

import com.hotelbooking.application.port.PropertyOnboardingUseCase;
import com.hotelbooking.domain.common.Location;
import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.owner.OwnerAccount;
import com.hotelbooking.domain.property.Amenity;
import com.hotelbooking.domain.property.PropertyType;
import com.hotelbooking.domain.property.RoomCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Optional seed data enabled via --spring.profiles.active=demo for quick interactive exploration.
 */
@Component
@Profile("demo")
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final PropertyOnboardingUseCase onboardingUseCase;

    public DemoDataInitializer(PropertyOnboardingUseCase onboardingUseCase) {
        this.onboardingUseCase = onboardingUseCase;
    }

    @Override
    public void run(String... args) {
        OwnerAccount chain = onboardingUseCase.createOwner(
                new PropertyOnboardingUseCase.CreateOwnerCommand(
                        "Grand Horizon Hospitality Group",
                        "portfolio@grandhorizon.com",
                        "+91-22-40001000"
                )
        );

        onboardingUseCase.onboardProperty(new PropertyOnboardingUseCase.OnboardPropertyCommand(
                chain.getId(),
                null,
                "Grand Horizon Marine Drive",
                "Luxury sea-facing flagship hotel in South Mumbai",
                PropertyType.HOTEL,
                new Location("24 Marine Drive", "Nariman Point", "Mumbai", "Maharashtra", "India", "400021"),
                5,
                Set.of(Amenity.WIFI, Amenity.POOL, Amenity.SPA, Amenity.GYM, Amenity.BREAKFAST),
                List.of(
                        new PropertyOnboardingUseCase.RoomTypeInput(
                                "Deluxe City View",
                                RoomCategory.DELUXE,
                                2,
                                4,
                                Money.inr(8500),
                                Set.of(Amenity.AIR_CONDITIONING)
                        ),
                        new PropertyOnboardingUseCase.RoomTypeInput(
                                "Arabian Sea Presidential Suite",
                                RoomCategory.SUITE,
                                4,
                                2,
                                Money.inr(22000),
                                Set.of(Amenity.AIR_CONDITIONING, Amenity.of("JACUZZI"))
                        )
                ),
                "MODERATE_TIERED"
        ));

        onboardingUseCase.onboardProperty(new PropertyOnboardingUseCase.OnboardPropertyCommand(
                chain.getId(),
                null,
                "Grand Horizon Candolim Beach Resort",
                "Beachfront tropical resort in North Goa",
                PropertyType.RESORT,
                new Location("Beach Road", "Candolim", "Goa", "Goa", "India", "403515"),
                5,
                Set.of(Amenity.WIFI, Amenity.POOL, Amenity.PARKING, Amenity.PET_FRIENDLY),
                List.of(
                        new PropertyOnboardingUseCase.RoomTypeInput(
                                "Garden Cottage",
                                RoomCategory.DELUXE,
                                3,
                                5,
                                Money.inr(6500),
                                Set.of(Amenity.AIR_CONDITIONING)
                        )
                ),
                "FLEXIBLE"
        ));

        onboardingUseCase.onboardProperty(new PropertyOnboardingUseCase.OnboardPropertyCommand(
                null,
                new PropertyOnboardingUseCase.CreateOwnerCommand(
                        "Asha Nair",
                        "asha@indiranagarloft.in",
                        "+91-80-25251234"
                ),
                "The Courtyard Boutique Stay",
                "Quiet standalone boutique stay in Indiranagar",
                PropertyType.BOUTIQUE,
                new Location("12th Main Road", "Indiranagar", "Bengaluru", "Karnataka", "India", "560038"),
                4,
                Set.of(Amenity.WIFI, Amenity.BREAKFAST, Amenity.PARKING),
                List.of(
                        new PropertyOnboardingUseCase.RoomTypeInput(
                                "Courtyard Studio",
                                RoomCategory.STANDARD,
                                2,
                                2,
                                Money.inr(4200),
                                Set.of(Amenity.AIR_CONDITIONING)
                        )
                ),
                "MODERATE_TIERED"
        ));

        log.info("Demo profile active: seeded 1 multi-property chain (2 hotels) and 1 standalone boutique stay.");
    }
}
