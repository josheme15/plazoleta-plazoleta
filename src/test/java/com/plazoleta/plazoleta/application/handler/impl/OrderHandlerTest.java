package com.plazoleta.plazoleta.application.handler.impl;

import com.plazoleta.plazoleta.application.dto.request.OrderDishRequestDto;
import com.plazoleta.plazoleta.application.dto.request.OrderRequestDto;
import com.plazoleta.plazoleta.application.dto.response.OrderResponseDto;
import com.plazoleta.plazoleta.application.mapper.IOrderRequestMapper;
import com.plazoleta.plazoleta.application.mapper.IOrderResponseMapper;
import com.plazoleta.plazoleta.domain.api.IOrderServicePort;
import com.plazoleta.plazoleta.domain.model.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
}
