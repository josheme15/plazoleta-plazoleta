package com.plazoleta.plazoleta.infrastructure.output.client.adapter;

import com.plazoleta.plazoleta.domain.model.Order;
import com.plazoleta.plazoleta.domain.model.OrderStatus;
import com.plazoleta.plazoleta.infrastructure.output.client.exception.TraceServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TraceRestClientAdapterTest {

    private static final String BASE_URL = "http://trazabilidad";
    private static final String TOKEN = "jwt-de-quien-cambia-el-estado";

    private MockRestServiceServer server;
    private TraceRestClientAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new TraceRestClientAdapter(builder.build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("empleado@plazoleta.com", TOKEN, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Order order(OrderStatus status, Long employeeId) {
        return new Order(10L, 30L, 1L, LocalDateTime.of(2026, 11, 4, 10, 0), status, employeeId, List.of());
    }

    @Test
    void registerStatusChange_postsTheChangeForwardingToken() {
        server.expect(requestTo(BASE_URL + "/traces"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(content().json("""
                        {"orderId":10,"clientId":30,"restaurantId":1,"employeeId":8,
                         "previousStatus":"PENDIENTE","newStatus":"EN_PREPARACION"}"""))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertDoesNotThrow(() -> adapter.registerStatusChange(order(OrderStatus.EN_PREPARACION, 8L),
                OrderStatus.PENDIENTE));
        server.verify();
    }

    @Test
    void registerStatusChange_ofNewOrder_sendsNullPreviousStatus() {
        server.expect(requestTo(BASE_URL + "/traces"))
                .andExpect(content().json("""
                        {"orderId":10,"clientId":30,"restaurantId":1,"employeeId":null,
                         "previousStatus":null,"newStatus":"PENDIENTE"}"""))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertDoesNotThrow(() -> adapter.registerStatusChange(order(OrderStatus.PENDIENTE, null), null));
        server.verify();
    }

    @Test
    void registerStatusChange_whenTrazabilidadFails_throwsServiceUnavailable() {
        server.expect(requestTo(BASE_URL + "/traces")).andRespond(withServerError());
        Order order = order(OrderStatus.CANCELADO, null);

        assertThrows(TraceServiceUnavailableException.class,
                () -> adapter.registerStatusChange(order, OrderStatus.PENDIENTE));
    }

    @Test
    void registerStatusChange_whenTrazabilidadRejects_throwsServiceUnavailable() {
        server.expect(requestTo(BASE_URL + "/traces")).andRespond(withStatus(HttpStatus.FORBIDDEN));
        Order order = order(OrderStatus.CANCELADO, null);

        assertThrows(TraceServiceUnavailableException.class,
                () -> adapter.registerStatusChange(order, OrderStatus.PENDIENTE));
    }
}
