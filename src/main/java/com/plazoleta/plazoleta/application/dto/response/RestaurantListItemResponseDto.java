package com.plazoleta.plazoleta.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Restaurante en el listado para clientes: solo nombre y logo (HU-9).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Restaurante del listado: solo nombre y URL del logo")
public class RestaurantListItemResponseDto {

    @Schema(example = "El Corral")
    private String name;

    @Schema(example = "https://misitio.com/logo.png")
    private String urlLogo;
}
