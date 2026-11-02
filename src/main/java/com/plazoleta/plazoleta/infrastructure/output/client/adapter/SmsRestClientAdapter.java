package com.plazoleta.plazoleta.infrastructure.output.client.adapter;

import com.plazoleta.plazoleta.domain.spi.ISmsClientPort;
import com.plazoleta.plazoleta.infrastructure.output.client.dto.SmsClientRequestDto;
import com.plazoleta.plazoleta.infrastructure.output.client.exception.MessagingServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Envia SMS a traves del microservicio de mensajeria reenviando el JWT de la peticion actual.
 */
@RequiredArgsConstructor
public class SmsRestClientAdapter implements ISmsClientPort {

    private final RestClient mensajeriaRestClient;

    @Override
    public void sendSms(String phoneNumber, String message) {
        try {
            mensajeriaRestClient.post()
                    .uri("/sms")
                    .headers(CurrentRequestToken::addTo)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SmsClientRequestDto(phoneNumber, message))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new MessagingServiceUnavailableException(e);
        }
    }
}
