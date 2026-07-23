package com.bahvago.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfertaExterna {

    private String parceiro;

    private String botao;

    private String url;

    private String quarto;

    // @JsonAlias só afeta a leitura: aceita "preco_noite" (nome usado pela API externa) na
    // desserialização, mas serializa de volta como "precoNoite" no nosso endpoint /ofertas,
    // consistente com o restante do JSON consumido pelo front-end.
    @JsonAlias("preco_noite")
    private String precoNoite;

    @JsonAlias("preco_total")
    private String precoTotal;

    private List<String> atributos;
}
