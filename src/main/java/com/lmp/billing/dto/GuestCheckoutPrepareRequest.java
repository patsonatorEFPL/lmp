package com.lmp.billing.dto;

import com.lmp.auth.dto.RegisterDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code POST /api/v1/payments/guest-order/prepare} : inscription + TVA + token de commande invité.
 */
public class GuestCheckoutPrepareRequest {

    @NotBlank
    private String checkoutToken;

    @NotNull
    @Valid
    private RegisterDto registration;

    private Boolean vatReverseCharge;

    private String vatNumber;

    public String getCheckoutToken() {
        return checkoutToken;
    }

    public void setCheckoutToken(String checkoutToken) {
        this.checkoutToken = checkoutToken;
    }

    public RegisterDto getRegistration() {
        return registration;
    }

    public void setRegistration(RegisterDto registration) {
        this.registration = registration;
    }

    public Boolean getVatReverseCharge() {
        return vatReverseCharge;
    }

    public void setVatReverseCharge(Boolean vatReverseCharge) {
        this.vatReverseCharge = vatReverseCharge;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }
}
