package com.plazoleta.plazoleta.domain.usecase;

import com.plazoleta.plazoleta.domain.exception.CategoryNotFoundException;
import com.plazoleta.plazoleta.domain.exception.InvalidPriceException;
import com.plazoleta.plazoleta.domain.exception.NotRestaurantOwnerException;
import com.plazoleta.plazoleta.domain.exception.RequiredFieldException;
import com.plazoleta.plazoleta.domain.exception.RestaurantNotFoundException;
import com.plazoleta.plazoleta.domain.model.Dish;
import com.plazoleta.plazoleta.domain.model.Restaurant;
import com.plazoleta.plazoleta.domain.spi.IAuthenticatedUserPort;
import com.plazoleta.plazoleta.domain.spi.ICategoryPersistencePort;
import com.plazoleta.plazoleta.domain.spi.IDishPersistencePort;
import com.plazoleta.plazoleta.domain.spi.IRestaurantPersistencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishUseCaseTest {

    private static final Long OWNER_ID = 2L;
    private static final Long RESTAURANT_ID = 10L;
    private static final Long CATEGORY_ID = 3L;

    @Mock
    private IDishPersistencePort dishPersistencePort;

    @Mock
    private IRestaurantPersistencePort restaurantPersistencePort;

    @Mock
    private ICategoryPersistencePort categoryPersistencePort;

    @Mock
    private IAuthenticatedUserPort authenticatedUserPort;

    @InjectMocks
    private DishUseCase dishUseCase;

    private Dish validDish() {
        return new Dish(null, "Bandeja paisa", 25000, "Frijoles, arroz, carne molida y chicharron",
                "https://misitio.com/bandeja.png", CATEGORY_ID, RESTAURANT_ID, null);
    }

    private Restaurant restaurantOwnedBy(Long ownerId) {
        return new Restaurant(RESTAURANT_ID, "Restaurante El Sabor", "900123456", "Calle 10 # 20-30",
                "+573005698325", "https://misitio.com/logo.png", ownerId);
    }

    private void givenOwnerOfRestaurantAndExistingCategory() {
        when(restaurantPersistencePort.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurantOwnedBy(OWNER_ID)));
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(OWNER_ID);
        when(categoryPersistencePort.existsById(CATEGORY_ID)).thenReturn(true);
    }

    @Test
    void createDish_withValidData_savesDishAsActive() {
        Dish dish = validDish();
        givenOwnerOfRestaurantAndExistingCategory();
        when(dishPersistencePort.saveDish(any(Dish.class))).thenAnswer(inv -> inv.getArgument(0));

        Dish result = dishUseCase.createDish(dish);

        ArgumentCaptor<Dish> captor = ArgumentCaptor.forClass(Dish.class);
        verify(dishPersistencePort).saveDish(captor.capture());
        assertTrue(captor.getValue().getActive());
        assertEquals(RESTAURANT_ID, captor.getValue().getRestaurantId());
        assertSame(captor.getValue(), result);
    }

    @Test
    void createDish_whenClientSendsInactive_alwaysSavesAsActive() {
        Dish dish = validDish();
        dish.setActive(false);
        givenOwnerOfRestaurantAndExistingCategory();
        when(dishPersistencePort.saveDish(any(Dish.class))).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(dishUseCase.createDish(dish).getActive());
    }

    @Test
    void createDish_withMinimumPrice_succeeds() {
        Dish dish = validDish();
        dish.setPrice(1);
        givenOwnerOfRestaurantAndExistingCategory();
        when(dishPersistencePort.saveDish(any(Dish.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(1, dishUseCase.createDish(dish).getPrice());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -25000})
    void createDish_withNonPositivePrice_throwsException(int price) {
        Dish dish = validDish();
        dish.setPrice(price);

        assertThrows(InvalidPriceException.class, () -> dishUseCase.createDish(dish));
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @Test
    void createDish_whenRestaurantDoesNotExist_throwsException() {
        Dish dish = validDish();
        when(restaurantPersistencePort.findById(RESTAURANT_ID)).thenReturn(Optional.empty());

        assertThrows(RestaurantNotFoundException.class, () -> dishUseCase.createDish(dish));
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @Test
    void createDish_whenUserIsNotOwnerOfRestaurant_throwsException() {
        Dish dish = validDish();
        when(restaurantPersistencePort.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurantOwnedBy(OWNER_ID)));
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(99L);

        assertThrows(NotRestaurantOwnerException.class, () -> dishUseCase.createDish(dish));
        verify(categoryPersistencePort, never()).existsById(any());
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @Test
    void createDish_whenCategoryDoesNotExist_throwsException() {
        Dish dish = validDish();
        when(restaurantPersistencePort.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurantOwnedBy(OWNER_ID)));
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(OWNER_ID);
        when(categoryPersistencePort.existsById(CATEGORY_ID)).thenReturn(false);

        assertThrows(CategoryNotFoundException.class, () -> dishUseCase.createDish(dish));
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void createDish_withBlankName_throwsException(String value) {
        Dish dish = validDish();
        dish.setName(value);

        assertThrows(RequiredFieldException.class, () -> dishUseCase.createDish(dish));
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @Test
    void createDish_withNullPrice_throwsException() {
        Dish dish = validDish();
        dish.setPrice(null);

        assertThrows(RequiredFieldException.class, () -> dishUseCase.createDish(dish));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createDish_withBlankDescription_throwsException(String value) {
        Dish dish = validDish();
        dish.setDescription(value);

        assertThrows(RequiredFieldException.class, () -> dishUseCase.createDish(dish));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createDish_withBlankUrlImage_throwsException(String value) {
        Dish dish = validDish();
        dish.setUrlImage(value);

        assertThrows(RequiredFieldException.class, () -> dishUseCase.createDish(dish));
    }

    @Test
    void createDish_withNullCategory_throwsException() {
        Dish dish = validDish();
        dish.setCategoryId(null);

        assertThrows(RequiredFieldException.class, () -> dishUseCase.createDish(dish));
    }

    @Test
    void createDish_withNullRestaurant_throwsException() {
        Dish dish = validDish();
        dish.setRestaurantId(null);

        assertThrows(RequiredFieldException.class, () -> dishUseCase.createDish(dish));
        verify(restaurantPersistencePort, never()).findById(any());
    }
}
