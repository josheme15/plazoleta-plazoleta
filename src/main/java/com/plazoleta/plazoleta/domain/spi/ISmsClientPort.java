package com.plazoleta.plazoleta.domain.spi;

/**
 * Puerto hacia el microservicio de mensajeria (envio de SMS).
 */
public interface ISmsClientPort {

    void sendSms(String phoneNumber, String message);
}
