package com.bahvago.controller;

import com.bahvago.dto.OfertaExterna;
import com.bahvago.dto.OfertasQuartoResponse;
import com.bahvago.model.Hotel;
import com.bahvago.model.Quarto;
import com.bahvago.service.FileStorageService;
import com.bahvago.service.HotelService;
import com.bahvago.service.OfertaExternaService;
import com.bahvago.service.QuartoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
@RequestMapping("/quartos")
public class QuartoController {

    @Autowired
    private QuartoService quartoService;

    @Autowired
    private HotelService hotelService;

    @Autowired
    private OfertaExternaService ofertaExternaService;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/hotel/{codigoHotel}")
    public String listarQuartosPorHotel(@PathVariable Long codigoHotel, Model model) {
        List<Quarto> quartos = quartoService.buscarPorHotel(codigoHotel);
        model.addAttribute("quartos", quartos);
        model.addAttribute("codigoHotel", codigoHotel);
        return "novo-quarto";
    }

    @GetMapping("/hotel/{codigoHotel}/numero/{numero}")
    public String detalheQuarto(@PathVariable Long codigoHotel,
                                 @PathVariable Integer numero,
                                 @RequestParam(required = false) String checkin,
                                 @RequestParam(required = false) String checkout,
                                 @RequestParam(required = false) Integer pessoas,
                                 @RequestParam(required = false) Integer quartos,
                                 Model model) {
        ResolucaoOfertas resolucao = resolverOfertas(codigoHotel, numero, checkin, checkout, pessoas, quartos);

        model.addAttribute("quarto", resolucao.quarto());
        model.addAttribute("hotel", resolucao.hotel());
        model.addAttribute("ofertas", resolucao.ofertas());
        model.addAttribute("checkinResolvido", resolucao.checkin());
        model.addAttribute("checkoutResolvido", resolucao.checkout());
        model.addAttribute("pessoasResolvido", resolucao.pessoas());
        model.addAttribute("noites", resolucao.noites());
        return "quarto";
    }

    @GetMapping("/hotel/{codigoHotel}/numero/{numero}/ofertas")
    @ResponseBody
    public OfertasQuartoResponse buscarOfertasQuarto(@PathVariable Long codigoHotel,
                                                       @PathVariable Integer numero,
                                                       @RequestParam(required = false) String checkin,
                                                       @RequestParam(required = false) String checkout,
                                                       @RequestParam(required = false) Integer pessoas,
                                                       @RequestParam(required = false) Integer quartos) {
        ResolucaoOfertas resolucao = resolverOfertas(codigoHotel, numero, checkin, checkout, pessoas, quartos);
        return new OfertasQuartoResponse(resolucao.ofertas(), resolucao.noites(),
                resolucao.checkin(), resolucao.checkout(), resolucao.pessoas());
    }

    private ResolucaoOfertas resolverOfertas(Long codigoHotel, Integer numero, String checkin, String checkout,
                                              Integer pessoas, Integer quartos) {
        Quarto quarto = quartoService.buscarPorId(numero, codigoHotel)
                .orElseThrow(() -> new RuntimeException("Quarto não encontrado"));
        Hotel hotel = hotelService.buscarPorId(codigoHotel.intValue())
                .orElseThrow(() -> new RuntimeException("Hotel não encontrado"));

        LocalDate checkinResolvido = parseDataOuPadrao(checkin, LocalDate.now().plusDays(30));
        LocalDate checkoutResolvido = parseDataOuPadrao(checkout, checkinResolvido.plusDays(2));
        if (!checkoutResolvido.isAfter(checkinResolvido)) {
            checkoutResolvido = checkinResolvido.plusDays(2);
        }
        int pessoasResolvido = (pessoas != null && pessoas > 0)
                ? pessoas
                : (quarto.getCapacidade() != null ? quarto.getCapacidade() : 2);
        int quartosResolvido = (quartos != null && quartos > 0) ? quartos : 1;

        List<OfertaExterna> ofertas = ofertaExternaService.buscarOfertas(
                hotel.getNome(), checkinResolvido, checkoutResolvido, quartosResolvido, pessoasResolvido, quarto);

        long noites = ChronoUnit.DAYS.between(checkinResolvido, checkoutResolvido);
        return new ResolucaoOfertas(quarto, hotel, ofertas, checkinResolvido, checkoutResolvido, pessoasResolvido, noites);
    }

    private LocalDate parseDataOuPadrao(String valor, LocalDate padrao) {
        if (!StringUtils.hasText(valor)) {
            return padrao;
        }
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            return padrao;
        }
    }

    private record ResolucaoOfertas(Quarto quarto, Hotel hotel, List<OfertaExterna> ofertas,
                                     LocalDate checkin, LocalDate checkout, int pessoas, long noites) {
    }

    @PostMapping("/criar")
    public String criarQuarto(@ModelAttribute Quarto quarto,
                               @RequestParam(value = "imagemArquivos", required = false) List<MultipartFile> imagemArquivos,
                               @RequestParam(value = "imagensUrls", required = false) String imagensUrls,
                               RedirectAttributes redirectAttributes) {
        List<String> novasUrls = fileStorageService.salvarArquivos(imagemArquivos, "quartos");
        if (imagensUrls != null && !imagensUrls.trim().isEmpty()) {
            for (String url : imagensUrls.split("[\n,]+")) {
                String cleanUrl = url.trim();
                if (!cleanUrl.isEmpty()) {
                    novasUrls.add(cleanUrl);
                }
            }
        }
        if (!novasUrls.isEmpty()) {
            quarto.getImagens().addAll(novasUrls);
        }
        quartoService.criarQuarto(quarto);
        redirectAttributes.addFlashAttribute("mensagem", "Quarto criado com sucesso!");
        return "redirect:/quartos/hotel/" + quarto.getCodigoHotel();
    }

    @PostMapping("/atualizar/{codigoHotel}/{numero}")
    public String atualizarQuarto(@PathVariable Long codigoHotel,
                                   @PathVariable Integer numero,
                                   @ModelAttribute Quarto quarto,
                                   @RequestParam(value = "imagemArquivos", required = false) List<MultipartFile> imagemArquivos,
                                   @RequestParam(value = "imagensUrls", required = false) String imagensUrls,
                                   RedirectAttributes redirectAttributes) {
        quarto.setNumero(numero);
        quarto.setCodigoHotel(codigoHotel);
        List<String> novasUrls = fileStorageService.salvarArquivos(imagemArquivos, "quartos");
        if (imagensUrls != null && !imagensUrls.trim().isEmpty()) {
            for (String url : imagensUrls.split("[\n,]+")) {
                String cleanUrl = url.trim();
                if (!cleanUrl.isEmpty()) {
                    novasUrls.add(cleanUrl);
                }
            }
        }
        if (!novasUrls.isEmpty()) {
            quarto.getImagens().clear();
            quarto.getImagens().addAll(novasUrls);
        } else {
            quartoService.buscarPorId(numero, codigoHotel).ifPresent(q -> {
                if (q.getImagens() != null) {
                    quarto.getImagens().addAll(q.getImagens());
                }
            });
        }
        quartoService.atualizarQuarto(quarto);
        redirectAttributes.addFlashAttribute("mensagem", "Quarto atualizado com sucesso!");
        return "redirect:/quartos/hotel/" + codigoHotel + "/numero/" + numero;
    }

    @GetMapping("/deletar/{codigoHotel}/{numero}")
    public String deletarQuarto(@PathVariable Long codigoHotel,
                                 @PathVariable Integer numero,
                                 RedirectAttributes redirectAttributes) {
        quartoService.buscarPorId(numero, codigoHotel)
                .orElseThrow(() -> new RuntimeException("Quarto não encontrado"));
        quartoService.deletarQuarto(numero, codigoHotel);
        redirectAttributes.addFlashAttribute("mensagem", "Quarto deletado com sucesso!");
        return "redirect:/quartos/hotel/" + codigoHotel;
    }
}