package managers.mensajes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MensajeRepository extends JpaRepository<MensajeSB, Long> {
    List<MensajeSB> findByDestinatario(String destinatario);
    List<MensajeSB> findByRemitente(String remitente);

    @Modifying
    @Query("DELETE FROM MensajeSB m WHERE m.expiryDate < :currentTime")
    void deleteByExpiryDateBefore(@Param("currentTime") LocalDateTime currentTime);
}

