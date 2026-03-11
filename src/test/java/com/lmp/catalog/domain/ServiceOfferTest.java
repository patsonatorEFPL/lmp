package com.lmp.catalog.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.lmp.catalog.domain.DurationType;

/**
 * Tests unitaires pour ServiceOffer.isCurrentlyValid()
 */
class ServiceOfferTest {

    @Test
    void isCurrentlyValid_activeNoDateConstraints_returnsTrue() {
        ServiceOffer offer = createOffer(true, null, null);
        assertTrue(offer.isCurrentlyValid());
    }

    @Test
    void isCurrentlyValid_inactive_returnsFalse() {
        ServiceOffer offer = createOffer(false, null, null);
        assertFalse(offer.isCurrentlyValid());
    }

    @Test
    void isCurrentlyValid_futureStartDate_returnsFalse() {
        ServiceOffer offer = createOffer(true, LocalDateTime.now().plusDays(1), null);
        assertFalse(offer.isCurrentlyValid());
    }

    @Test
    void isCurrentlyValid_pastEndDate_returnsFalse() {
        ServiceOffer offer = createOffer(true, null, LocalDateTime.now().minusDays(1));
        assertFalse(offer.isCurrentlyValid());
    }

    @Test
    void isCurrentlyValid_withinDateRange_returnsTrue() {
        ServiceOffer offer = createOffer(true,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1));
        assertTrue(offer.isCurrentlyValid());
    }

    @Test
    void isCurrentlyValid_pastStartNoEnd_returnsTrue() {
        ServiceOffer offer = createOffer(true, LocalDateTime.now().minusDays(5), null);
        assertTrue(offer.isCurrentlyValid());
    }

    private ServiceOffer createOffer(boolean active, LocalDateTime validFrom, LocalDateTime validTo) {
        ServiceOffer offer = new ServiceOffer();
        offer.setActive(active);
        offer.setValidFrom(validFrom);
        offer.setValidTo(validTo);
        offer.setName("Test Offer");
        offer.setPrice(new BigDecimal("100.00"));
        offer.setDurationType(DurationType.ONE_TIME);
        offer.setIsDefault(true);
        return offer;
    }
}
