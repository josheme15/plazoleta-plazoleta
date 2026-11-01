package com.plazoleta.plazoleta.domain.exception;

public class EmployeeWithoutRestaurantException extends DomainException {

    public EmployeeWithoutRestaurantException() {
        super("El empleado no tiene un restaurante asignado");
    }
}
