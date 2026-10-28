package com.plazoleta.plazoleta.infrastructure.configuration;

import com.plazoleta.plazoleta.domain.api.IRestaurantServicePort;
import com.plazoleta.plazoleta.domain.spi.IRestaurantPersistencePort;
import com.plazoleta.plazoleta.domain.spi.IUserClientPort;
import com.plazoleta.plazoleta.domain.usecase.RestaurantUseCase;
import com.plazoleta.plazoleta.infrastructure.output.client.adapter.UserRestClientAdapter;
import com.plazoleta.plazoleta.infrastructure.output.jpa.adapter.RestaurantJpaAdapter;
import com.plazoleta.plazoleta.infrastructure.output.jpa.mapper.IRestaurantEntityMapper;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.IRestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class BeanConfiguration {

    private final IRestaurantRepository restaurantRepository;
    private final IRestaurantEntityMapper restaurantEntityMapper;

    @Value("${clients.usuarios.url}")
    private String usuariosUrl;

    @Bean
    public RestClient usuariosRestClient(RestClient.Builder builder) {
        return builder.baseUrl(usuariosUrl).build();
    }

    @Bean
    public IRestaurantPersistencePort restaurantPersistencePort() {
        return new RestaurantJpaAdapter(restaurantRepository, restaurantEntityMapper);
    }

    @Bean
    public IUserClientPort userClientPort(RestClient usuariosRestClient) {
        return new UserRestClientAdapter(usuariosRestClient);
    }

    @Bean
    public IRestaurantServicePort restaurantServicePort(IUserClientPort userClientPort) {
        return new RestaurantUseCase(restaurantPersistencePort(), userClientPort);
    }
}
