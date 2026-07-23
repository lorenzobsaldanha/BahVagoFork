package com.bahvago.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OfertaSalvaView {

    private Integer codigoOferta;
    private Integer codigoHotel;
    private String nomeHotel;
    private String imagemUrl;
    private String cidade;
    private String estado;
    private Double avaliacaoMedia;
    private Double preco;
    private LocalDate dataCheckIn;
    private LocalDate dataCheckOut;
    private String urlOrigem;
    private Integer numeroQuarto;
    private String tipoQuarto;
}
