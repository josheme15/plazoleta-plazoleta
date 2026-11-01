package com.plazoleta.plazoleta.application.handler;

import com.plazoleta.plazoleta.application.dto.request.OrderRequestDto;
import com.plazoleta.plazoleta.application.dto.response.OrderResponseDto;

public interface IOrderHandler {

    OrderResponseDto createOrder(OrderRequestDto orderRequestDto);
}
