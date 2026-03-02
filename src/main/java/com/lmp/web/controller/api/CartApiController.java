package com.lmp.web.controller.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lmp.domain.entity.Cart;
import com.lmp.domain.entity.User;
import com.lmp.service.CartService;
import com.lmp.service.user.UserService;

@RestController
@RequestMapping("/api/cart")
public class CartApiController {

    private final CartService cartService;
    private final UserService userService;

    public CartApiController(CartService cartService, UserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }

    @GetMapping
    public ResponseEntity<?> getCart(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Non authentifié"));
        }

        Cart cart = cartService.getOrCreateCartForUser(user);
        return ResponseEntity.ok(formatCartResponse(cart));
    }

    @PostMapping("/add/{serviceId}")
    public ResponseEntity<?> addToCart(@PathVariable Long serviceId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Veuillez vous connecter pour ajouter au panier"));
        }

        try {
            Cart cart = cartService.addToCart(user, serviceId);
            return ResponseEntity.ok(formatCartResponse(cart));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long cartItemId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Non authentifié"));
        }

        Cart cart = cartService.removeFromCart(user, cartItemId);
        return ResponseEntity.ok(formatCartResponse(cart));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Non authentifié"));
        }

        Cart cart = cartService.clearCart(user);
        return ResponseEntity.ok(formatCartResponse(cart));
    }

    /**
     * Helper paramétré pour formater une réponse lisible côté Front
     */
    private Map<String, Object> formatCartResponse(Cart cart) {
        Map<String, Object> response = new HashMap<>();
        response.put("cartId", cart.getId());
        response.put("totalAmount", cart.getTotalAmount());

        List<Map<String, Object>> items = cart.getItems().stream().map(item -> {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", item.getId());
            itemMap.put("serviceId", item.getService().getId());
            itemMap.put("serviceName", item.getService().getTitle());

            com.lmp.domain.entity.ServiceOffer offer = item.getService().getCurrentOffer();
            if (offer != null) {
                itemMap.put("priceValue", offer.getPrice().doubleValue());
                itemMap.put("price", offer.getPrice().toString() + " €"); // Temp format
            } else {
                itemMap.put("priceValue", 0.0);
                itemMap.put("price", "Non disponible");
            }

            itemMap.put("quantity", item.getQuantity());
            return itemMap;
        }).collect(Collectors.toList());

        response.put("items", items);
        response.put("itemCount", cart.getItems().size());
        return response;
    }
}
