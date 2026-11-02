package com.plazoleta.plazoleta.domain.exception;

public class OrderFromAnotherRestaurantException extends DomainException {

    public OrderFromAnotherRestaurantException() {
        super("El pedido no pertenece al restaurante del empleado");
    }
}
