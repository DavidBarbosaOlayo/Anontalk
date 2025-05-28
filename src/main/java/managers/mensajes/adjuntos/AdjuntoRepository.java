package managers.mensajes.adjuntos;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdjuntoRepository extends JpaRepository<AdjuntoSB, Long> {
    List<AdjuntoSB> findByMensajeId(Long mensajeId);
}
