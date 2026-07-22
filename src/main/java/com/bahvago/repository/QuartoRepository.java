package com.bahvago.repository;

import com.bahvago.model.Quarto;
import com.bahvago.model.QuartoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuartoRepository extends JpaRepository<Quarto, QuartoId> {
    List<Quarto> findByCodigoHotel(Long codigoHotel);
    List<Quarto> findByCodigoHotelAndDisponivel(Long codigoHotel, Boolean disponivel);
    boolean existsByCodigoHotelAndAceitaPetTrue(Integer codigoHotel);
}