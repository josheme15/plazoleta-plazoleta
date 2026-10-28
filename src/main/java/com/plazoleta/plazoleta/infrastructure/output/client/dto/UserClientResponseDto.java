package com.plazoleta.plazoleta.infrastructure.output.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta de GET /users/{id} del microservicio de usuarios. Solo se mapean los campos necesarios.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserClientResponseDto {

    private Long id;
    private String role;
}
