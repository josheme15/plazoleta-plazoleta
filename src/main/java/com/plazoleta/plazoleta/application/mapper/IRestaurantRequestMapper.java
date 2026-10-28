package com.plazoleta.plazoleta.application.mapper;

import com.plazoleta.plazoleta.application.dto.request.RestaurantRequestDto;
import com.plazoleta.plazoleta.domain.model.Restaurant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IRestaurantRequestMapper {

    @Mapping(target = "id", ignore = true)
    Restaurant toRestaurant(RestaurantRequestDto restaurantRequestDto);
}
