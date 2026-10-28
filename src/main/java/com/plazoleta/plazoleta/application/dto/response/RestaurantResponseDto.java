package com.plazoleta.plazoleta.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantResponseDto {

    private Long id;
    private String name;
    private String nit;
    private String address;
    private String phone;
    private String urlLogo;
    private Long ownerId;
}
