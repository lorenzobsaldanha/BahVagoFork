package com.bahvago.controller;

import com.bahvago.model.Avaliacao;
import com.bahvago.model.Hotel;
import com.bahvago.model.Oferta;
import com.bahvago.service.AvaliacaoService;
import com.bahvago.service.HotelService;
import com.bahvago.service.OfertaService;
import com.bahvago.service.QuartoService;
import com.bahvago.service.UsuarioService;
import com.bahvago.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/hoteis")
public class HotelController {

    @Autowired
    private HotelService hotelService;

    @Autowired
    private AvaliacaoService avaliacaoService;

    @Autowired
    private OfertaService ofertaService;

    @Autowired
    private QuartoService quartoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping
    public String listarHoteis(Model model) {
        List<Hotel> hoteis = hotelService.listarTodos();
        hotelService.preencherInformacaoPet(hoteis);
        hoteis.sort((h1, h2) -> {
            Double r1 = h1.getAvaliacaoMedia() != null ? h1.getAvaliacaoMedia() : 0.0;
            Double r2 = h2.getAvaliacaoMedia() != null ? h2.getAvaliacaoMedia() : 0.0;
            return r2.compareTo(r1);
        });
        List<Integer> ids = hoteis.stream().map(Hotel::getId).collect(Collectors.toList());
        model.addAttribute("hoteis", hoteis);
        model.addAttribute("ofertasPorHotel", mapOfertasPorHotel(hoteis));
        model.addAttribute("totalAvaliacoesPorHotel", avaliacaoService.contarAvaliacoesPorHoteis(ids));
        model.addAttribute("termo", "Todos os Hotéis");
        return "resultados";
    }

    @GetMapping("/search")
    public String buscarHoteis(@RequestParam(value = "termo", required = false) String termo, Model model) {
        List<Hotel> hoteis;

        if (termo == null || termo.trim().isEmpty()) {
            hoteis = hotelService.listarTodos();
            model.addAttribute("termo", "");
        } else {
            hoteis = hotelService.buscarPorNomeOuCidade(termo);
            model.addAttribute("termo", termo);
        }

        hotelService.preencherInformacaoPet(hoteis); 

        model.addAttribute("hoteis", hoteis);
        model.addAttribute("ofertasPorHotel", mapOfertasPorHotel(hoteis));
        
        return "resultados";
    }

    @GetMapping("/cidade/{cidade}")
    public String hotelsPorCidade(@PathVariable String cidade, Model model) {
        List<Hotel> hoteis = hotelService.buscarPorCidade(cidade);
        model.addAttribute("hoteis", hoteis);
        model.addAttribute("cidade", cidade);
        model.addAttribute("ofertasPorHotel", mapOfertasPorHotel(hoteis));
        return "resultados";
    }

    @GetMapping("/{id}")
    public String detalheHotel(@PathVariable Integer id, Model model, Authentication authentication) {
        Hotel hotel = hotelService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Hotel não encontrado"));
        Double mediaAvaliacoes = avaliacaoService.calcularMediaAvaliacoes(id);
        
        List<com.bahvago.model.Quarto> quartos = quartoService.buscarPorHotel(id.longValue());
        boolean aceitaPet = quartos.stream().anyMatch(q -> Boolean.TRUE.equals(q.getAceitaPet()));
        boolean usuarioLogado = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName());
        
        model.addAttribute("hotel", hotel);
        model.addAttribute("mediaAvaliacoes", mediaAvaliacoes);
        model.addAttribute("avaliacoes", avaliacaoService.buscarPorHotel(id));
        model.addAttribute("quartos", quartos);
        model.addAttribute("aceitaPet", aceitaPet);
        model.addAttribute("usuarioLogado", usuarioLogado);
        
        return "hotel";
    }

    @PostMapping("/criar")
    public String criarHotel(@ModelAttribute Hotel hotel,
                              @RequestParam(value = "imagemArquivos", required = false) List<MultipartFile> imagemArquivos,
                              @RequestParam(value = "imagensUrls", required = false) String imagensUrls,
                              RedirectAttributes redirectAttributes) {
        List<String> novasUrls = fileStorageService.salvarArquivos(imagemArquivos, "hoteis");
        if (imagensUrls != null && !imagensUrls.trim().isEmpty()) {
            for (String url : imagensUrls.split("[\n,]+")) {
                String cleanUrl = url.trim();
                if (!cleanUrl.isEmpty()) {
                    novasUrls.add(cleanUrl);
                }
            }
        }
        if (!novasUrls.isEmpty()) {
            hotel.getImagens().addAll(novasUrls);
        }
        Hotel novoHotel = hotelService.criarHotel(hotel);
        redirectAttributes.addFlashAttribute("mensagem", "Hotel criado com sucesso!");
        return "redirect:/hoteis/" + novoHotel.getId();
    }

    @PostMapping("/atualizar/{id}")
    public String atualizarHotel(@PathVariable Integer id,
                                  @ModelAttribute Hotel hotel,
                                  @RequestParam(value = "imagemArquivos", required = false) List<MultipartFile> imagemArquivos,
                                  @RequestParam(value = "imagensUrls", required = false) String imagensUrls,
                                  RedirectAttributes redirectAttributes) {
        hotel.setId(id);
        List<String> novasUrls = fileStorageService.salvarArquivos(imagemArquivos, "hoteis");
        if (imagensUrls != null && !imagensUrls.trim().isEmpty()) {
            for (String url : imagensUrls.split("[\n,]+")) {
                String cleanUrl = url.trim();
                if (!cleanUrl.isEmpty()) {
                    novasUrls.add(cleanUrl);
                }
            }
        }
        if (!novasUrls.isEmpty()) {
            hotel.getImagens().clear();
            hotel.getImagens().addAll(novasUrls);
        } else {
            hotelService.buscarPorId(id).ifPresent(h -> {
                if (h.getImagens() != null) {
                    hotel.getImagens().addAll(h.getImagens());
                }
            });
        }
        hotelService.atualizarHotel(hotel);
        redirectAttributes.addFlashAttribute("mensagem", "Hotel atualizado com sucesso!");
        return "redirect:/hoteis/" + id;
    }

    @GetMapping("/deletar/{id}")
    public String deletarHotel(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        hotelService.deletarHotel(id);
        redirectAttributes.addFlashAttribute("mensagem", "Hotel deletado com sucesso!");
        return "redirect:/hoteis";
    }

    @PostMapping("/{id}/avaliacoes")
    @ResponseBody
    public ResponseEntity<?> criarAvaliacao(@PathVariable Integer id,
                                             @RequestParam Float nota,
                                             @RequestParam(required = false) String comentario,
                                             Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("erro", "Você precisa estar logado para avaliar."));
        }

        try {
            com.bahvago.model.Usuario usuario = usuarioService.buscarPorEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            Avaliacao avaliacao = Avaliacao.builder()
                    .nota(nota)
                    .comentario(comentario)
                    .codigoHotel(id)
                    .cpf(usuario.getCpf())
                    .build();

            Avaliacao salva = avaliacaoService.criarAvaliacao(avaliacao);
            return ResponseEntity.ok(Map.of(
                    "sucesso", true,
                    "codigoAvaliacao", salva.getId(),
                    "nomeUsuario", usuario.getNome(),
                    "nota", salva.getNota(),
                    "comentario", salva.getComentario() != null ? salva.getComentario() : "",
                    "data", salva.getData().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Não foi possível salvar a avaliação: " + e.getMessage()));
        }
    }

    private Map<Integer, Oferta> mapOfertasPorHotel(List<Hotel> hoteis) {
        List<Integer> ids = hoteis.stream().map(Hotel::getId).collect(Collectors.toList());
        return ofertaService.mapOfertaPrincipalPorHotel(ids);
    }
}