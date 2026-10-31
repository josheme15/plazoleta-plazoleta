package com.plazoleta.plazoleta.domain.usecase;

import com.plazoleta.plazoleta.domain.exception.CategoryNotFoundException;
import com.plazoleta.plazoleta.domain.exception.DishNotFoundException;
import com.plazoleta.plazoleta.domain.exception.InvalidDescriptionException;
import com.plazoleta.plazoleta.domain.exception.InvalidPriceException;
import com.plazoleta.plazoleta.domain.exception.NoDishFieldsToUpdateException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    // ---------- HU-4: editar plato ----------

    private static final Long DISH_ID = 50L;

    private Dish storedDish() {
        return new Dish(DISH_ID, "Bandeja paisa", 25000, "Descripcion original",
                "https://misitio.com/bandeja.png", CATEGORY_ID, RESTAURANT_ID, true);
    }

    private Dish changes(Integer price, String description) {
        Dish changes = new Dish();
        changes.setPrice(price);
        changes.setDescription(description);
        return changes;
    }

    private void givenStoredDishOwnedByAuthenticatedUser() {
        when(dishPersistencePort.findById(DISH_ID)).thenReturn(Optional.of(storedDish()));
        when(restaurantPersistencePort.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurantOwnedBy(OWNER_ID)));
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(OWNER_ID);
        when(dishPersistencePort.saveDish(any(Dish.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void updateDish_withPriceAndDescription_updatesBothFields() {
        givenStoredDishOwnedByAuthenticatedUser();

        Dish result = dishUseCase.updateDish(DISH_ID, changes(30000, "Nueva descripcion"));

        assertEquals(30000, result.getPrice());
        assertEquals("Nueva descripcion", result.getDescription());
        verify(dishPersistencePort).saveDish(result);
    }

    @Test
    void updateDish_withOnlyPrice_keepsDescription() {
        givenStoredDishOwnedByAuthenticatedUser();

        Dish result = dishUseCase.updateDish(DISH_ID, changes(18000, null));

        assertEquals(18000, result.getPrice());
        assertEquals("Descripcion original", result.getDescription());
    }

    @Test
    void updateDish_withOnlyDescription_keepsPrice() {
        givenStoredDishOwnedByAuthenticatedUser();

        Dish result = dishUseCase.updateDish(DISH_ID, changes(null, "Solo cambia la descripcion"));

        assertEquals(25000, result.getPrice());
        assertEquals("Solo cambia la descripcion", result.getDescription());
    }

    @Test
    void updateDish_ignoresFieldsOtherThanPriceAndDescription() {
        givenStoredDishOwnedByAuthenticatedUser();
        Dish changes = new Dish(999L, "Otro nombre", 30000, "Nueva descripcion", "https://otra.png",
                5L, 777L, false);

        Dish result = dishUseCase.updateDish(DISH_ID, changes);

        assertEquals(DISH_ID, result.getId());
        assertEquals("Bandeja paisa", result.getName());
        assertEquals("https://misitio.com/bandeja.png", result.getUrlImage());
        assertEquals(CATEGORY_ID, result.getCategoryId());
        assertEquals(RESTAURANT_ID, result.getRestaurantId());
        assertTrue(result.getActive());
        assertEquals(30000, result.getPrice());
    }

    @Test
    void updateDish_withoutPriceNorDescription_throwsException() {
        assertThrows(NoDishFieldsToUpdateException.class, () -> dishUseCase.updateDish(DISH_ID, changes(null, null)));
        verify(dishPersistencePort, never()).findById(any());
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -30000})
    void updateDish_withNonPositivePrice_throwsException(int price) {
        assertThrows(InvalidPriceException.class, () -> dishUseCase.updateDish(DISH_ID, changes(price, null)));
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void updateDish_withBlankDescription_throwsException(String description) {
        assertThrows(InvalidDescriptionException.class,
                () -> dishUseCase.updateDish(DISH_ID, changes(null, description)));
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @Test
    void updateDish_whenDishDoesNotExist_throwsException() {
        when(dishPersistencePort.findById(DISH_ID)).thenReturn(Optional.empty());

        assertThrows(DishNotFoundException.class, () -> dishUseCase.updateDish(DISH_ID, changes(30000, null)));
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @Test
    void updateDish_whenUserIsNotOwnerOfRestaurant_throwsException() {
        when(dishPersistencePort.findById(DISH_ID)).thenReturn(Optional.of(storedDish()));
        when(restaurantPersistencePort.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurantOwnedBy(OWNER_ID)));
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(99L);

        assertThrows(NotRestaurantOwnerException.class,
                () -> dishUseCase.updateDish(DISH_ID, changes(30000, null)));
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @Test
    void updateDish_withNullId_throwsException() {
        assertThrows(RequiredFieldException.class, () -> dishUseCase.updateDish(null, changes(30000, null)));
        verify(dishPersistencePort, never()).findById(any());
    }

    // ---------- HU-7: activar / desactivar plato ----------

    @Test
    void changeDishStatus_deactivate_savesDishAsInactive() {
        givenStoredDishOwnedByAuthenticatedUser();

        Dish result = dishUseCase.changeDishStatus(DISH_ID, false);

        assertFalse(result.getActive());
        verify(dishPersistencePort).saveDish(result);
    }

    @Test
    void changeDishStatus_activate_savesDishAsActive() {
        Dish inactive = storedDish();
        inactive.setActive(false);
        when(dishPersistencePort.findById(DISH_ID)).thenReturn(Optional.of(inactive));
        when(restaurantPersistencePort.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurantOwnedBy(OWNER_ID)));
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(OWNER_ID);
        when(dishPersistencePort.saveDish(any(Dish.class))).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(dishUseCase.changeDishStatus(DISH_ID, true).getActive());
    }

    @Test
    void changeDishStatus_withSameStatus_isIdempotent() {
        givenStoredDishOwnedByAuthenticatedUser();

        assertTrue(dishUseCase.changeDishStatus(DISH_ID, true).getActive());
    }

    @Test
    void changeDishStatus_onlyChangesActive_keepsOtherFields() {
        givenStoredDishOwnedByAuthenticatedUser();

        Dish result = dishUseCase.changeDishStatus(DISH_ID, false);

        assertEquals("Bandeja paisa", result.getName());
        assertEquals(25000, result.getPrice());
        assertEquals("Descripcion original", result.getDescription());
        assertEquals(CATEGORY_ID, result.getCategoryId());
        assertEquals(RESTAURANT_ID, result.getRestaurantId());
    }

    @Test
    void changeDishStatus_whenUserIsNotOwnerOfRestaurant_throwsException() {
        when(dishPersistencePort.findById(DISH_ID)).thenReturn(Optional.of(storedDish()));
        when(restaurantPersistencePort.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurantOwnedBy(OWNER_ID)));
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(99L);

        assertThrows(NotRestaurantOwnerException.class, () -> dishUseCase.changeDishStatus(DISH_ID, false));
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @Test
    void changeDishStatus_whenDishDoesNotExist_throwsException() {
        when(dishPersistencePort.findById(DISH_ID)).thenReturn(Optional.empty());

        assertThrows(DishNotFoundException.class, () -> dishUseCase.changeDishStatus(DISH_ID, false));
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @Test
    void changeDishStatus_withNullActive_throwsException() {
        assertThrows(RequiredFieldException.class, () -> dishUseCase.changeDishStatus(DISH_ID, null));
        verify(dishPersistencePort, never()).findById(any());
    }

    @Test
    void changeDishStatus_withNullId_throwsException() {
        assertThrows(RequiredFieldException.class, () -> dishUseCase.changeDishStatus(null, false));
        verify(dishPersistencePort, never()).findById(any());
    }
}
