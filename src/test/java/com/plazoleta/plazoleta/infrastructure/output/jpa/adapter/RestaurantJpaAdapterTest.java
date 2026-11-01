package com.plazoleta.plazoleta.infrastructure.output.jpa.adapter;

import com.plazoleta.plazoleta.domain.model.PageResult;
import com.plazoleta.plazoleta.domain.model.Restaurant;
import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.RestaurantEntity;
import com.plazoleta.plazoleta.infrastructure.output.jpa.mapper.IRestaurantEntityMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantJpaAdapterTest {

    @Mock
    private IRestaurantRepository restaurantRepository;

    @Mock
    private IRestaurantEntityMapper restaurantEntityMapper;

    @InjectMocks
    private RestaurantJpaAdapter adapter;

    private RestaurantEntity entity(Long id, String name) {
        return new RestaurantEntity(id, name, "90012345" + id, "Calle 1", "3001234567", "https://logo/" + id, 2L);
    }

    @Test
    void findAllOrderedByName_requestsPageSortedAlphabeticallyIgnoringCase() {
        when(restaurantRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        adapter.findAllOrderedByName(2, 5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(restaurantRepository).findAll(captor.capture());
        Pageable pageable = captor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        Sort.Order order = pageable.getSort().getOrderFor("name");
        assertNotNull(order);
        assertTrue(order.isAscending());
        assertTrue(order.isIgnoreCase());
    }

    @Test
    void findAllOrderedByName_mapsContentAndPaginationData() {
        RestaurantEntity first = entity(1L, "Arepas");
        RestaurantEntity second = entity(2L, "burger");
        Pageable pageable = PageRequest.of(0, 2, Sort.by("name"));
        when(restaurantRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), pageable, 5));
        when(restaurantEntityMapper.toRestaurant(first))
                .thenReturn(new Restaurant(1L, "Arepas", null, null, null, "https://logo/1", 2L));
        when(restaurantEntityMapper.toRestaurant(second))
                .thenReturn(new Restaurant(2L, "burger", null, null, null, "https://logo/2", 2L));

        PageResult<Restaurant> result = adapter.findAllOrderedByName(0, 2);

        assertEquals(List.of("Arepas", "burger"), result.getContent().stream().map(Restaurant::getName).toList());
        assertEquals(0, result.getPage());
        assertEquals(2, result.getSize());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
    }
}
