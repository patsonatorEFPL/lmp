package com.lmp.integration.sync.mapper;

import com.lmp.auth.domain.User;
import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderItem;
import com.lmp.catalog.domain.Service;
import com.lmp.integration.sync.SyncProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du mapping Order → payload external ERP.
 */
class OrderSyncMapperTest {

    private OrderSyncMapper mapper;
    private SyncProperties syncProperties;

    @BeforeEach
    void setUp() {
        syncProperties = mock(SyncProperties.class);
        SyncProperties.External external = new SyncProperties.External();
        external.setCompany("LMP Services");
        external.setCurrency("EUR");
        when(syncProperties.getExternal()).thenReturn(external);
        mapper = new OrderSyncMapper(syncProperties);
    }

    @Test
    void toSalesOrderPayload_shouldContainCoreFields() {
        User user = new User();
        user.setFirstName("Alice");
        user.setLastName("Dupont");
        user.setEmail("alice@example.com");

        Service service = new Service();
        service.setTitle("Référencement SEO");

        OrderItem item = new OrderItem();
        item.setService(service);
        item.setQuantity(1);
        item.setPrice(BigDecimal.valueOf(500));

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUser(user);
        order.setItems(Set.of(item));
        order.setTotalAmount(BigDecimal.valueOf(500));
        order.setCreatedAt(LocalDateTime.now());

        Map<String, Object> payload = mapper.toSalesOrderPayload(order);

        assertThat(payload).containsKey("customer");
        assertThat(payload).containsEntry("company", "LMP Services");
        assertThat(payload).containsEntry("currency", "EUR");
        assertThat(payload).containsKey("transaction_date");
        assertThat(payload).containsEntry("order_type", "Shopping Cart");
        assertThat(payload).containsEntry("lmp_order_id", order.getId().toString());
        assertThat(payload).containsKey("items");
        assertThat((Iterable<?>) payload.get("items")).hasSize(1);
    }

    @Test
    void toSalesOrderPayload_shouldUseEmailAsCustomerName_whenUserHasNoFirstName() {
        User user = new User();
        user.setEmail("bob@example.com");

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUser(user);
        order.setItems(Set.of());
        order.setTotalAmount(BigDecimal.ZERO);
        order.setCreatedAt(LocalDateTime.now());

        Map<String, Object> payload = mapper.toSalesOrderPayload(order);

        assertThat(payload).containsEntry("customer", "bob@example.com");
    }
}
