package com.lmp.web.controller.api.v1.dto;

import com.lmp.domain.entity.ServiceBenefit;
import com.lmp.domain.entity.ServiceOffer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO de réponse pour un service du catalogue.
 */
public record ServiceResponse(
        UUID id,
        String title,
        String slug,
        String description,
        String icon,
        String categoryName,
        String categorySlug,
        boolean featured,
        boolean active,
        int displayOrder,
        List<String> benefits,
        OfferResponse currentOffer
) {
    public static ServiceResponse from(com.lmp.domain.entity.Service service) {
        List<String> benefitList = service.getBenefits() != null
                ? service.getBenefits().stream().map(ServiceBenefit::getBenefit).collect(Collectors.toList())
                : List.of();

        ServiceOffer offer = service.getCurrentOffer();
        OfferResponse offerResponse = offer != null ? OfferResponse.from(offer) : null;

        return new ServiceResponse(
                service.getId(),
                service.getTitle(),
                service.getSlug(),
                service.getDescription(),
                service.getIcon(),
                service.getCategory() != null ? service.getCategory().getName() : null,
                service.getCategory() != null ? service.getCategory().getSlug() : null,
                Boolean.TRUE.equals(service.getFeatured()),
                Boolean.TRUE.equals(service.getActive()),
                service.getDisplayOrder() != null ? service.getDisplayOrder() : 0,
                benefitList,
                offerResponse);
    }

    public record OfferResponse(
            UUID id,
            String name,
            BigDecimal price,
            BigDecimal originalPrice,
            String durationType,
            boolean isDefault
    ) {
        public static OfferResponse from(ServiceOffer offer) {
            return new OfferResponse(
                    offer.getId(),
                    offer.getName(),
                    offer.getPrice(),
                    offer.getOriginalPrice(),
                    offer.getDurationType() != null ? offer.getDurationType().name() : null,
                    offer.getIsDefault());
        }
    }
}
