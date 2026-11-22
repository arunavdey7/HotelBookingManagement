# Hotel Booking Management Platform

A clean, modular Spring Boot backend modelling hotel discovery, single & multi-property onboarding, concurrency-safe room booking, idempotent multi-method payments, and pluggable cancellation/refund policies.

---

## 1. Prerequisites & How to Run

- **Java**: JDK 17+ (tested on JDK 17 & 21)
- **Build Tool**: Included via Maven Wrapper (`./mvnw` / `mvnw.cmd`) — no local Maven installation required.

### Run the Test Suite
```bash
./mvnw test
```

### Start the Application
```bash
# Clean state on http://localhost:8080
./mvnw spring-boot:run

# Optional: start with pre-seeded sample hotel chain + standalone boutique property
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

### Interactive API Documentation (OpenAPI / Swagger UI)
Once running, open:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

---

## 2. Architecture & Package Structure

The codebase follows **Clean / Hexagonal Architecture (Ports & Adapters)** with strict inward dependencies:

```text
com.hotelbooking
├── domain                 # Pure Java 17 domain layer (zero Spring/Jackson/framework imports)
│   ├── common             # Value objects: Money (currency-safe BigDecimal), DateRange ([checkIn, checkOut)), Location
│   ├── owner              # OwnerAccount aggregate & OwnerRepository port (unified 1..N portfolio model)
│   ├── property           # Property aggregate, RoomType, extensible Amenity value object, PropertyRepository port
│   ├── inventory          # InventoryManager port (per-night room reservation & release contract)
│   ├── pricing            # PricingStrategy port, PriceQuote, PriceAdjustment
│   ├── search             # Specification<T>, PropertySearchFilter contract, PropertySearchContext, PropertySearchCriteria
│   ├── booking            # Booking aggregate root with explicit BookingStatus state machine, BookingRepository port
│   ├── payment            # Payment entity, open PaymentInstrumentDetails contract, PaymentProcessor port, IdempotencyRecord
│   ├── cancellation       # CancellationPolicy port, RefundDecision
│   └── exception          # Domain exception hierarchy
├── application            # Use-case interfaces, orchestrators, and business policies
│   ├── port               # Inbound use-case contracts (Onboarding, Discovery, Booking, Payment)
│   ├── service            # Use-case implementations programmed strictly against domain ports
│   ├── search/filter      # Pluggable PropertySearchFilter implementations
│   ├── pricing            # FlatRatePricingStrategy, DynamicPricingStrategy, composable PricingRule beans
│   ├── cancellation       # Flexible, Tiered (48h/24h), and NonRefundable policies + CancellationPolicyRegistry
│   └── support            # KeyedLockManager for fine-grained per-booking / per-idempotency-key synchronization
├── infrastructure         # Technical adapters (in-memory repositories, lock-striped inventory, mock gateways, Spring config)
│   ├── persistence        # Thread-safe ConcurrentHashMap repository adapters
│   ├── inventory          # InMemoryInventoryManager with per-(propertyId, roomTypeId) ReentrantReadWriteLock
│   ├── payment            # Mock CARD, UPI, and WALLET gateway processors + PaymentProcessorRegistry
│   └── config             # Clock, PricingStrategy wiring, OpenAPI metadata, optional @Profile("demo") seeder
└── api                    # REST controllers, validation DTOs, extensible instrument parsers, GlobalExceptionHandler
```

---

## 3. Key Design Decisions & Extensibility Seams

### 3.1 Unified Single & Multi-Property Ownership (`OwnerAccount` -> `1..N Property`)
Every `Property` belongs to an `OwnerAccount` managing a portfolio of property IDs.
- A **multi-property chain/group** creates an `OwnerAccount` (`POST /api/v1/owners`) and onboards multiple properties under `POST /api/v1/owners/{ownerId}/properties`.
- A **standalone property** onboarded via `POST /api/v1/properties` (with inline owner details or minimal payload) creates a 1-property `OwnerAccount` and delegates to the exact same onboarding pipeline — no separate single-vs-chain branching, and an independent host can add a second property under the same `ownerId` at any time.

### 3.2 Extensible Property Discovery (Filter Pipeline + Room-Aware Evaluation)
`PropertyDiscoveryService` receives `List<PropertySearchFilter>` via constructor injection, ordered by cost (`Location` -> `PropertyType` -> `StarRating` -> `Amenities` -> `GuestCapacity` -> `PriceRange` -> `Availability`).
- **Accurate room-level filtering**: Filters operate on `PropertySearchContext`, narrowing down `candidateRooms`. If a hotel's budget room is sold out for the requested dates and only its luxury suite remains, a low `maxPrice` search accurately excludes the property rather than producing a false positive.
- **Open/Closed**: Adding a new filter (e.g., `MinimumGuestRatingFilter`, `FreeCancellationFilter`) requires adding one `@Component` implementing `PropertySearchFilter` with zero changes to `PropertyDiscoveryService`.
- **Extensible Amenities**: `Amenity` is a normalized value object (`record Amenity(String code)`) rather than a closed enum, so custom amenities (`EV_CHARGING`, `JACUZZI`) work without code changes.

### 3.3 Concurrency-Safe Inventory & Half-Open Stay Intervals
- Stays are modelled as half-open intervals `[checkIn, checkOut)` via `DateRange#stayNights()`, naturally permitting same-day check-out / check-in turnaround on date `D`.
- `InMemoryInventoryManager` maintains a dedicated `ReentrantReadWriteLock` per `(propertyId, roomTypeId)` pool. Simultaneous booking requests for the last room of the same room type serialize atomically across all nights of the stay, while bookings for other room types or properties run concurrently without contention.

### 3.4 Booking State Machine & Payment-Driven Transitions
`Booking` enforces state transitions internally:
```text
                  ┌─── payment succeeded ───► CONFIRMED ─── cancel (policy refund + release inventory) ───► CANCELLED
                  │
[Created] ──► PENDING_PAYMENT (inventory held)
                  │
                  ├─── payment declined ────► PAYMENT_FAILED (held inventory immediately released)
                  └─── cancel (unpaid) ─────► CANCELLED (0 refund + held inventory released)
```

### 3.5 Pluggable Payments & Stripe-Style Idempotency
- **End-to-end OCP**: Both REST payload parsing (`PaymentInstrumentParserRegistry`) and gateway charging/refunding (`PaymentProcessorRegistry`) auto-discover Spring beans (`CARD`, `UPI`, `WALLET`). Adding `NET_BANKING` requires adding its instrument record, parser, and processor — zero edits to controllers or services.
- **Idempotency**: Passing an `Idempotency-Key` header (or `idempotencyKey` body field) locks on that key, fingerprints `bookingId + instrument`, replays the original `PaymentOutcome` (`idempotentReplay: true`) on retries, and returns `409 Conflict` if the same key is reused with different parameters.

### 3.6 Pluggable Pricing & Cancellation Policies
- **Pricing**: Configurable via `booking.pricing.mode` (`dynamic` default, or `flat`). `DynamicPricingStrategy` composes ordered `PricingRule` beans (`WeekendSurgePricingRule` `+20%` on Fri/Sat nights, `LongStayDiscountPricingRule` `-10%` for `7+` nights) and returns an itemized `PriceQuote`.
- **Cancellation**: Resolved per property via `CancellationPolicyRegistry` (`FLEXIBLE`, `MODERATE_TIERED`, `NON_REFUNDABLE`) using an injected `java.time.Clock` for deterministic time-window evaluation.

---

## 4. Core REST Endpoints Quick Reference

| Operation | Method & Path | Notes |
|---|---|---|
| Create Owner / Group | `POST /api/v1/owners` | Creates host or multi-property chain account |
| Onboard Chain Property | `POST /api/v1/owners/{ownerId}/properties` | Adds property to owner's portfolio |
| Onboard Standalone Property | `POST /api/v1/properties` | Creates/links owner under unified model |
| Discover Properties | `GET /api/v1/properties/search` | Params: `city`, `locality`, `checkIn`, `checkOut`, `guests`, `minPrice`, `maxPrice`, `amenities`, `minStarRating`, `propertyType` |
| Create Booking | `POST /api/v1/bookings` | Atomically reserves room inventory (`PENDING_PAYMENT`) |
| Process Payment | `POST /api/v1/bookings/{bookingId}/payments` | Supports `CARD`, `UPI`, `WALLET` + `Idempotency-Key` header |
| Cancel Booking | `POST /api/v1/bookings/{bookingId}/cancel` | Evaluates refund policy, triggers gateway refund, releases inventory |

### Deterministic Gateway Failure Hooks (for Testing)
- **CARD**: Card number ending in `0000` simulates issuer decline.
- **UPI**: VPA starting with `fail@` or `declined@` simulates collect rejection.
- **WALLET**: `walletAccountId` containing `insufficient` simulates low balance.

---

## 5. Key Assumptions

1. **Inventory Hold Semantics**: Creating a booking (`PENDING_PAYMENT`) places an immediate hold on room inventory so a guest entering payment details cannot lose the room mid-checkout. Declined payments transition the booking to `PAYMENT_FAILED` and immediately release the hold.
2. **Check-in Timezone & Cutoff**: Standard check-in time for cancellation window calculations (`48h` / `24h` before check-in) defaults to `14:00 UTC` (`booking.cancellation.check-in-hour=14`).
3. **Currency**: Defaults to `INR` when omitted, with currency-mismatch guards enforced inside the `Money` value object.
4. **Occupancy**: Total guest count must satisfy `1 <= numberOfGuests <= roomType.maxOccupancy * roomsRequested` (`roomsRequested` defaults to `1`).

---

## 6. What I Would Add With More Time

- **TTL Hold Expiration**: A scheduled background sweeper to auto-expire `PENDING_PAYMENT` bookings abandoned beyond a configurable TTL (e.g., 15 minutes) and release their held inventory.
- **Persistent Relational Storage & Optimistic/Row Locking**: Swap `InMemory*` adapters for PostgreSQL/JPA adapters using a `room_night_inventory(property_id, room_type_id, stay_date, reserved_count, version)` table with `SELECT ... FOR UPDATE` or optimistic locking + unique `(idempotency_key)` constraint.
- **Indexed Search Read Model**: Push property metadata and availability projections into Elasticsearch/PostgreSQL criteria queries instead of in-memory filtering once property count scales beyond tens of thousands.
- **Asynchronous Domain Events (Transactional Outbox)**: Emit `BookingConfirmedEvent` / `BookingCancelledEvent` for notifications, analytics, and audit logs.
