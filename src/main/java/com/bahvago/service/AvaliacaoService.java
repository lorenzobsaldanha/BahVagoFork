package com.bahvago.service;

import com.bahvago.model.Avaliacao;
import com.bahvago.repository.AvaliacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AvaliacaoService {

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    @Autowired
    private com.bahvago.repository.HotelRepository hotelRepository;

    public Avaliacao criarAvaliacao(Avaliacao avaliacao) {
        Avaliacao salva = avaliacaoRepository.save(avaliacao);
        atualizarMediaHotel(avaliacao.getCodigoHotel());
        return salva;
    }

    public Optional<Avaliacao> buscarPorId(Integer id) {
        return avaliacaoRepository.findById(id);
    }

    public List<Avaliacao> listarTodas() {
        return avaliacaoRepository.findAll();
    }

    public List<Avaliacao> buscarPorHotel(Integer codigoHotel) {
        return avaliacaoRepository.findByCodigoHotel(codigoHotel);
    }

    public List<Avaliacao> buscarPorUsuario(String cpf) {
        return avaliacaoRepository.findByCpf(cpf);
    }

    public Avaliacao atualizarAvaliacao(Avaliacao avaliacao) {
        Avaliacao salva = avaliacaoRepository.save(avaliacao);
        atualizarMediaHotel(avaliacao.getCodigoHotel());
        return salva;
    }

    public void deletarAvaliacao(Integer id) {
        avaliacaoRepository.findById(id).ifPresent(av -> {
            avaliacaoRepository.deleteById(id);
            atualizarMediaHotel(av.getCodigoHotel());
        });
    }

    public Double calcularMediaAvaliacoes(Integer codigoHotel) {
        List<Avaliacao> avaliacoes = buscarPorHotel(codigoHotel);
        if (avaliacoes.isEmpty()) {
            return 0.0;
        }
        return avaliacoes.stream()
            .mapToDouble(Avaliacao::getNota)
            .average()
            .orElse(0.0);
    }

    public Map<Integer, Long> contarAvaliacoesPorHoteis(List<Integer> hotelIds) {
        return hotelIds.stream()
            .collect(Collectors.toMap(
                id -> id,
                id -> avaliacaoRepository.countByCodigoHotel(id)
            ));
    }

    private void atualizarMediaHotel(Integer codigoHotel) {
        Double media = calcularMediaAvaliacoes(codigoHotel);
        hotelRepository.findById(codigoHotel).ifPresent(hotel -> {
            hotel.setAvaliacaoMedia(media);
            hotelRepository.save(hotel);
        });
    }
}