package com.lmp.catalog.dto;

import com.lmp.catalog.domain.ServiceBenefit;
import com.lmp.catalog.domain.ServiceOffer;
import com.lmp.shared.pricing.PricingContext;
import com.lmp.shared.pricing.RegionalPricingService;

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
    /** Catalogue admin / sans conversion régionale (prix EUR en base). */
    public static ServiceResponse from(com.lmp.catalog.domain.Service service) {
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

    /** API publique : prix convertis selon l'IP du client. */
    public static ServiceResponse from(com.lmp.catalog.domain.Service service,
            PricingContext ctx,
            RegionalPricingService pricing) {
        List<String> benefitList = service.getBenefits() != null
                ? service.getBenefits().stream().map(ServiceBenefit::getBenefit).collect(Collectors.toList())
                : List.of();

        ServiceOffer offer = service.getCurrentOffer();
        OfferResponse offerResponse = offer != null ? OfferResponse.from(offer, ctx, pricing) : null;

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
            boolean isDefault,
            String currency,
            BigDecimal priceEur
    ) {
        public static OfferResponse from(ServiceOffer offer) {
            BigDecimal p = offer.getPrice();
            return new OfferResponse(
                    offer.getId(),
                    offer.getName(),
                    p,
                    offer.getOriginalPrice(),
                    offer.getDurationType() != null ? offer.getDurationType().name() : null,
                    offer.getIsDefault(),
                    "EUR",
                    p);
        }

        public static OfferResponse from(ServiceOffer offer, PricingContext ctx, RegionalPricingService pricing) {
            BigDecimal eur = offer.getPrice();
            BigDecimal eurOrig = offer.getOriginalPrice();
            BigDecimal display = pricing.convertFromEur(eur, ctx);
            BigDecimal displayOrig = eurOrig != null ? pricing.convertFromEur(eurOrig, ctx) : null;
            return new OfferResponse(
                    offer.getId(),
                    offer.getName(),
                    display,
                    displayOrig,
                    offer.getDurationType() != null ? offer.getDurationType().name() : null,
                    offer.getIsDefault(),
                    ctx.currency(),
                    eur);
        }
    }
}
