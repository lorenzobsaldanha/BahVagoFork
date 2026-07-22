package com.bahvago.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "HotelEstatisticas")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CodigoHotel")
    private Integer id;

    @Column(name = "Nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "Descricao", nullable = false, length = 2000)
    private String descricao;

    @Column(name = "NumeroAcesso")
    private Integer numeroAcesso;

    @Column(name = "AvaliacaoMedia", columnDefinition = "DECIMAL(10,2)")
    private Double avaliacaoMedia;

    @Column(name = "CPF", nullable = false, length = 11)
    private String cpf;

    @Column(name = "Latitude", nullable = false)
    private Integer latitude;

    @Column(name = "Longitude", nullable = false)
    private Integer longitude;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ImagemHotel", joinColumns = @JoinColumn(name = "CodigoHotel"))
    @Column(name = "Url", length = 1000)
    @Builder.Default
    private List<String> imagens = new ArrayList<>();

    public String getImagemUrl() {
        return (imagens != null && !imagens.isEmpty()) ? imagens.get(0) : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "Latitude", referencedColumnName = "Latitude", insertable = false, updatable = false),
        @JoinColumn(name = "Longitude", referencedColumnName = "Longitude", insertable = false, updatable = false)
    })
    private Localizacao localizacao;

    @Column(name = "DataCriacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    protected void onCreate() {
        this.dataCriacao = LocalDateTime.now();
    }

    @Transient
    private Boolean aceitaPet;
}