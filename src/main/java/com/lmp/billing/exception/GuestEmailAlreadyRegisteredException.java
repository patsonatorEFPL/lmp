package com.lmp.billing.exception;

/**
 * E-mail déjà utilisé lors du parcours commande invité : le client doit se connecter.
 */
public class GuestEmailAlreadyRegisteredException extends RuntimeException {

    public GuestEmailAlreadyRegisteredException(String message) {
        super(message);
    }
}
