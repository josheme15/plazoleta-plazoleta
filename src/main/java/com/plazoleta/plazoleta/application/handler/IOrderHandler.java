package com.plazoleta.plazoleta.application.handler;

import com.plazoleta.plazoleta.application.dto.request.OrderDeliveryRequestDto;
import com.plazoleta.plazoleta.application.dto.request.OrderRequestDto;
import com.plazoleta.plazoleta.application.dto.response.OrderResponseDto;
import com.plazoleta.plazoleta.application.dto.response.PageResponseDto;
import com.plazoleta.plazoleta.domain.model.OrderStatus;

public interface IOrderHandler {

    OrderResponseDto createOrder(OrderRequestDto orderRequestDto);

    PageResponseDto<OrderResponseDto> listRestaurantOrders(OrderStatus status, Integer page, Integer size);

    OrderResponseDto assignOrder(Long orderId);

    OrderResponseDto markOrderReady(Long orderId);

    OrderResponseDto deliverOrder(Long orderId, OrderDeliveryRequestDto orderDeliveryRequestDto);
}
