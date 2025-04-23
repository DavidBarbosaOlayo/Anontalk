package managers.mensajes;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar mensajes: cifrar al guardar y descifrar al leer.
 */
@Service
public class MensajeService {
    private final MensajeRepository repo;

    public MensajeService(MensajeRepository repo) {
        this.repo = repo;
    }

    /**
     * Recibe el ciphertext (Base64 RSA) y lo guarda “tal cual”.
     */
    public MensajeDTO sendMessage(MensajeDTO dto) {
        MensajeSB entidad = new MensajeSB(dto.getRemitente(), dto.getDestinatario(), dto.getMensaje());  // aquí dto.getMensaje() ya es el RSA-ciphertext
        MensajeSB saved = repo.save(entidad);
        return new MensajeDTO(saved.getId(), saved.getRemitente(), saved.getDestinatario(), saved.getMensaje(),   // devolvemos el mismo ciphertext
                saved.getFechaHora());
    }

    /**
     * Devuelve la lista de mensajes entrantes, sin tocar el contenido.
     */
    public List<MensajeDTO> getInbox(String destinatario) {
        return repo.findByDestinatario(destinatario).stream().map(m -> new MensajeDTO(m.getId(), m.getRemitente(), m.getDestinatario(), m.getMensaje(),     // ciphertext
                m.getFechaHora())).collect(Collectors.toList());
    }

    /**
     * Igual para los enviados: devolvemos el contenido crudo.
     */
    public List<MensajeDTO> getSent(String remitente) {
        return repo.findByRemitente(remitente).stream().map(m -> new MensajeDTO(m.getId(), m.getRemitente(), m.getDestinatario(), m.getMensaje(),     // ciphertext
                m.getFechaHora())).collect(Collectors.toList());
    }

    public void deleteMessage(Long id) {
        repo.deleteById(id);
    }
}