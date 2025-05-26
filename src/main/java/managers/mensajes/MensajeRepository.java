package managers.mensajes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeRepository extends JpaRepository<MensajeSB, Long> {
    List<MensajeSB> findByDestinatario(String destinatario);
    List<MensajeSB> findByRemitente(String remitente);
}

