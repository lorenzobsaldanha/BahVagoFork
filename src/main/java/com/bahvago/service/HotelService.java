package com.bahvago.service;

import com.bahvago.model.Hotel;
import com.bahvago.repository.HotelRepository;
import com.bahvago.repository.QuartoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class HotelService {

    @Autowired
    private HotelRepository hotelRepository;

    public Hotel criarHotel(Hotel hotel) {
        return hotelRepository.save(hotel);
    }

    public Optional<Hotel> buscarPorId(Integer id) {
        return hotelRepository.findById(id);
    }

    public List<Hotel> listarTodos() {
        return hotelRepository.findAll();
    }

    public List<Hotel> buscarPorCidade(String cidade) {
        return hotelRepository.findByCidade(cidade);
    }

    public List<Hotel> buscarPorEstado(String estado) {
        return hotelRepository.findByEstado(estado);
    }

    public List<Hotel> buscarPorHoteleiro(String cpf) {
        return hotelRepository.findByCpf(cpf);
    }

    public List<Hotel> buscarPorNomeOuCidade(String termo) {
        return hotelRepository.buscarPorNomeOuCidade(termo);
    }

    public Hotel atualizarHotel(Hotel hotel) {
        return hotelRepository.save(hotel);
    }

    public void deletarHotel(Integer id) {
        hotelRepository.deleteById(id);
    }

    @Autowired
    private QuartoRepository quartoRepository;

    public void preencherInformacaoPet(List<Hotel> hoteis) {
        hoteis.forEach(hotel -> {
            boolean temQuartoPet = quartoRepository.existsByCodigoHotelAndAceitaPetTrue(hotel.getId());
            hotel.setAceitaPet(temQuartoPet);
        });
    }
}