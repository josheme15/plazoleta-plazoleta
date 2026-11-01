package com.plazoleta.plazoleta.infrastructure.security;

import com.plazoleta.plazoleta.application.dto.response.DishMenuItemResponseDto;
import com.plazoleta.plazoleta.application.dto.response.DishResponseDto;
import com.plazoleta.plazoleta.application.dto.response.OrderDishResponseDto;
import com.plazoleta.plazoleta.application.dto.response.OrderResponseDto;
import com.plazoleta.plazoleta.application.dto.response.PageResponseDto;
import com.plazoleta.plazoleta.application.dto.response.RestaurantListItemResponseDto;
import com.plazoleta.plazoleta.application.dto.response.RestaurantResponseDto;
import com.plazoleta.plazoleta.application.handler.IDishHandler;
import com.plazoleta.plazoleta.application.handler.IOrderHandler;
import com.plazoleta.plazoleta.application.handler.IRestaurantHandler;
import com.plazoleta.plazoleta.domain.model.OrderStatus;
import com.plazoleta.plazoleta.infrastructure.input.rest.DishRestController;
import com.plazoleta.plazoleta.infrastructure.input.rest.OrderRestController;
import com.plazoleta.plazoleta.infrastructure.input.rest.RestaurantMenuRestController;
import com.plazoleta.plazoleta.infrastructure.input.rest.RestaurantRestController;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-5: verifica que cada endpoint de plazoleta exija token valido y el rol correspondiente.
 * Usa el filtro JWT y las reglas reales de SecurityConfig; los handlers son mocks.
 * La validacion "es el propietario de ESE restaurante" se prueba en DishUseCaseTest.
 */
@WebMvcTest(controllers = {RestaurantRestController.class, DishRestController.class,
        RestaurantMenuRestController.class, OrderRestController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = "security.jwt.secret=" + EndpointSecurityTest.SECRET)
class EndpointSecurityTest {

    static final String SECRET = "cGxhem9sZXRhLWRlLWNvbWlkYXMtc2VjcmV0by1zdXBlci1zZWd1cm8tMjU2LWJpdHM=";

    private static final String RESTAURANT_BODY = """
            {"name":"Restaurante El Sabor","nit":"900123456","address":"Calle 10","phone":"+573005698325",
             "urlLogo":"https://misitio.com/logo.png","ownerId":2}""";
    private static final String DISH_BODY = """
            {"name":"Bandeja paisa","price":25000,"description":"Frijoles y arroz",
             "urlImage":"https://misitio.com/bandeja.png","categoryId":2,"restaurantId":1}""";
    private static final String DISH_UPDATE_BODY = "{\"price\":28000}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IRestaurantHandler restaurantHandler;

    @MockitoBean
    private IDishHandler dishHandler;

    @MockitoBean
    private IOrderHandler orderHandler;

    /** Token firmado con la misma clave que usa plazoleta-usuarios. */
    private String bearer(String role) {
        Date now = new Date();
        String token = Jwts.builder()
                .subject(role.toLowerCase() + "@plazoleta.com")
                .claim(JwtService.CLAIM_USER_ID, 2L)
                .claim(JwtService.CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();
        return "Bearer " + token;
    }

    // ---------- POST /restaurants: solo ADMIN ----------

    @Test
    void createRestaurant_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/restaurants").contentType(MediaType.APPLICATION_JSON).content(RESTAURANT_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRestaurant_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(post("/restaurants").header(HttpHeaders.AUTHORIZATION, "Bearer token.invalido.firma")
                        .contentType(MediaType.APPLICATION_JSON).content(RESTAURANT_BODY))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "EMPLOYEE", "CLIENT"})
    void createRestaurant_withNonAdminRole_returns403(String role) throws Exception {
        mockMvc.perform(post("/restaurants").header(HttpHeaders.AUTHORIZATION, bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(RESTAURANT_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRestaurant_withAdminRole_returns201() throws Exception {
        when(restaurantHandler.createRestaurant(any())).thenReturn(new RestaurantResponseDto());

        mockMvc.perform(post("/restaurants").header(HttpHeaders.AUTHORIZATION, bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(RESTAURANT_BODY))
                .andExpect(status().isCreated());
    }

    // ---------- POST /dishes: solo OWNER ----------

    @Test
    void createDish_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/dishes").contentType(MediaType.APPLICATION_JSON).content(DISH_BODY))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "EMPLOYEE", "CLIENT"})
    void createDish_withNonOwnerRole_returns403(String role) throws Exception {
        mockMvc.perform(post("/dishes").header(HttpHeaders.AUTHORIZATION, bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(DISH_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void createDish_withOwnerRole_returns201() throws Exception {
        when(dishHandler.createDish(any())).thenReturn(new DishResponseDto());

        mockMvc.perform(post("/dishes").header(HttpHeaders.AUTHORIZATION, bearer("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON).content(DISH_BODY))
                .andExpect(status().isCreated());
    }

    // ---------- PATCH /dishes/{id}: solo OWNER ----------

    @Test
    void updateDish_withoutToken_returns401() throws Exception {
        mockMvc.perform(patch("/dishes/1").contentType(MediaType.APPLICATION_JSON).content(DISH_UPDATE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "EMPLOYEE", "CLIENT"})
    void updateDish_withNonOwnerRole_returns403(String role) throws Exception {
        mockMvc.perform(patch("/dishes/1").header(HttpHeaders.AUTHORIZATION, bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(DISH_UPDATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateDish_withOwnerRole_returns200() throws Exception {
        when(dishHandler.updateDish(anyLong(), any())).thenReturn(new DishResponseDto());

        mockMvc.perform(patch("/dishes/1").header(HttpHeaders.AUTHORIZATION, bearer("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON).content(DISH_UPDATE_BODY))
                .andExpect(status().isOk());
    }

    // ---------- PATCH /dishes/{id}/status: solo OWNER (HU-7) ----------

    private static final String DISH_STATUS_BODY = "{\"active\":false}";

    @Test
    void changeDishStatus_withoutToken_returns401() throws Exception {
        mockMvc.perform(patch("/dishes/1/status").contentType(MediaType.APPLICATION_JSON).content(DISH_STATUS_BODY))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "EMPLOYEE", "CLIENT"})
    void changeDishStatus_withNonOwnerRole_returns403(String role) throws Exception {
        mockMvc.perform(patch("/dishes/1/status").header(HttpHeaders.AUTHORIZATION, bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(DISH_STATUS_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeDishStatus_withOwnerRole_returns200() throws Exception {
        when(dishHandler.changeDishStatus(anyLong(), any())).thenReturn(new DishResponseDto());

        mockMvc.perform(patch("/dishes/1/status").header(HttpHeaders.AUTHORIZATION, bearer("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON).content(DISH_STATUS_BODY))
                .andExpect(status().isOk());
    }

    // ---------- GET /restaurants: solo CLIENT (HU-9) ----------

    @Test
    void listRestaurants_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/restaurants")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "OWNER", "EMPLOYEE"})
    void listRestaurants_withNonClientRole_returns403(String role) throws Exception {
        mockMvc.perform(get("/restaurants").header(HttpHeaders.AUTHORIZATION, bearer(role)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listRestaurants_withClientRole_returnsOnlyNameAndLogo() throws Exception {
        when(restaurantHandler.listRestaurants(0, 10)).thenReturn(new PageResponseDto<>(
                List.of(new RestaurantListItemResponseDto("Arepas", "https://logo/1.png")), 0, 10, 1, 1));

        mockMvc.perform(get("/restaurants").header(HttpHeaders.AUTHORIZATION, bearer("CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Arepas"))
                .andExpect(jsonPath("$.content[0].urlLogo").value("https://logo/1.png"))
                .andExpect(jsonPath("$.content[0].nit").doesNotExist())
                .andExpect(jsonPath("$.content[0].ownerId").doesNotExist())
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listRestaurants_passesPageAndSizeParameters() throws Exception {
        when(restaurantHandler.listRestaurants(2, 5)).thenReturn(new PageResponseDto<>(List.of(), 2, 5, 11, 3));

        mockMvc.perform(get("/restaurants").param("page", "2").param("size", "5")
                        .header(HttpHeaders.AUTHORIZATION, bearer("CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void listRestaurants_withNonNumericSize_returns400() throws Exception {
        mockMvc.perform(get("/restaurants").param("size", "abc").header(HttpHeaders.AUTHORIZATION, bearer("CLIENT")))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /restaurants/{id}/dishes: solo CLIENT (HU-10) ----------

    @Test
    void listDishesByRestaurant_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/restaurants/1/dishes")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "OWNER", "EMPLOYEE"})
    void listDishesByRestaurant_withNonClientRole_returns403(String role) throws Exception {
        mockMvc.perform(get("/restaurants/1/dishes").header(HttpHeaders.AUTHORIZATION, bearer(role)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listDishesByRestaurant_withClientRole_returnsMenuPage() throws Exception {
        when(dishHandler.listDishesByRestaurant(1L, null, 0, 10)).thenReturn(new PageResponseDto<>(
                List.of(new DishMenuItemResponseDto(5L, "Bandeja paisa", 32000, "Frijoles", "https://img", 2L)),
                0, 10, 1, 1));

        mockMvc.perform(get("/restaurants/1/dishes").header(HttpHeaders.AUTHORIZATION, bearer("CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].name").value("Bandeja paisa"))
                .andExpect(jsonPath("$.content[0].categoryId").value(2))
                .andExpect(jsonPath("$.content[0].active").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listDishesByRestaurant_passesCategoryAndPaginationParameters() throws Exception {
        when(dishHandler.listDishesByRestaurant(1L, 3L, 2, 5)).thenReturn(new PageResponseDto<>(List.of(), 2, 5, 0, 0));

        mockMvc.perform(get("/restaurants/1/dishes").param("categoryId", "3").param("page", "2").param("size", "5")
                        .header(HttpHeaders.AUTHORIZATION, bearer("CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void listDishesByRestaurant_withNonNumericCategory_returns400() throws Exception {
        mockMvc.perform(get("/restaurants/1/dishes").param("categoryId", "abc")
                        .header(HttpHeaders.AUTHORIZATION, bearer("CLIENT")))
                .andExpect(status().isBadRequest());
    }

    // ---------- POST /orders: solo CLIENT (HU-11) ----------

    private static final String ORDER_BODY = "{\"restaurantId\":1,\"dishes\":[{\"dishId\":5,\"quantity\":2}]}";

    @Test
    void createOrder_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(ORDER_BODY))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "OWNER", "EMPLOYEE"})
    void createOrder_withNonClientRole_returns403(String role) throws Exception {
        mockMvc.perform(post("/orders").header(HttpHeaders.AUTHORIZATION, bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(ORDER_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void createOrder_withClientRole_returns201() throws Exception {
        when(orderHandler.createOrder(any())).thenReturn(new OrderResponseDto(1L, 2L, 1L, null, "PENDIENTE", null,
                List.of(new OrderDishResponseDto(5L, 2))));

        mockMvc.perform(post("/orders").header(HttpHeaders.AUTHORIZATION, bearer("CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(ORDER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.dishes[0].quantity").value(2));
    }

    @Test
    void createOrder_withEmptyDishList_returns400() throws Exception {
        mockMvc.perform(post("/orders").header(HttpHeaders.AUTHORIZATION, bearer("CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"restaurantId\":1,\"dishes\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_withZeroQuantity_returns400() throws Exception {
        mockMvc.perform(post("/orders").header(HttpHeaders.AUTHORIZATION, bearer("CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"restaurantId\":1,\"dishes\":[{\"dishId\":5,\"quantity\":0}]}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /orders: solo EMPLOYEE (HU-12) ----------

    @Test
    void listRestaurantOrders_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/orders")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "OWNER", "CLIENT"})
    void listRestaurantOrders_withNonEmployeeRole_returns403(String role) throws Exception {
        mockMvc.perform(get("/orders").header(HttpHeaders.AUTHORIZATION, bearer(role)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listRestaurantOrders_withEmployeeRole_returnsAllOrderFields() throws Exception {
        when(orderHandler.listRestaurantOrders(null, 0, 10)).thenReturn(new PageResponseDto<>(List.of(
                new OrderResponseDto(1L, 30L, 10L, null, "PENDIENTE", null, List.of(new OrderDishResponseDto(5L, 2)))),
                0, 10, 1, 1));

        mockMvc.perform(get("/orders").header(HttpHeaders.AUTHORIZATION, bearer("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].clientId").value(30))
                .andExpect(jsonPath("$.content[0].restaurantId").value(10))
                .andExpect(jsonPath("$.content[0].status").value("PENDIENTE"))
                .andExpect(jsonPath("$.content[0].dishes[0].dishId").value(5))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listRestaurantOrders_passesStatusAndPaginationParameters() throws Exception {
        when(orderHandler.listRestaurantOrders(OrderStatus.LISTO, 1, 5))
                .thenReturn(new PageResponseDto<>(List.of(), 1, 5, 0, 0));

        mockMvc.perform(get("/orders").param("status", "LISTO").param("page", "1").param("size", "5")
                        .header(HttpHeaders.AUTHORIZATION, bearer("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void listRestaurantOrders_withInvalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/orders").param("status", "INVENTADO")
                        .header(HttpHeaders.AUTHORIZATION, bearer("EMPLOYEE")))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /restaurants/{id}: cualquier usuario autenticado (HU-6) ----------

    @Test
    void getRestaurantById_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/restaurants/1")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "OWNER", "EMPLOYEE", "CLIENT"})
    void getRestaurantById_withAnyAuthenticatedRole_returns200(String role) throws Exception {
        when(restaurantHandler.getRestaurantById(anyLong())).thenReturn(new RestaurantResponseDto());

        mockMvc.perform(get("/restaurants/1").header(HttpHeaders.AUTHORIZATION, bearer(role)))
                .andExpect(status().isOk());
    }
}
