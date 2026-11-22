package com.hotelbooking.application.service;

import com.hotelbooking.application.port.PropertyDiscoveryUseCase;
import com.hotelbooking.domain.common.Money;
import com.hotelbooking.domain.inventory.InventoryManager;
import com.hotelbooking.domain.pricing.PriceQuote;
import com.hotelbooking.domain.pricing.PricingStrategy;
import com.hotelbooking.domain.property.Property;
import com.hotelbooking.domain.property.PropertyRepository;
import com.hotelbooking.domain.property.RoomType;
import com.hotelbooking.domain.search.AvailableRoomQuote;
import com.hotelbooking.domain.search.PropertySearchContext;
import com.hotelbooking.domain.search.PropertySearchCriteria;
import com.hotelbooking.domain.search.PropertySearchFilter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class PropertyDiscoveryService implements PropertyDiscoveryUseCase {

    private final PropertyRepository propertyRepository;
    private final InventoryManager inventoryManager;
    private final PricingStrategy pricingStrategy;
    private final List<PropertySearchFilter> filters;

    public PropertyDiscoveryService(
            PropertyRepository propertyRepository,
            InventoryManager inventoryManager,
            PricingStrategy pricingStrategy,
            List<PropertySearchFilter> filters
    ) {
        this.propertyRepository = propertyRepository;
        this.inventoryManager = inventoryManager;
        this.pricingStrategy = pricingStrategy;
        this.filters = filters.stream()
                .sorted(Comparator.comparingInt(PropertySearchFilter::order))
                .toList();
    }

    @Override
    public List<PropertySearchResult> searchProperties(PropertySearchCriteria criteria) {
        Objects.requireNonNull(criteria, "PropertySearchCriteria cannot be null");
        List<PropertySearchResult> matchingResults = new ArrayList<>();

        for (Property property : propertyRepository.findAllActive()) {
            List<AvailableRoomQuote> initialRoomQuotes = buildRoomQuotes(property, criteria);
            PropertySearchContext context = new PropertySearchContext(property, initialRoomQuotes);

            if (matchesAllFilters(context, criteria) && context.hasMatchingRooms()) {
                List<AvailableRoomQuote> sortedRooms = context.getCandidateRooms().stream()
                        .sorted(Comparator.comparing(rq -> rq.priceQuote().averageNightlyRate()))
                        .toList();
                Money lowestNightly = sortedRooms.get(0).priceQuote().averageNightlyRate();
                matchingResults.add(new PropertySearchResult(property, sortedRooms, lowestNightly));
            }
        }

        matchingResults.sort(Comparator.comparing(PropertySearchResult::startingFromPerNight));
        return matchingResults;
    }

    private boolean matchesAllFilters(PropertySearchContext context, PropertySearchCriteria criteria) {
        for (PropertySearchFilter filter : filters) {
            if (!filter.matches(context, criteria)) {
                return false;
            }
        }
        return true;
    }

    private List<AvailableRoomQuote> buildRoomQuotes(Property property, PropertySearchCriteria criteria) {
        int effectiveGuests = criteria.guests() == null ? 1 : criteria.guests();
        List<AvailableRoomQuote> quotes = new ArrayList<>();

        for (RoomType roomType : property.getRoomTypes()) {
            if (criteria.dateRange() != null) {
                int available = inventoryManager.getAvailableRooms(
                        property.getId(),
                        roomType.id(),
                        criteria.dateRange()
                );
                PriceQuote quote = pricingStrategy.quote(
                        property,
                        roomType,
                        criteria.dateRange(),
                        effectiveGuests
                );
                quotes.add(new AvailableRoomQuote(roomType, available, quote));
            } else {
                Money nightly = roomType.basePricePerNight();
                PriceQuote referenceQuote = new PriceQuote(nightly, List.of(), nightly, nightly, 1);
                quotes.add(new AvailableRoomQuote(roomType, roomType.totalInventory(), referenceQuote));
            }
        }
        return quotes;
    }
}
