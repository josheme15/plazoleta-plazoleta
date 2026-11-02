package com.plazoleta.plazoleta.domain.exception;

public class NotAssignedEmployeeException extends DomainException {

    public NotAssignedEmployeeException() {
        super("Solo el empleado asignado al pedido puede marcarlo como LISTO");
    }
}
