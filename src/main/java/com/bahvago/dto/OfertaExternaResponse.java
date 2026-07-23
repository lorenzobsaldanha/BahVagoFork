package com.bahvago.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfertaExternaResponse {

    private String status;

    private String timestamp;

    private String hotelResolvido;

    @JsonAlias("quantidade_ofertas")
    private Integer quantidadeOfertas;

    private List<OfertaExterna> ofertas;

    private List<String> avisos;
}
