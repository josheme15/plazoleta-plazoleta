package com.plazoleta.plazoleta.infrastructure.output.jpa.mapper;

import com.plazoleta.plazoleta.domain.model.Restaurant;
import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.RestaurantEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IRestaurantEntityMapper {

    RestaurantEntity toEntity(Restaurant restaurant);

    Restaurant toRestaurant(RestaurantEntity restaurantEntity);
}
