package com.bahvago.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OfertasQuartoResponse {

    private List<OfertaExterna> ofertas;

    private long noites;

    private LocalDate checkin;

    private LocalDate checkout;

    private int pessoas;
}
