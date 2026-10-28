package com.plazoleta.plazoleta.domain.exception;

public class InvalidRestaurantNameException extends DomainException {

    public InvalidRestaurantNameException() {
        super("El nombre del restaurante puede contener numeros, pero no puede estar compuesto solo por numeros");
    }
}
