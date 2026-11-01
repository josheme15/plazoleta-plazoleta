package com.plazoleta.plazoleta.application.mapper;

import com.plazoleta.plazoleta.application.dto.response.OrderDishResponseDto;
import com.plazoleta.plazoleta.application.dto.response.OrderResponseDto;
import com.plazoleta.plazoleta.domain.model.Order;
import com.plazoleta.plazoleta.domain.model.OrderDish;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IOrderResponseMapper {

    OrderResponseDto toResponse(Order order);

    OrderDishResponseDto toDishResponse(OrderDish orderDish);
}
