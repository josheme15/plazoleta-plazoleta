package com.plazoleta.plazoleta.domain.usecase;

import com.plazoleta.plazoleta.domain.exception.InvalidNitException;
import com.plazoleta.plazoleta.domain.exception.InvalidPhoneException;
import com.plazoleta.plazoleta.domain.exception.InvalidRestaurantNameException;
import com.plazoleta.plazoleta.domain.exception.NitAlreadyExistsException;
import com.plazoleta.plazoleta.domain.exception.OwnerNotFoundException;
import com.plazoleta.plazoleta.domain.exception.RequiredFieldException;
import com.plazoleta.plazoleta.domain.exception.RestaurantNotFoundException;
import com.plazoleta.plazoleta.domain.exception.UserIsNotOwnerException;
import com.plazoleta.plazoleta.domain.model.Restaurant;
import com.plazoleta.plazoleta.domain.spi.IRestaurantPersistencePort;
import com.plazoleta.plazoleta.domain.spi.IUserClientPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantUseCaseTest {

    private static final Long OWNER_ID = 2L;

    @Mock
    private IRestaurantPersistencePort restaurantPersistencePort;

    @Mock
    private IUserClientPort userClientPort;

    @InjectMocks
    private RestaurantUseCase restaurantUseCase;

    private Restaurant validRestaurant() {
        return new Restaurant(null, "Restaurante El Sabor 2", "900123456", "Calle 10 # 20-30",
                "+573005698325", "https://misitio.com/logo.png", OWNER_ID);
    }

    private void givenValidOwner() {
        when(restaurantPersistencePort.existsByNit(any())).thenReturn(false);
        when(userClientPort.findUserRoleById(OWNER_ID)).thenReturn(Optional.of("OWNER"));
    }

    @Test
    void createRestaurant_withValidData_savesRestaurant() {
        Restaurant restaurant = validRestaurant();
        givenValidOwner();
        when(restaurantPersistencePort.saveRestaurant(restaurant)).thenAnswer(inv -> {
            Restaurant saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Restaurant result = restaurantUseCase.createRestaurant(restaurant);

        assertSame(restaurant, result);
        assertEquals(1L, result.getId());
        verify(userClientPort).findUserRoleById(OWNER_ID);
        verify(restaurantPersistencePort).saveRestaurant(restaurant);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Restaurante 123", "123 Sabores", "El Corral", "7 Mares", "Ñaño"})
    void createRestaurant_withNameContainingLetters_succeeds(String name) {
        Restaurant restaurant = validRestaurant();
        restaurant.setName(name);
        givenValidOwner();
        when(restaurantPersistencePort.saveRestaurant(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(name, restaurantUseCase.createRestaurant(restaurant).getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "123 456", "0", "1-2-3"})
    void createRestaurant_withNumericOnlyName_throwsException(String name) {
        Restaurant restaurant = validRestaurant();
        restaurant.setName(name);

        assertThrows(InvalidRestaurantNameException.class, () -> restaurantUseCase.createRestaurant(restaurant));
        verify(restaurantPersistencePort, never()).saveRestaurant(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"900123456A", "900-123-456", "900.123.456", "+900123456", "NIT900"})
    void createRestaurant_withNonNumericNit_throwsException(String nit) {
        Restaurant restaurant = validRestaurant();
        restaurant.setNit(nit);

        assertThrows(InvalidNitException.class, () -> restaurantUseCase.createRestaurant(restaurant));
        verify(restaurantPersistencePort, never()).saveRestaurant(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"3005698325", "+573005698325", "1234567890123"})
    void createRestaurant_withValidPhones_succeeds(String phone) {
        Restaurant restaurant = validRestaurant();
        restaurant.setPhone(phone);
        givenValidOwner();
        when(restaurantPersistencePort.saveRestaurant(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(phone, restaurantUseCase.createRestaurant(restaurant).getPhone());
    }

    @ParameterizedTest
    @ValueSource(strings = {"+5730056983251", "12345678901234", "300abc8325", "57+3005698325", "+"})
    void createRestaurant_withInvalidPhone_throwsException(String phone) {
        Restaurant restaurant = validRestaurant();
        restaurant.setPhone(phone);

        assertThrows(InvalidPhoneException.class, () -> restaurantUseCase.createRestaurant(restaurant));
        verify(restaurantPersistencePort, never()).saveRestaurant(any());
    }

    @Test
    void createRestaurant_whenNitAlreadyExists_throwsException() {
        Restaurant restaurant = validRestaurant();
        when(restaurantPersistencePort.existsByNit(restaurant.getNit())).thenReturn(true);

        assertThrows(NitAlreadyExistsException.class, () -> restaurantUseCase.createRestaurant(restaurant));
        verify(userClientPort, never()).findUserRoleById(anyLong());
        verify(restaurantPersistencePort, never()).saveRestaurant(any());
    }

    @Test
    void createRestaurant_whenOwnerDoesNotExist_throwsException() {
        Restaurant restaurant = validRestaurant();
        when(restaurantPersistencePort.existsByNit(any())).thenReturn(false);
        when(userClientPort.findUserRoleById(OWNER_ID)).thenReturn(Optional.empty());

        assertThrows(OwnerNotFoundException.class, () -> restaurantUseCase.createRestaurant(restaurant));
        verify(restaurantPersistencePort, never()).saveRestaurant(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "EMPLOYEE", "CLIENT"})
    void createRestaurant_whenUserIsNotOwner_throwsException(String role) {
        Restaurant restaurant = validRestaurant();
        when(restaurantPersistencePort.existsByNit(any())).thenReturn(false);
        when(userClientPort.findUserRoleById(OWNER_ID)).thenReturn(Optional.of(role));

        assertThrows(UserIsNotOwnerException.class, () -> restaurantUseCase.createRestaurant(restaurant));
        verify(restaurantPersistencePort, never()).saveRestaurant(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void createRestaurant_withBlankName_throwsException(String value) {
        Restaurant restaurant = validRestaurant();
        restaurant.setName(value);

        assertThrows(RequiredFieldException.class, () -> restaurantUseCase.createRestaurant(restaurant));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createRestaurant_withBlankNit_throwsException(String value) {
        Restaurant restaurant = validRestaurant();
        restaurant.setNit(value);

        assertThrows(RequiredFieldException.class, () -> restaurantUseCase.createRestaurant(restaurant));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createRestaurant_withBlankAddress_throwsException(String value) {
        Restaurant restaurant = validRestaurant();
        restaurant.setAddress(value);

        assertThrows(RequiredFieldException.class, () -> restaurantUseCase.createRestaurant(restaurant));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createRestaurant_withBlankPhone_throwsException(String value) {
        Restaurant restaurant = validRestaurant();
        restaurant.setPhone(value);

        assertThrows(RequiredFieldException.class, () -> restaurantUseCase.createRestaurant(restaurant));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createRestaurant_withBlankUrlLogo_throwsException(String value) {
        Restaurant restaurant = validRestaurant();
        restaurant.setUrlLogo(value);

        assertThrows(RequiredFieldException.class, () -> restaurantUseCase.createRestaurant(restaurant));
    }

    @Test
    void createRestaurant_withNullOwnerId_throwsException() {
        Restaurant restaurant = validRestaurant();
        restaurant.setOwnerId(null);

        assertThrows(RequiredFieldException.class, () -> restaurantUseCase.createRestaurant(restaurant));
        verify(userClientPort, never()).findUserRoleById(any());
    }

    // ---------- HU-6: consultar restaurante por id (lo usa usuarios al crear empleados) ----------

    @Test
    void getRestaurantById_whenRestaurantExists_returnsRestaurantWithOwner() {
        Restaurant stored = validRestaurant();
        stored.setId(1L);
        when(restaurantPersistencePort.findById(1L)).thenReturn(Optional.of(stored));

        Restaurant result = restaurantUseCase.getRestaurantById(1L);

        assertSame(stored, result);
        assertEquals(OWNER_ID, result.getOwnerId());
    }

    @Test
    void getRestaurantById_whenRestaurantDoesNotExist_throwsException() {
        when(restaurantPersistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RestaurantNotFoundException.class, () -> restaurantUseCase.getRestaurantById(99L));
    }

    @Test
    void getRestaurantById_withNullId_throwsException() {
        assertThrows(RequiredFieldException.class, () -> restaurantUseCase.getRestaurantById(null));
        verify(restaurantPersistencePort, never()).findById(any());
    }
}
