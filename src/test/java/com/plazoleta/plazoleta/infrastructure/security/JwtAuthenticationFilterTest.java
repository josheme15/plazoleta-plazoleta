package com.plazoleta.plazoleta.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "cGxhem9sZXRhLWRlLWNvbWlkYXMtc2VjcmV0by1zdXBlci1zZWd1cm8tMjU2LWJpdHM=";

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new JwtService(SECRET));

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String token(String role, Long restaurantId) {
        var builder = Jwts.builder()
                .subject("usuario@plazoleta.com")
                .claim(JwtService.CLAIM_USER_ID, 8L)
                .claim(JwtService.CLAIM_ROLE, role);
        if (restaurantId != null) {
            builder.claim(JwtService.CLAIM_RESTAURANT_ID, restaurantId);
        }
        return builder.expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();
    }

    private Authentication filterWith(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void employeeToken_loadsIdRoleAndRestaurant() throws Exception {
        Authentication authentication = filterWith(token("EMPLOYEE", 10L));

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        assertEquals(8L, user.id());
        assertEquals("usuario@plazoleta.com", user.email());
        assertEquals(10L, user.restaurantId());
        assertEquals("ROLE_EMPLOYEE", authentication.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void tokenWithoutRestaurant_leavesRestaurantNull() throws Exception {
        Authentication authentication = filterWith(token("CLIENT", null));

        assertNull(((AuthenticatedUser) authentication.getPrincipal()).restaurantId());
    }

    @Test
    void invalidToken_leavesContextUnauthenticated() throws Exception {
        assertNull(filterWith("token.invalido.firma"));
    }
}
