package com.plazoleta.plazoleta.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estado del plato: true para activarlo, false para desactivarlo")
public class DishStatusRequestDto {

    @Schema(example = "false")
    @NotNull(message = "El campo active es obligatorio (true o false)")
    private Boolean active;
}
