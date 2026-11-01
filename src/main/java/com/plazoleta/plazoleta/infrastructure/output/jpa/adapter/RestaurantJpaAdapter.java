package com.plazoleta.plazoleta.infrastructure.output.jpa.adapter;

import com.plazoleta.plazoleta.domain.model.PageResult;
import com.plazoleta.plazoleta.domain.model.Restaurant;
import com.plazoleta.plazoleta.domain.spi.IRestaurantPersistencePort;
import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.RestaurantEntity;
import com.plazoleta.plazoleta.infrastructure.output.jpa.mapper.IRestaurantEntityMapper;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.IRestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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

    @Override
    public PageResult<Restaurant> findAllOrderedByName(int page, int size) {
        // Orden alfabetico sin distinguir mayusculas de minusculas.
        Sort byName = Sort.by(Sort.Order.asc("name").ignoreCase());
        Page<RestaurantEntity> result = restaurantRepository.findAll(PageRequest.of(page, size, byName));
        return new PageResult<>(
                result.getContent().stream().map(restaurantEntityMapper::toRestaurant).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
