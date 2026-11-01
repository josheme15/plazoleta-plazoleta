package com.plazoleta.plazoleta.infrastructure.output.jpa.repository;

import com.plazoleta.plazoleta.domain.model.OrderStatus;
import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface IOrderRepository extends JpaRepository<OrderEntity, Long> {

    boolean existsByClientIdAndStatusIn(Long clientId, Collection<OrderStatus> statuses);

    Page<OrderEntity> findByRestaurant_Id(Long restaurantId, Pageable pageable);

    Page<OrderEntity> findByRestaurant_IdAndStatus(Long restaurantId, OrderStatus status, Pageable pageable);
}
