package com.plazoleta.plazoleta.domain.exception;

public class InvalidPhoneException extends DomainException {

    public InvalidPhoneException() {
        super("El telefono debe tener maximo 13 caracteres, solo digitos y opcionalmente el simbolo + al inicio");
    }
}
