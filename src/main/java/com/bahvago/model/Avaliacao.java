package com.bahvago.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "Avaliacao")
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CodigoAvaliacao")
    private Integer id;

    @Column(name = "Nota", nullable = false)
    private Float nota; // 0 a 5

    @Column(name = "Comentario", length = 200)
    private String comentario;

    @Column(name = "Data", nullable = false)
    private LocalDate data;

    @Column(name = "Resposta", length = 200)
    private String resposta;

    @Column(name = "CodigoHotel", nullable = false)
    private Integer codigoHotel;

    @Column(name = "CPF", nullable = false, length = 11)
    private String cpf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CPF", referencedColumnName = "CPF", insertable = false, updatable = false)
    private Usuario usuario;

    @PrePersist
    protected void onCreate() {
        if (this.data == null) {
            this.data = LocalDate.now();
        }
    }
}