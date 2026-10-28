package com.plazoleta.plazoleta.infrastructure.output.client.adapter;

import com.plazoleta.plazoleta.domain.spi.IUserClientPort;
import com.plazoleta.plazoleta.infrastructure.output.client.dto.UserClientResponseDto;
import com.plazoleta.plazoleta.infrastructure.output.client.exception.UserServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Consulta el microservicio de usuarios reenviando el JWT de la peticion actual.
 */
@RequiredArgsConstructor
public class UserRestClientAdapter implements IUserClientPort {

    private static final String BEARER_PREFIX = "Bearer ";

    private final RestClient usuariosRestClient;

    @Override
    public Optional<String> findUserRoleById(Long userId) {
        try {
            UserClientResponseDto user = usuariosRestClient.get()
                    .uri("/users/{id}", userId)
                    .headers(headers -> currentToken()
                            .ifPresent(token -> headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)))
                    .retrieve()
                    .body(UserClientResponseDto.class);
            return Optional.ofNullable(user).map(UserClientResponseDto::getRole);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new UserServiceUnavailableException(e);
        }
    }

    private Optional<String> currentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String token) {
            return Optional.of(token);
        }
        return Optional.empty();
    }
}
