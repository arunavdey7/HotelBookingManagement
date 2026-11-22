package com.hotelbooking.domain.search;

/**
 * Pluggable search filter contract (Open/Closed Principle).
 * New discovery filters can be introduced by adding a new implementation without modifying
 * the search orchestrator.
 */
public interface PropertySearchFilter {

    /**
     * Execution priority (lower runs earlier so cheap property checks short-circuit before inventory lookups).
     */
    default int order() {
        return 100;
    }

    boolean matches(PropertySearchContext context, PropertySearchCriteria criteria);
}
