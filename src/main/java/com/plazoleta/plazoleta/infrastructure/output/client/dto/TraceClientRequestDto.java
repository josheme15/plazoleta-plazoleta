package com.plazoleta.plazoleta.infrastructure.output.client.dto;

/**
 * Cuerpo de POST /traces del microservicio de trazabilidad.
 */
public record TraceClientRequestDto(Long orderId, Long clientId, Long restaurantId, Long employeeId,
                                    String previousStatus, String newStatus) {
}
