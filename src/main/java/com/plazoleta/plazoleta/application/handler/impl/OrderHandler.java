package com.plazoleta.plazoleta.application.handler.impl;

import com.plazoleta.plazoleta.application.dto.request.OrderRequestDto;
import com.plazoleta.plazoleta.application.dto.response.OrderResponseDto;
import com.plazoleta.plazoleta.application.dto.response.PageResponseDto;
import com.plazoleta.plazoleta.application.handler.IOrderHandler;
import com.plazoleta.plazoleta.application.mapper.IOrderRequestMapper;
import com.plazoleta.plazoleta.application.mapper.IOrderResponseMapper;
import com.plazoleta.plazoleta.domain.api.IOrderServicePort;
import com.plazoleta.plazoleta.domain.model.OrderStatus;
import com.plazoleta.plazoleta.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderHandler implements IOrderHandler {

    private final IOrderServicePort orderServicePort;
    private final IOrderRequestMapper orderRequestMapper;
    private final IOrderResponseMapper orderResponseMapper;

    @Override
    public OrderResponseDto createOrder(OrderRequestDto orderRequestDto) {
        return orderResponseMapper.toResponse(orderServicePort.createOrder(orderRequestMapper.toOrder(orderRequestDto)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<OrderResponseDto> listRestaurantOrders(OrderStatus status, Integer page, Integer size) {
        PageResult<OrderResponseDto> result =
                orderServicePort.listRestaurantOrders(status, page, size).map(orderResponseMapper::toResponse);
        return new PageResponseDto<>(result.getContent(), result.getPage(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
