package com.plazoleta.plazoleta.domain.exception;

public class InvalidPaginationException extends DomainException {

    public InvalidPaginationException() {
        super("La pagina debe ser mayor o igual a 0 y la cantidad de elementos por pagina mayor a 0");
    }
}
