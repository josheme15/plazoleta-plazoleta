package com.plazoleta.plazoleta.infrastructure.security;

import com.plazoleta.plazoleta.application.dto.response.DishResponseDto;
import com.plazoleta.plazoleta.application.dto.response.RestaurantResponseDto;
import com.plazoleta.plazoleta.application.handler.IDishHandler;
import com.plazoleta.plazoleta.application.handler.IRestaurantHandler;
import com.plazoleta.plazoleta.infrastructure.input.rest.DishRestController;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-5: verifica que cada endpoint de plazoleta exija token valido y el rol correspondiente.
 * Usa el filtro JWT y las reglas reales de SecurityConfig; los handlers son mocks.
 * La validacion "es el propietario de ESE restaurante" se prueba en DishUseCaseTest.
 */
@WebMvcTest(controllers = {RestaurantRestController.class, DishRestController.class})
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
}
