package com.hotelbooking.api.controller;

import com.hotelbooking.api.dto.OnboardingDtos;
import com.hotelbooking.api.mapper.ApiMapper;
import com.hotelbooking.application.port.PropertyOnboardingUseCase;
import com.hotelbooking.domain.owner.OwnerAccount;
import com.hotelbooking.domain.property.Property;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/owners")
@Tag(name = "Owners & Portfolios", description = "Manage single-property hosts and multi-property hotel groups/chains")
public class OwnerController {

    private final PropertyOnboardingUseCase onboardingUseCase;

    public OwnerController(PropertyOnboardingUseCase onboardingUseCase) {
        this.onboardingUseCase = onboardingUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an owner account (individual proprietor or hotel chain/group)")
    public OnboardingDtos.OwnerResponse createOwner(
            @Valid @RequestBody OnboardingDtos.CreateOwnerRequest request
    ) {
        OwnerAccount owner = onboardingUseCase.createOwner(
                new PropertyOnboardingUseCase.CreateOwnerCommand(
                        request.name(),
                        request.contactEmail(),
                        request.phone()
                )
        );
        return ApiMapper.toOwnerResponse(owner);
    }

    @GetMapping("/{ownerId}")
    @Operation(summary = "Retrieve owner account and portfolio summary")
    public OnboardingDtos.OwnerResponse getOwner(@PathVariable String ownerId) {
        return ApiMapper.toOwnerResponse(onboardingUseCase.getOwner(ownerId));
    }

    @PostMapping("/{ownerId}/properties")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Onboard a property into an existing owner's portfolio")
    public OnboardingDtos.PropertyResponse onboardPropertyUnderOwner(
            @PathVariable String ownerId,
            @Valid @RequestBody OnboardingDtos.OnboardPropertyRequest request
    ) {
        Property property = onboardingUseCase.onboardProperty(
                ApiMapper.toOnboardCommand(ownerId, request)
        );
        return ApiMapper.toPropertyResponse(property);
    }

    @GetMapping("/{ownerId}/properties")
    @Operation(summary = "List all properties managed under an owner account")
    public List<OnboardingDtos.PropertyResponse> listOwnerProperties(@PathVariable String ownerId) {
        return onboardingUseCase.getPropertiesByOwner(ownerId).stream()
                .map(ApiMapper::toPropertyResponse)
                .toList();
    }
}
