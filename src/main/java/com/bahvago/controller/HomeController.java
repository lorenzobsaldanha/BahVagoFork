package com.bahvago.controller;

import com.bahvago.model.Hotel;
import com.bahvago.model.Oferta;
import com.bahvago.service.AvaliacaoService;
import com.bahvago.service.HotelService;
import com.bahvago.service.OfertaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    @Autowired
    private HotelService hotelService;

    @Autowired
    private OfertaService ofertaService;

    @Autowired
    private AvaliacaoService avaliacaoService;

    @GetMapping("/")
    public String index(Model model) {
        List<Hotel> hoteis = hotelService.listarTodos();
        List<Integer> ids = hoteis.stream().map(Hotel::getId).collect(Collectors.toList());
        model.addAttribute("hoteis", hoteis);
        model.addAttribute("ofertasPorHotel", mapOfertasPorHotel(hoteis));
        model.addAttribute("totalAvaliacoesPorHotel", avaliacaoService.contarAvaliacoesPorHoteis(ids));
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/estatisticas")
    public String estatisticas() {
        return "estatisticas";
    }

    @GetMapping("/gerenciar-hotel")
    public String gerenciarHotel() {
        return "gerenciar-hotel";
    }

    @GetMapping("/gerenciar-quartos")
    public String gerenciarQuartos() {
        return "gerenciar-quartos";
    }

    @GetMapping("/gerenciar-avaliacoes")
    public String gerenciarAvaliacoes() {
        return "gerenciar-avaliacoes";
    }

    @GetMapping("/cadastro")
    public String cadastro() {
        return "redirect:/usuarios/cadastro";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/login-hoteleiro")
    public String loginHoteleiro() {
        return "login-hoteleiro";
    }

    private Map<Integer, Oferta> mapOfertasPorHotel(List<Hotel> hoteis) {
        List<Integer> ids = hoteis.stream().map(Hotel::getId).collect(Collectors.toList());
        return ofertaService.mapOfertaPrincipalPorHotel(ids);
    }
}
