package com.plazoleta.plazoleta.domain.exception;

public class InvalidQuantityException extends DomainException {

    public InvalidQuantityException() {
        super("La cantidad de cada plato debe ser un numero entero mayor a 0");
    }
}
