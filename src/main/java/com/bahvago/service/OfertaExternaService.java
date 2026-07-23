package com.bahvago.service;

import com.bahvago.dto.OfertaExterna;
import com.bahvago.dto.OfertaExternaResponse;
import com.bahvago.model.Quarto;
import com.bahvago.util.OfertaMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OfertaExternaService {

    private static final Logger log = LoggerFactory.getLogger(OfertaExternaService.class);
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ISO_LOCAL_DATE;

    // SimpleClientHttpRequestFactory bufferiza o corpo inteiro e envia com Content-Length
    // explícito, evitando um bug do factory padrão baseado no JdkClient (chunked via pipe)
    // que descarta corpos pequenos silenciosamente.
    private final RestClient restClient = RestClient.builder()
            .requestFactory(new SimpleClientHttpRequestFactory())
            .build();

    // Cache em memória por (hotel, checkin, checkout, quartos, pessoas): evita repetir a
    // mesma chamada (lenta) à API externa quando o usuário reabre a mesma busca pouco
    // depois — ex.: alterna entre quartos do mesmo hotel ou reenvia as mesmas datas.
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Value("${api.hoteis.ofertas.url}")
    private String urlOfertas;

    @Value("${api.hoteis.ofertas.cache-ttl-segundos:300}")
    private long cacheTtlSegundos;

    public List<OfertaExterna> buscarOfertas(String hotelNome, LocalDate checkin, LocalDate checkout,
                                              int quartos, int pessoas, Quarto quarto) {
        List<OfertaExterna> ofertas = buscarNaApi(hotelNome, checkin, checkout, quartos, pessoas);
        List<OfertaExterna> filtradas = OfertaMatcher.filtrarPorQuarto(ofertas, quarto);
        return filtradas.stream()
                .sorted(Comparator.comparingDouble(OfertaExternaService::parsePreco))
                .toList();
    }

    private List<OfertaExterna> buscarNaApi(String hotelNome, LocalDate checkin, LocalDate checkout,
                                             int quartos, int pessoas) {
        String chaveCache = chaveCache(hotelNome, checkin, checkout, quartos, pessoas);

        CacheEntry emCache = cache.get(chaveCache);
        if (emCache != null) {
            if (Instant.now().isBefore(emCache.expiraEm())) {
                log.debug("Ofertas obtidas do cache para '{}' ({} a {})", hotelNome, checkin, checkout);
                return emCache.ofertas();
            }
            cache.remove(chaveCache);
        }

        Map<String, Object> corpo = Map.of(
                "hotel", hotelNome,
                "dataCheckin", checkin.format(FORMATO_DATA),
                "dataCheckout", checkout.format(FORMATO_DATA),
                "quartos", quartos,
                "pessoas", pessoas
        );
        log.debug("Enviando para a API externa de ofertas ({}): {}", urlOfertas, corpo);
        try {
            OfertaExternaResponse resposta = restClient.post()
                    .uri(urlOfertas)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(OfertaExternaResponse.class);

            log.debug("Retorno da API externa de ofertas para o hotel '{}': {}", hotelNome, resposta);

            List<OfertaExterna> ofertas = (resposta == null || resposta.getOfertas() == null)
                    ? List.of()
                    : resposta.getOfertas();

            if (!ofertas.isEmpty()) {
                cache.put(chaveCache, new CacheEntry(ofertas, Instant.now().plusSeconds(cacheTtlSegundos)));
            }
            return ofertas;
        } catch (Exception e) {
            log.warn("Falha ao buscar ofertas na API externa para o hotel '{}': {}", hotelNome, e.getMessage());
            return List.of();
        }
    }

    private static String chaveCache(String hotelNome, LocalDate checkin, LocalDate checkout,
                                      int quartos, int pessoas) {
        String hotelNormalizado = hotelNome == null ? "" : hotelNome.trim().toLowerCase();
        return hotelNormalizado + "|" + checkin + "|" + checkout + "|" + quartos + "|" + pessoas;
    }

    private record CacheEntry(List<OfertaExterna> ofertas, Instant expiraEm) {
    }

    private static double parsePreco(OfertaExterna oferta) {
        if (oferta.getPrecoNoite() == null) {
            return Double.MAX_VALUE;
        }
        String limpo = oferta.getPrecoNoite()
                .replaceAll("[^0-9,.]", "")
                .replace(".", "")
                .replace(",", ".");
        try {
            return Double.parseDouble(limpo);
        } catch (NumberFormatException e) {
            return Double.MAX_VALUE;
        }
    }
}
