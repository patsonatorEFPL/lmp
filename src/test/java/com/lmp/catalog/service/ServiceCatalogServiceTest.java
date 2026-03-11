package com.lmp.catalog.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lmp.catalog.domain.Service;
import com.lmp.catalog.domain.ServiceCategory;
import com.lmp.catalog.domain.ServiceOffer;
import com.lmp.catalog.domain.DurationType;
import com.lmp.catalog.repository.ServiceCategoryRepository;
import com.lmp.catalog.repository.ServiceOfferRepository;
import com.lmp.catalog.repository.ServiceRepository;

/**
 * Tests unitaires pour ServiceCatalogService
 */
@ExtendWith(MockitoExtension.class)
class ServiceCatalogServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ServiceOfferRepository offerRepository;

    @Mock
    private ServiceCategoryRepository categoryRepository;

    @InjectMocks
    private ServiceCatalogService catalogService;

    private static final UUID OFFER_ID_1 = UUID.randomUUID();
    private static final UUID OFFER_ID_2 = UUID.randomUUID();
    private static final UUID OFFER_ID_INVALID = UUID.randomUUID();
    private static final UUID SERVICE_ID_1 = UUID.randomUUID();
    private static final UUID SERVICE_ID_INVALID = UUID.randomUUID();
    private static final UUID OFFER_ID_DEFAULT = UUID.randomUUID();

    @Test
    void getValidOffer_activeOffer_returnsOffer() {
        ServiceOffer offer = createValidOffer(OFFER_ID_1);
        when(offerRepository.findByIdAndActiveTrue(OFFER_ID_1)).thenReturn(Optional.of(offer));

        Optional<ServiceOffer> result = catalogService.getValidOffer(OFFER_ID_1);

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("353.89"), result.get().getPrice());
    }

    @Test
    void getValidOffer_inactiveOffer_returnsEmpty() {
        when(offerRepository.findByIdAndActiveTrue(OFFER_ID_INVALID)).thenReturn(Optional.empty());

        Optional<ServiceOffer> result = catalogService.getValidOffer(OFFER_ID_INVALID);

        assertTrue(result.isEmpty());
    }

    @Test
    void getValidOffer_expiredOffer_returnsEmpty() {
        ServiceOffer offer = createValidOffer(OFFER_ID_2);
        offer.setValidTo(LocalDateTime.now().minusDays(1)); // Expired
        when(offerRepository.findByIdAndActiveTrue(OFFER_ID_2)).thenReturn(Optional.of(offer));

        Optional<ServiceOffer> result = catalogService.getValidOffer(OFFER_ID_2);

        assertTrue(result.isEmpty());
    }

    @Test
    void getActiveServices_returnsList() {
        Service s1 = new Service();
        s1.setTitle("Service 1");
        s1.setActive(true);
        when(serviceRepository.findByActiveTrue()).thenReturn(List.of(s1));

        List<Service> result = catalogService.getActiveServices();

        assertEquals(1, result.size());
        assertEquals("Service 1", result.get(0).getTitle());
    }

    @Test
    void getCurrentOfferForService_defaultOffer_returnsDefault() {
        Service service = new Service();
        service.setId(SERVICE_ID_1);

        ServiceOffer defaultOffer = createValidOffer(OFFER_ID_DEFAULT);
        defaultOffer.setIsDefault(true);
        service.setOffers(Set.of(defaultOffer));

        when(serviceRepository.findById(SERVICE_ID_1)).thenReturn(Optional.of(service));

        Optional<ServiceOffer> result = catalogService.getCurrentOfferForService(SERVICE_ID_1);

        assertTrue(result.isPresent());
    }

    @Test
    void getCurrentOfferForService_nonExistentService_returnsEmpty() {
        when(serviceRepository.findById(SERVICE_ID_INVALID)).thenReturn(Optional.empty());

        Optional<ServiceOffer> result = catalogService.getCurrentOfferForService(SERVICE_ID_INVALID);

        assertTrue(result.isEmpty());
    }

    private ServiceOffer createValidOffer(UUID id) {
        Service service = new Service();
        service.setId(SERVICE_ID_1);
        service.setTitle("Test Service");

        ServiceOffer offer = new ServiceOffer();
        offer.setId(id);
        offer.setService(service);
        offer.setName("Tarif Standard");
        offer.setPrice(new BigDecimal("353.89"));
        offer.setDurationType(DurationType.ONE_TIME);
        offer.setIsDefault(true);
        offer.setActive(true);
        return offer;
    }
}
