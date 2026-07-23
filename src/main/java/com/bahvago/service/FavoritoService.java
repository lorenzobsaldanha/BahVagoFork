package com.bahvago.service;

import com.bahvago.controller.dto.OfertaSalvaView;
import com.bahvago.model.Hotel;
import com.bahvago.model.Oferta;
import com.bahvago.model.Quarto;
import com.bahvago.model.QuartoId;
import com.bahvago.model.Salva;
import com.bahvago.repository.OfertaRepository;
import com.bahvago.repository.QuartoRepository;
import com.bahvago.repository.SalvaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FavoritoService {

    @Autowired
    private SalvaRepository salvaRepository;

    @Autowired
    private OfertaRepository ofertaRepository;

    @Autowired
    private QuartoRepository quartoRepository;

    public List<Integer> listarCodigosOfertaPorUsuario(String cpf) {
        return salvaRepository.findCodigosOfertaByCpf(cpf);
    }

    public boolean isSalva(String cpf, Integer codigoOferta) {
        return salvaRepository.existsByCpfAndCodigoOferta(cpf, codigoOferta);
    }

    @Transactional
    public boolean alternar(String cpf, Integer codigoOferta) {
        if (!ofertaRepository.existsById(codigoOferta)) {
            throw new RuntimeException("Oferta nao encontrada");
        }

        if (salvaRepository.existsByCpfAndCodigoOferta(cpf, codigoOferta)) {
            salvaRepository.deleteByCpfAndCodigoOferta(cpf, codigoOferta);
            return false;
        }

        salvaRepository.save(Salva.builder()
            .cpf(cpf)
            .codigoOferta(codigoOferta)
            .build());
        return true;
    }

    @Transactional
    public void remover(String cpf, Integer codigoOferta) {
        salvaRepository.deleteByCpfAndCodigoOferta(cpf, codigoOferta);
    }

    @Transactional(readOnly = true)
    public List<OfertaSalvaView> listarOfertasSalvas(String cpf) {
        List<Salva> salvas = salvaRepository.findByCpf(cpf);
        List<OfertaSalvaView> views = new ArrayList<>();

        for (Salva salva : salvas) {
            Optional<Oferta> ofertaOpt = ofertaRepository.findById(salva.getCodigoOferta());
            if (ofertaOpt.isEmpty()) {
                continue;
            }

            Oferta oferta = ofertaOpt.get();
            Hotel hotel = oferta.getHotel();
            String tipoQuarto = quartoRepository.findById(
                new QuartoId(oferta.getNumero(), oferta.getCodigoHotel().longValue())
            ).map(Quarto::getTipo).orElse("Quarto");

            views.add(OfertaSalvaView.builder()
                .codigoOferta(oferta.getId())
                .codigoHotel(oferta.getCodigoHotel())
                .nomeHotel(hotel != null ? hotel.getNome() : "Hotel")
                .imagemUrl(hotel != null ? hotel.getImagemUrl() : null)
                .cidade(hotel != null && hotel.getLocalizacao() != null ? hotel.getLocalizacao().getCidade() : "")
                .estado(hotel != null && hotel.getLocalizacao() != null ? hotel.getLocalizacao().getEstado() : "")
                .avaliacaoMedia(hotel != null ? hotel.getAvaliacaoMedia() : null)
                .preco(oferta.getPreco())
                .dataCheckIn(oferta.getDataCheckIn())
                .dataCheckOut(oferta.getDataCheckOut())
                .urlOrigem(oferta.getUrlOrigem())
                .numeroQuarto(oferta.getNumero())
                .tipoQuarto(tipoQuarto)
                .build());
        }

        return views;
    }
}
