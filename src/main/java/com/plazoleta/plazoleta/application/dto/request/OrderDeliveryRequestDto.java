package com.plazoleta.plazoleta.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "PIN de seguridad que el cliente recibio por SMS cuando su pedido quedo LISTO")
public class OrderDeliveryRequestDto {

    @Schema(example = "482913", description = "PIN de 6 digitos")
    @NotBlank(message = "El PIN de seguridad es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El PIN de seguridad debe tener 6 digitos")
    private String pin;
}
