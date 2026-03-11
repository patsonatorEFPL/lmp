package com.lmp.billing.service;

import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.lmp.billing.domain.Cart;
import com.lmp.billing.domain.CartItem;
import com.lmp.catalog.domain.Service;
import com.lmp.auth.domain.User;
import com.lmp.billing.repository.CartRepository;
import com.lmp.catalog.repository.ServiceRepository;

@org.springframework.stereotype.Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final ServiceRepository serviceRepository;

    public CartService(CartRepository cartRepository, ServiceRepository serviceRepository) {
        this.cartRepository = cartRepository;
        this.serviceRepository = serviceRepository;
    }

    public Cart getOrCreateCartForUser(User user) {
        return cartRepository.findByUser(user).orElseGet(() -> {
            Cart newCart = new Cart(user);
            return cartRepository.save(newCart);
        });
    }

    public Cart addToCart(User user, java.util.UUID serviceId) {
        Cart cart = getOrCreateCartForUser(user);

        Optional<Service> serviceOpt = serviceRepository.findById(serviceId);
        if (serviceOpt.isEmpty()) {
            throw new IllegalArgumentException("Service non trouvé.");
        }

        Service service = serviceOpt.get();

        // Check if service is already in cart to increment or simply add
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getService().getId().equals(serviceId))
                .findFirst();

        if (existingItem.isPresent()) {
            // For now, service quantities are 1, but this allows future expansion
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + 1);
        } else {
            CartItem newItem = new CartItem(cart, service, 1);
            cart.addItem(newItem);
        }

        return cartRepository.save(cart);
    }

    public Cart removeFromCart(User user, java.util.UUID cartItemId) {
        Cart cart = getOrCreateCartForUser(user);

        cart.getItems().removeIf(item -> item.getId().equals(cartItemId));

        return cartRepository.save(cart);
    }

    public Cart clearCart(User user) {
        Cart cart = getOrCreateCartForUser(user);
        cart.getItems().clear();
        return cartRepository.save(cart);
    }
}
