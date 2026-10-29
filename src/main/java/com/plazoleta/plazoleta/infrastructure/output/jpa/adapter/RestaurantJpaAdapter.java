package com.plazoleta.plazoleta.infrastructure.output.jpa.adapter;

import com.plazoleta.plazoleta.domain.model.Restaurant;
import com.plazoleta.plazoleta.domain.spi.IRestaurantPersistencePort;
import com.plazoleta.plazoleta.infrastructure.output.jpa.mapper.IRestaurantEntityMapper;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.IRestaurantRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class RestaurantJpaAdapter implements IRestaurantPersistencePort {

    private final IRestaurantRepository restaurantRepository;
    private final IRestaurantEntityMapper restaurantEntityMapper;

    @Override
    public Restaurant saveRestaurant(Restaurant restaurant) {
        return restaurantEntityMapper.toRestaurant(
                restaurantRepository.save(restaurantEntityMapper.toEntity(restaurant)));
    }

    @Override
    public boolean existsByNit(String nit) {
        return restaurantRepository.existsByNit(nit);
    }

    @Override
    public Optional<Restaurant> findById(Long restaurantId) {
        return restaurantRepository.findById(restaurantId).map(restaurantEntityMapper::toRestaurant);
    }
}
