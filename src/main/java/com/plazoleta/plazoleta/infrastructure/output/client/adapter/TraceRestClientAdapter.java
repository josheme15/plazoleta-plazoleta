package com.plazoleta.plazoleta.infrastructure.output.client.adapter;

import com.plazoleta.plazoleta.domain.model.Order;
import com.plazoleta.plazoleta.domain.model.OrderStatus;
import com.plazoleta.plazoleta.domain.spi.ITraceClientPort;
import com.plazoleta.plazoleta.infrastructure.output.client.dto.TraceClientRequestDto;
import com.plazoleta.plazoleta.infrastructure.output.client.exception.TraceServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Registra los cambios de estado en el microservicio de trazabilidad reenviando el JWT de quien hizo el cambio.
 */
@RequiredArgsConstructor
public class TraceRestClientAdapter implements ITraceClientPort {

    private final RestClient trazabilidadRestClient;

    @Override
    public void registerStatusChange(Order order, OrderStatus previousStatus) {
        TraceClientRequestDto body = new TraceClientRequestDto(order.getId(), order.getClientId(),
                order.getRestaurantId(), order.getEmployeeId(),
                previousStatus == null ? null : previousStatus.name(), order.getStatus().name());
        try {
            trazabilidadRestClient.post()
                    .uri("/traces")
                    .headers(CurrentRequestToken::addTo)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new TraceServiceUnavailableException(e);
        }
    }
}
