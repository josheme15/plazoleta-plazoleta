package com.plazoleta.plazoleta.application.handler.impl;

import com.plazoleta.plazoleta.application.dto.request.OrderDeliveryRequestDto;
import com.plazoleta.plazoleta.application.dto.request.OrderDishRequestDto;
import com.plazoleta.plazoleta.application.dto.request.OrderRequestDto;
import com.plazoleta.plazoleta.application.dto.response.OrderResponseDto;
import com.plazoleta.plazoleta.application.dto.response.PageResponseDto;
import com.plazoleta.plazoleta.application.mapper.IOrderRequestMapper;
import com.plazoleta.plazoleta.application.mapper.IOrderResponseMapper;
import com.plazoleta.plazoleta.domain.api.IOrderServicePort;
import com.plazoleta.plazoleta.domain.model.Order;
import com.plazoleta.plazoleta.domain.model.OrderStatus;
import com.plazoleta.plazoleta.domain.model.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderHandlerTest {

    @Mock
    private IOrderServicePort orderServicePort;

    @Mock
    private IOrderRequestMapper orderRequestMapper;

    @Mock
    private IOrderResponseMapper orderResponseMapper;

    @InjectMocks
    private OrderHandler orderHandler;

    @Test
    void createOrder_mapsRequestCallsDomainAndMapsResponse() {
        OrderRequestDto request = new OrderRequestDto(1L, List.of(new OrderDishRequestDto(5L, 2)));
        Order order = new Order();
        Order saved = new Order();
        OrderResponseDto response = new OrderResponseDto();
        when(orderRequestMapper.toOrder(request)).thenReturn(order);
        when(orderServicePort.createOrder(order)).thenReturn(saved);
        when(orderResponseMapper.toResponse(saved)).thenReturn(response);

        assertSame(response, orderHandler.createOrder(request));
        verify(orderServicePort).createOrder(order);
    }

    @Test
    void listRestaurantOrders_mapsEachOrderAndKeepsPagination() {
        Order order = new Order();
        OrderResponseDto response = new OrderResponseDto();
        when(orderServicePort.listRestaurantOrders(OrderStatus.PENDIENTE, 1, 5))
                .thenReturn(new PageResult<>(List.of(order), 1, 5, 6, 2));
        when(orderResponseMapper.toResponse(order)).thenReturn(response);

        PageResponseDto<OrderResponseDto> result = orderHandler.listRestaurantOrders(OrderStatus.PENDIENTE, 1, 5);

        assertSame(response, result.getContent().get(0));
        assertEquals(1, result.getPage());
        assertEquals(5, result.getSize());
        assertEquals(6, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
    }

    @Test
    void assignOrder_callsDomainAndMapsResponse() {
        Order assigned = new Order();
        OrderResponseDto response = new OrderResponseDto();
        when(orderServicePort.assignOrder(100L)).thenReturn(assigned);
        when(orderResponseMapper.toResponse(assigned)).thenReturn(response);

        assertSame(response, orderHandler.assignOrder(100L));
    }

    @Test
    void markOrderReady_callsDomainAndMapsResponse() {
        Order ready = new Order();
        OrderResponseDto response = new OrderResponseDto();
        when(orderServicePort.markOrderReady(100L)).thenReturn(ready);
        when(orderResponseMapper.toResponse(ready)).thenReturn(response);

        assertSame(response, orderHandler.markOrderReady(100L));
    }

    @Test
    void deliverOrder_passesPinToDomainAndMapsResponse() {
        Order delivered = new Order();
        OrderResponseDto response = new OrderResponseDto();
        when(orderServicePort.deliverOrder(100L, "304930")).thenReturn(delivered);
        when(orderResponseMapper.toResponse(delivered)).thenReturn(response);

        assertSame(response, orderHandler.deliverOrder(100L, new OrderDeliveryRequestDto("304930")));
    }
}
