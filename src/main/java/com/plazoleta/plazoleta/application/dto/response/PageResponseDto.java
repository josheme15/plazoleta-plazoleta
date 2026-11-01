package com.plazoleta.plazoleta.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pagina de resultados")
public class PageResponseDto<T> {

    private List<T> content;

    @Schema(example = "0", description = "Numero de pagina (desde 0)")
    private int page;

    @Schema(example = "10", description = "Cantidad de elementos por pagina")
    private int size;

    @Schema(example = "25", description = "Total de elementos")
    private long totalElements;

    @Schema(example = "3", description = "Total de paginas")
    private int totalPages;
}
