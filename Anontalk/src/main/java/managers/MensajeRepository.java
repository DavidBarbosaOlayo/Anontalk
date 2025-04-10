package managers;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MensajeRepository extends JpaRepository<MensajeSB, Long> {
    // Puedes añadir métodos personalizados si quieres buscar por remitente o destinatario
}

