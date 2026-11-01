package com.plazoleta.plazoleta.infrastructure.output.jpa.adapter;

import com.plazoleta.plazoleta.domain.model.Dish;
import com.plazoleta.plazoleta.domain.model.PageResult;
import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.DishEntity;
import com.plazoleta.plazoleta.infrastructure.output.jpa.mapper.IDishEntityMapper;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.ICategoryRepository;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.IDishRepository;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.IRestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishJpaAdapterTest {

    @Mock
    private IDishRepository dishRepository;

    @Mock
    private IRestaurantRepository restaurantRepository;

    @Mock
    private ICategoryRepository categoryRepository;

    @Mock
    private IDishEntityMapper dishEntityMapper;

    @InjectMocks
    private DishJpaAdapter adapter;

    @Test
    void findActiveByRestaurant_withoutCategory_queriesActiveDishesSortedByName() {
        when(dishRepository.findByRestaurant_IdAndActiveTrue(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        adapter.findActiveByRestaurant(10L, null, 1, 5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(dishRepository).findByRestaurant_IdAndActiveTrue(eq(10L), captor.capture());
        verify(dishRepository, never()).findByRestaurant_IdAndCategory_IdAndActiveTrue(anyLong(), anyLong(), any());
        Pageable pageable = captor.getValue();
        assertEquals(1, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        Sort.Order order = pageable.getSort().getOrderFor("name");
        assertNotNull(order);
        assertTrue(order.isAscending());
        assertTrue(order.isIgnoreCase());
    }

    @Test
    void findActiveByRestaurant_withCategory_queriesByCategory() {
        when(dishRepository.findByRestaurant_IdAndCategory_IdAndActiveTrue(eq(10L), eq(2L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        adapter.findActiveByRestaurant(10L, 2L, 0, 10);

        verify(dishRepository).findByRestaurant_IdAndCategory_IdAndActiveTrue(eq(10L), eq(2L), any(Pageable.class));
        verify(dishRepository, never()).findByRestaurant_IdAndActiveTrue(anyLong(), any());
    }

    @Test
    void findActiveByRestaurant_mapsContentAndPaginationData() {
        DishEntity entity = new DishEntity();
        Dish dish = new Dish(1L, "Arroz con pollo", 18000, "Arroz", "https://img", 2L, 10L, true);
        when(dishRepository.findByRestaurant_IdAndActiveTrue(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 1), 3));
        when(dishEntityMapper.toDish(entity)).thenReturn(dish);

        PageResult<Dish> result = adapter.findActiveByRestaurant(10L, null, 0, 1);

        assertEquals(List.of(dish), result.getContent());
        assertEquals(0, result.getPage());
        assertEquals(1, result.getSize());
        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
    }
}
