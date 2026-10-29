package com.plazoleta.plazoleta.domain.exception;

public class RestaurantNotFoundException extends DomainException {

    public RestaurantNotFoundException() {
        super("No existe un restaurante con el id indicado");
    }
}
