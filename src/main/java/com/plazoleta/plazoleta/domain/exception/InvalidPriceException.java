package com.plazoleta.plazoleta.domain.exception;

public class InvalidPriceException extends DomainException {

    public InvalidPriceException() {
        super("El precio debe ser un numero entero positivo mayor a 0");
    }
}
