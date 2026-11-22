package com.hotelbooking.domain.owner;

import com.hotelbooking.domain.exception.ValidationException;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Unified ownership account modelling both single-property hosts and multi-property hotel chains/groups.
 * A standalone property owner is naturally an OwnerAccount whose portfolio contains a single property,
 * avoiding separate single-vs-chain code paths while supporting seamless expansion into a multi-property group.
 */
public class OwnerAccount {

    private final String id;
    private final String name;
    private final String contactEmail;
    private final String phone;
    private final Set<String> portfolioPropertyIds;
    private final Instant createdAt;

    public OwnerAccount(String id, String name, String contactEmail, String phone, Instant createdAt) {
        if (id == null || id.isBlank()) {
            throw new ValidationException("Owner id cannot be blank");
        }
        if (name == null || name.isBlank()) {
            throw new ValidationException("Owner or group name is required");
        }
        if (contactEmail == null || contactEmail.isBlank() || !contactEmail.contains("@")) {
            throw new ValidationException("A valid owner contact email is required");
        }
        this.id = id.trim();
        this.name = name.trim();
        this.contactEmail = contactEmail.trim().toLowerCase();
        this.phone = phone == null ? "" : phone.trim();
        this.portfolioPropertyIds = new LinkedHashSet<>();
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    public synchronized void registerProperty(String propertyId) {
        if (propertyId == null || propertyId.isBlank()) {
            throw new ValidationException("Property id cannot be blank");
        }
        portfolioPropertyIds.add(propertyId.trim());
    }

    public synchronized boolean ownsProperty(String propertyId) {
        return propertyId != null && portfolioPropertyIds.contains(propertyId.trim());
    }

    public synchronized Set<String> getPortfolioPropertyIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(portfolioPropertyIds));
    }

    public synchronized int getPropertyCount() {
        return portfolioPropertyIds.size();
    }

    public synchronized boolean isMultiPropertyPortfolio() {
        return portfolioPropertyIds.size() > 1;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getPhone() {
        return phone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
