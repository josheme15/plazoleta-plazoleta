package com.plazoleta.plazoleta.domain.exception;

public class NoDishFieldsToUpdateException extends DomainException {

    public NoDishFieldsToUpdateException() {
        super("Debe enviar al menos el precio o la descripcion del plato para actualizar");
    }
}
