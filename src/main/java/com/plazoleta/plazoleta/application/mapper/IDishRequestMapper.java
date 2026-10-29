package com.plazoleta.plazoleta.application.mapper;

import com.plazoleta.plazoleta.application.dto.request.DishRequestDto;
import com.plazoleta.plazoleta.domain.model.Dish;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IDishRequestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    Dish toDish(DishRequestDto dishRequestDto);
}
