package com.lmp.integration.sync.mapper;

import com.lmp.catalog.domain.Service;
import com.lmp.catalog.domain.ServiceOffer;
import com.lmp.integration.sync.ExternalResponse;
import com.lmp.integration.sync.ExternalSystemClient;
import com.lmp.integration.sync.SyncEntityType;
import com.lmp.integration.sync.SyncProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Mapper pour synchroniser les prix du catalogue (ServiceOffer) vers externalErp Item Price.
 * <p>
 * Stratégie upsert sur la clé composite (item_code, price_list) :
 * 1. GET ?filters=[["item_code","=","..."],["price_list","=","..."]]
 * 2. Si trouvé → UPDATE avec le name du document
 * 3. Sinon → CREATE
 */
@Component
public class ItemPriceSyncMapper {

    private static final Logger log = LoggerFactory.getLogger(ItemPriceSyncMapper.class);

    private final SyncProperties syncProperties;
    private final ExternalSystemClient externalClient;

    public ItemPriceSyncMapper(SyncProperties syncProperties, ExternalSystemClient externalClient) {
        this.syncProperties = syncProperties;
        this.externalClient = externalClient;
    }

    /**
     * Construit le payload Item Price pour une offre donnée.
     */
    public Map<String, Object> toItemPricePayload(ServiceOffer offer, Service service) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("item_code", service.getExternalItemCode());
        payload.put("price_list", syncProperties.getExternal().getPriceList());
        payload.put("price_list_rate", offer.getPrice());
        payload.put("currency", syncProperties.getExternal().getCurrency());
        SyncMapperUtils.setCompanyFields(payload,
                syncProperties.getExternal().getCompany(),
                syncProperties.getExternal().getCurrency());
        return payload;
    }

    /**
     * Upsert un Item Price côté externalErp.
     *
     * @return ExternalResponse du create ou update
     */
    public ExternalResponse upsertItemPrice(ServiceOffer offer, Service service) {
        String itemCode = service.getExternalItemCode();
        String priceList = syncProperties.getExternal().getPriceList();

        if (itemCode == null || itemCode.isBlank()) {
            log.warn("[SYNC] Service {} has no externalItemCode — skipping ItemPrice sync", service.getId());
            return null;
        }

        String filterJson = "[[\"item_code\",\"=\",\"" + escapeJsonString(itemCode) + "\"],[\"price_list\",\"=\",\"" + escapeJsonString(priceList) + "\"]]";

        Optional<Map<String, Object>> existing = externalClient.findFirstByFilters(SyncEntityType.ITEM_PRICE, filterJson);

        Map<String, Object> payload = toItemPricePayload(offer, service);

        if (existing.isPresent()) {
            String existingId = existing.get().get("name").toString();
            log.info("[SYNC] ItemPrice exists for {} / {} -> updating {}", itemCode, priceList, existingId);
            return externalClient.updateEntity(SyncEntityType.ITEM_PRICE, existingId, payload);
        } else {
            log.info("[SYNC] ItemPrice not found for {} / {} -> creating", itemCode, priceList);
            return externalClient.createEntity(SyncEntityType.ITEM_PRICE, payload);
        }
    }

    private String escapeJsonString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
