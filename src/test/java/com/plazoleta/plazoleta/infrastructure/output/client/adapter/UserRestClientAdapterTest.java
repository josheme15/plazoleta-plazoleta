package com.plazoleta.plazoleta.infrastructure.output.client.adapter;

import com.plazoleta.plazoleta.infrastructure.output.client.exception.UserServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UserRestClientAdapterTest {

    private static final String BASE_URL = "http://usuarios";
    private static final String TOKEN = "jwt-del-admin";

    private MockRestServiceServer server;
    private UserRestClientAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new UserRestClientAdapter(builder.build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@plazoleta.com", TOKEN, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findUserRoleById_whenUserExists_returnsRoleAndForwardsToken() {
        server.expect(requestTo(BASE_URL + "/users/2"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andRespond(withSuccess("{\"id\":2,\"email\":\"p@correo.com\",\"role\":\"OWNER\"}",
                        MediaType.APPLICATION_JSON));

        assertEquals(Optional.of("OWNER"), adapter.findUserRoleById(2L));
        server.verify();
    }

    @Test
    void findUserRoleById_whenUserDoesNotExist_returnsEmpty() {
        server.expect(requestTo(BASE_URL + "/users/99"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertEquals(Optional.empty(), adapter.findUserRoleById(99L));
    }

    @Test
    void findUserRoleById_whenUsuariosFails_throwsServiceUnavailable() {
        server.expect(requestTo(BASE_URL + "/users/2"))
                .andRespond(withServerError());

        assertThrows(UserServiceUnavailableException.class, () -> adapter.findUserRoleById(2L));
    }

    @Test
    void findUserRoleById_whenUsuariosRejectsToken_throwsServiceUnavailable() {
        server.expect(requestTo(BASE_URL + "/users/2"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(UserServiceUnavailableException.class, () -> adapter.findUserRoleById(2L));
    }
}
