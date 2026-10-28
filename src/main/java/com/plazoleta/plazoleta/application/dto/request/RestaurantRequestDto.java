package com.plazoleta.plazoleta.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para crear un restaurante")
public class RestaurantRequestDto {

    @Schema(example = "Restaurante El Sabor 2")
    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = ".*\\p{L}.*",
            message = "El nombre del restaurante puede contener numeros, pero no puede estar compuesto solo por numeros")
    private String name;

    @Schema(example = "900123456", description = "Solo numerico")
    @NotBlank(message = "El NIT es obligatorio")
    @Pattern(regexp = "^\\d+$", message = "El NIT debe ser numerico")
    private String nit;

    @Schema(example = "Calle 10 # 20-30")
    @NotBlank(message = "La direccion es obligatoria")
    private String address;

    @Schema(example = "+573005698325", description = "Maximo 13 caracteres, puede iniciar con +")
    @NotBlank(message = "El telefono es obligatorio")
    @Pattern(regexp = "^(\\+\\d{1,12}|\\d{1,13})$",
            message = "El telefono debe tener maximo 13 caracteres, solo digitos y opcionalmente el simbolo + al inicio")
    private String phone;

    @Schema(example = "https://misitio.com/logo.png")
    @NotBlank(message = "La URL del logo es obligatoria")
    private String urlLogo;

    @Schema(example = "2", description = "Id de un usuario con rol propietario")
    @NotNull(message = "El id del propietario es obligatorio")
    @Positive(message = "El id del propietario debe ser positivo")
    private Long ownerId;
}
