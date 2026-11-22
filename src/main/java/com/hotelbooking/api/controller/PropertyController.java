package com.hotelbooking.api.controller;

import com.hotelbooking.api.dto.DiscoveryDtos;
import com.hotelbooking.api.dto.OnboardingDtos;
import com.hotelbooking.api.mapper.ApiMapper;
import com.hotelbooking.application.port.PropertyDiscoveryUseCase;
import com.hotelbooking.application.port.PropertyOnboardingUseCase;
import com.hotelbooking.domain.common.DateRange;
import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.exception.ValidationException;
import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.property.PropertyType;
import com.hotelbooking.domain.search.PropertySearchCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/properties")
@Tag(name = "Properties & Discovery", description = "Onboard properties and search available inventory")
public class PropertyController {

    private final PropertyOnboardingUseCase onboardingUseCase;
    private final PropertyDiscoveryUseCase discoveryUseCase;

    public PropertyController(
            PropertyOnboardingUseCase onboardingUseCase,
            PropertyDiscoveryUseCase discoveryUseCase
    ) {
        this.onboardingUseCase = onboardingUseCase;
        this.discoveryUseCase = discoveryUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Onboard a standalone property or attach to an existing owner portfolio")
    public OnboardingDtos.PropertyResponse onboardProperty(
            @Valid @RequestBody OnboardingDtos.OnboardPropertyRequest request
    ) {
        Property property = onboardingUseCase.onboardProperty(ApiMapper.toOnboardCommand(null, request));
        return ApiMapper.toPropertyResponse(property);
    }

    @GetMapping("/search")
    @Operation(summary = "Discover available properties matching location, dates, guests, and extensible filters")
    public List<DiscoveryDtos.PropertySearchItemDto> searchProperties(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "INR") String currency,
            @RequestParam(required = false) Set<String> amenities,
            @RequestParam(required = false) Integer minStarRating,
            @RequestParam(required = false) String propertyType
    ) {
        if ((checkIn == null) != (checkOut == null)) {
            throw new ValidationException("Both checkIn and checkOut dates must be provided together");
        }
        DateRange dateRange = checkIn != null ? new DateRange(checkIn, checkOut) : null;
        Money minMoney = minPrice != null ? Money.of(minPrice, currency) : null;
        Money maxMoney = maxPrice != null ? Money.of(maxPrice, currency) : null;
        PropertyType type = (propertyType == null || propertyType.isBlank())
                ? null
                : PropertyType.fromString(propertyType);

        PropertySearchCriteria criteria = new PropertySearchCriteria(
                city,
                locality,
                dateRange,
                guests,
                minMoney,
                maxMoney,
                ApiMapper.toDomainAmenities(amenities),
                minStarRating,
                type
        );

        return discoveryUseCase.searchProperties(criteria).stream()
                .map(ApiMapper::toSearchItemDto)
                .toList();
    }

    @GetMapping("/{propertyId}")
    @Operation(summary = "Get property details by id")
    public OnboardingDtos.PropertyResponse getProperty(@PathVariable String propertyId) {
        return ApiMapper.toPropertyResponse(onboardingUseCase.getProperty(propertyId));
    }

    @PostMapping("/{propertyId}/room-types")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a new room type to an existing property")
    public OnboardingDtos.PropertyResponse addRoomType(
            @PathVariable String propertyId,
            @Valid @RequestBody OnboardingDtos.RoomTypeRequest request
    ) {
        Property updated = onboardingUseCase.addRoomType(propertyId, ApiMapper.toRoomTypeInput(request));
        return ApiMapper.toPropertyResponse(updated);
    }
}
