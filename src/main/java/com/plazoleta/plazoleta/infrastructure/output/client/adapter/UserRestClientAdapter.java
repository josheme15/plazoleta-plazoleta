package com.plazoleta.plazoleta.infrastructure.output.client.adapter;

import com.plazoleta.plazoleta.domain.spi.IUserClientPort;
import com.plazoleta.plazoleta.infrastructure.output.client.dto.UserClientResponseDto;
import com.plazoleta.plazoleta.infrastructure.output.client.exception.UserServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Consulta el microservicio de usuarios reenviando el JWT de la peticion actual.
 */
@RequiredArgsConstructor
public class UserRestClientAdapter implements IUserClientPort {

    private final RestClient usuariosRestClient;

    @Override
    public Optional<String> findUserRoleById(Long userId) {
        return findUser(userId).map(UserClientResponseDto::getRole);
    }

    @Override
    public Optional<String> findUserPhoneById(Long userId) {
        return findUser(userId).map(UserClientResponseDto::getPhone);
    }

    private Optional<UserClientResponseDto> findUser(Long userId) {
        try {
            UserClientResponseDto user = usuariosRestClient.get()
                    .uri("/users/{id}", userId)
                    .headers(CurrentRequestToken::addTo)
                    .retrieve()
                    .body(UserClientResponseDto.class);
            return Optional.ofNullable(user);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new UserServiceUnavailableException(e);
        }
    }
}
