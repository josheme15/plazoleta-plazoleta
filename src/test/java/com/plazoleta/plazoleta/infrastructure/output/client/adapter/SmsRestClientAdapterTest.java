package com.plazoleta.plazoleta.infrastructure.output.client.adapter;

import com.plazoleta.plazoleta.infrastructure.output.client.exception.MessagingServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SmsRestClientAdapterTest {

    private static final String BASE_URL = "http://mensajeria";
    private static final String TOKEN = "jwt-del-empleado";

    private MockRestServiceServer server;
    private SmsRestClientAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new SmsRestClientAdapter(builder.build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("empleado@plazoleta.com", TOKEN, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sendSms_postsPhoneAndMessageForwardingToken() {
        server.expect(requestTo(BASE_URL + "/sms"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(content().json("{\"phoneNumber\":\"+573005698325\",\"message\":\"Pedido listo\"}"))
                .andRespond(withSuccess("{\"status\":\"ENVIADO\"}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> adapter.sendSms("+573005698325", "Pedido listo"));
        server.verify();
    }

    @Test
    void sendSms_whenMensajeriaFails_throwsServiceUnavailable() {
        server.expect(requestTo(BASE_URL + "/sms")).andRespond(withServerError());

        assertThrows(MessagingServiceUnavailableException.class,
                () -> adapter.sendSms("+573005698325", "Pedido listo"));
    }

    @Test
    void sendSms_whenMensajeriaRejectsRequest_throwsServiceUnavailable() {
        server.expect(requestTo(BASE_URL + "/sms")).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(MessagingServiceUnavailableException.class,
                () -> adapter.sendSms("+573005698325", "Pedido listo"));
    }
}
