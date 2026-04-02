package com.lmp.billing.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Rattache une commande invité au compte déjà connecté et prépare le PaymentIntent.
 */
public class GuestCheckoutAttachRequest {

    @NotBlank(message = "Token de commande manquant")
    private String checkoutToken;

    public String getCheckoutToken() {
        return checkoutToken;
    }

    public void setCheckoutToken(String checkoutToken) {
        this.checkoutToken = checkoutToken;
    }
}
