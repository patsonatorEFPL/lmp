package com.lmp.service.catalog;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lmp.domain.entity.Service;
import com.lmp.domain.entity.ServiceCategory;
import com.lmp.domain.entity.ServiceOffer;
import com.lmp.domain.entity.enums.DurationType;
import com.lmp.repository.ServiceCategoryRepository;
import com.lmp.repository.ServiceOfferRepository;
import com.lmp.repository.ServiceRepository;

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

    @Test
    void getValidOffer_activeOffer_returnsOffer() {
        ServiceOffer offer = createValidOffer(1L);
        when(offerRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(offer));

        Optional<ServiceOffer> result = catalogService.getValidOffer(1L);

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("353.89"), result.get().getPrice());
    }

    @Test
    void getValidOffer_inactiveOffer_returnsEmpty() {
        when(offerRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        Optional<ServiceOffer> result = catalogService.getValidOffer(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getValidOffer_expiredOffer_returnsEmpty() {
        ServiceOffer offer = createValidOffer(2L);
        offer.setValidTo(LocalDateTime.now().minusDays(1)); // Expired
        when(offerRepository.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(offer));

        Optional<ServiceOffer> result = catalogService.getValidOffer(2L);

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
        service.setId(1L);

        ServiceOffer defaultOffer = createValidOffer(10L);
        defaultOffer.setIsDefault(true);
        service.setOffers(Set.of(defaultOffer));

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));

        Optional<ServiceOffer> result = catalogService.getCurrentOfferForService(1L);

        assertTrue(result.isPresent());
    }

    @Test
    void getCurrentOfferForService_nonExistentService_returnsEmpty() {
        when(serviceRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<ServiceOffer> result = catalogService.getCurrentOfferForService(999L);

        assertTrue(result.isEmpty());
    }

    private ServiceOffer createValidOffer(Long id) {
        Service service = new Service();
        service.setId(1L);
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
