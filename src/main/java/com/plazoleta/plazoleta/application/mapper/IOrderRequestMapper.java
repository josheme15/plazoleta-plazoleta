package com.plazoleta.plazoleta.application.mapper;

import com.plazoleta.plazoleta.application.dto.request.OrderDishRequestDto;
import com.plazoleta.plazoleta.application.dto.request.OrderRequestDto;
import com.plazoleta.plazoleta.domain.model.Order;
import com.plazoleta.plazoleta.domain.model.OrderDish;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IOrderRequestMapper {

    // Cliente, estado, fecha y empleado los asigna el dominio.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clientId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "date", ignore = true)
    @Mapping(target = "employeeId", ignore = true)
    Order toOrder(OrderRequestDto orderRequestDto);

    OrderDish toOrderDish(OrderDishRequestDto orderDishRequestDto);
}
