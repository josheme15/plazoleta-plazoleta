package com.plazoleta.plazoleta.domain.exception;

public class DishFromAnotherRestaurantException extends DomainException {

    public DishFromAnotherRestaurantException() {
        super("Todos los platos del pedido deben ser del mismo restaurante indicado");
    }
}
