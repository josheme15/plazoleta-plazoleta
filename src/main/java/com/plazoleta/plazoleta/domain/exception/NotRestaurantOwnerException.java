package com.plazoleta.plazoleta.domain.exception;

public class NotRestaurantOwnerException extends DomainException {

    public NotRestaurantOwnerException() {
        super("Solo el propietario del restaurante puede realizar esta accion");
    }
}
