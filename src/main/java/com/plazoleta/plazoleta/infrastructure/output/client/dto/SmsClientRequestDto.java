package com.plazoleta.plazoleta.infrastructure.output.client.dto;

/**
 * Cuerpo de POST /sms del microservicio de mensajeria.
 */
public record SmsClientRequestDto(String phoneNumber, String message) {
}
