package com.hotelbooking.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HotelBookingEndToEndApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void endToEndFlowOnboardingDiscoveryBookingIdempotentPaymentAndCancellation() throws Exception {
        // 1. Create a multi-property hotel group account
        MvcResult ownerResult = mockMvc.perform(post("/api/v1/owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " Sapphire Hospitality Chain ",
                                  "contactEmail": "ops@sapphirehotels.com",
                                  "phone": "+91-22-55550000"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sapphire Hospitality Chain"))
                .andExpect(jsonPath("$.propertyCount").value(0))
                .andReturn();

        String ownerId = objectMapper.readTree(ownerResult.getResponse().getContentAsString())
                .get("id").asText();

        // 2. Onboard Property #1 under chain (1 room inventory in Jaipur)
        MvcResult prop1Result = mockMvc.perform(post("/api/v1/owners/{ownerId}/properties", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Sapphire Palace Jaipur",
                                  "description": "Heritage luxury palace",
                                  "propertyType": "RESORT",
                                  "location": {
                                    "addressLine": "Civil Lines",
                                    "locality": "C-Scheme",
                                    "city": "Jaipur",
                                    "state": "Rajasthan",
                                    "country": "India",
                                    "postalCode": "302001"
                                  },
                                  "starRating": 5,
                                  "amenities": ["WIFI", "POOL", "SPA"],
                                  "cancellationPolicyCode": "FLEXIBLE",
                                  "roomTypes": [
                                    {
                                      "name": "Royal Heritage Suite",
                                      "category": "SUITE",
                                      "maxOccupancy": 3,
                                      "totalInventory": 1,
                                      "basePricePerNight": 12000,
                                      "currency": "INR",
                                      "amenities": ["AIR_CONDITIONING", "BALCONY"]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(ownerId))
                .andReturn();

        JsonNode prop1Json = objectMapper.readTree(prop1Result.getResponse().getContentAsString());
        String propertyId = prop1Json.get("id").asText();
        String roomTypeId = prop1Json.get("roomTypes").get(0).get("id").asText();

        // 3. Onboard Property #2 under same chain (Udaipur) to verify multi-property portfolio
        mockMvc.perform(post("/api/v1/owners/{ownerId}/properties", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Sapphire Lakeview Udaipur",
                                  "propertyType": "HOTEL",
                                  "location": {
                                    "locality": "Pichola",
                                    "city": "Udaipur"
                                  },
                                  "starRating": 5,
                                  "amenities": ["WIFI", "POOL"],
                                  "roomTypes": [
                                    {
                                      "name": "Lake View Deluxe",
                                      "category": "DELUXE",
                                      "maxOccupancy": 2,
                                      "totalInventory": 2,
                                      "basePricePerNight": 9000
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/owners/{ownerId}", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyCount").value(2))
                .andExpect(jsonPath("$.multiPropertyPortfolio").value(true));

        // 4. Discover available property in Jaipur for future dates
        LocalDate checkIn = LocalDate.now().plusDays(15);
        LocalDate checkOut = checkIn.plusDays(2);

        mockMvc.perform(get("/api/v1/properties/search")
                        .param("city", "Jaipur")
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString())
                        .param("guests", "2")
                        .param("amenities", "WIFI,SPA")
                        .param("minStarRating", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].propertyId").value(propertyId));

        // 5. Book the sole Royal Heritage Suite
        String bookingPayload = """
                {
                  "propertyId": "%s",
                  "roomTypeId": "%s",
                  "guest": {
                    "fullName": "Kabir Mehta",
                    "email": "kabir@example.com",
                    "phone": "9812345678"
                  },
                  "checkIn": "%s",
                  "checkOut": "%s",
                  "numberOfGuests": 2
                }
                """.formatted(propertyId, roomTypeId, checkIn, checkOut);

        MvcResult bookingResult = mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andReturn();

        String bookingId = objectMapper.readTree(bookingResult.getResponse().getContentAsString())
                .get("id").asText();

        // 6. Overlapping booking for the same room must return 409 Conflict (double-booking prevented)
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload))
                .andExpect(status().isConflict());

        // 7. Property is no longer returned in date-filtered search while sold out
        mockMvc.perform(get("/api/v1/properties/search")
                        .param("city", "Jaipur")
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString())
                        .param("guests", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 8. Pay via UPI with Idempotency-Key header -> confirms booking
        String paymentPayload = """
                {
                  "method": "UPI",
                  "details": {
                    "vpa": "kabir@okhdfcbank"
                  }
                }
                """;

        MvcResult payResult = mockMvc.perform(post("/api/v1/bookings/{bookingId}/payments", bookingId)
                        .header("Idempotency-Key", "e2e-idem-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.idempotentReplay").value(false))
                .andReturn();

        String firstPaymentId = objectMapper.readTree(payResult.getResponse().getContentAsString())
                .get("paymentId").asText();

        // 9. Replay identical payment request with same Idempotency-Key -> idempotentReplay = true
        mockMvc.perform(post("/api/v1/bookings/{bookingId}/payments", bookingId)
                        .header("Idempotency-Key", "e2e-idem-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(firstPaymentId))
                .andExpect(jsonPath("$.idempotentReplay").value(true));

        // 10. Cancel booking -> applies FLEXIBLE refund policy (100%) and releases inventory
        mockMvc.perform(post("/api/v1/bookings/{bookingId}/cancel", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "Trip postponed" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationDetails.refundReference").isNotEmpty());

        // 11. Property is immediately discoverable again for those dates!
        mockMvc.perform(get("/api/v1/properties/search")
                        .param("city", "Jaipur")
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString())
                        .param("guests", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void validationErrorsAndOpenApiSpecAreExposedCleanly() throws Exception {
        mockMvc.perform(post("/api/v1/owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "contactEmail": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray());

        MvcResult openApiResult = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(openApiResult.getResponse().getContentAsString())
                .contains("Hotel Booking Management Platform API");
    }
}
