package com.bahvago.util;

import com.bahvago.dto.OfertaExterna;
import com.bahvago.model.Quarto;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OfertaMatcher {

    private static final Map<String, List<String>> SINONIMOS_POR_TIPO = Map.of(

    );

    private OfertaMatcher() {
    }

    public static List<OfertaExterna> filtrarPorQuarto(List<OfertaExterna> ofertas, Quarto quarto) {
        if (ofertas == null || ofertas.isEmpty() || quarto == null) {
            return ofertas;
        }

        List<String> palavrasChave = palavrasChavePara(quarto);
        List<OfertaExterna> filtradas = ofertas.stream()
                .filter(oferta -> combina(oferta.getQuarto(), palavrasChave))
                .collect(Collectors.toList());

        return filtradas.isEmpty() ? ofertas : filtradas;
    }

    private static List<String> palavrasChavePara(Quarto quarto) {
        String tipoNormalizado = normalizar(quarto.getTipo());
        List<String> sinonimos = SINONIMOS_POR_TIPO.getOrDefault(tipoNormalizado, List.of(tipoNormalizado));

        List<String> palavrasDescricao = quarto.getDescricao() == null
                ? List.of()
                : Pattern.compile("\\s+").splitAsStream(normalizar(quarto.getDescricao()))
                        .filter(palavra -> palavra.length() > 3)
                        .collect(Collectors.toList());

        return java.util.stream.Stream.concat(sinonimos.stream(), palavrasDescricao.stream())
                .distinct()
                .collect(Collectors.toList());
    }

    private static boolean combina(String textoOferta, List<String> palavrasChave) {
        String normalizado = normalizar(textoOferta);
        return palavrasChave.stream().anyMatch(palavra -> !palavra.isBlank() && normalizado.contains(palavra));
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase();
    }
}
