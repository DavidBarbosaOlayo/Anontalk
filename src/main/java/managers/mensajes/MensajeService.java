package managers.mensajes;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio “ciego”: almacenamos blobs cifrados sin conocer su contenido.
 */
@Service
public class MensajeService {

    private final MensajeRepository repo;

    public MensajeService(MensajeRepository repo) {
        this.repo = repo;
    }

    public MensajeDTO sendMessage(MensajeDTO dto) {
        MensajeSB ent = new MensajeSB(dto.getRemitente(), dto.getDestinatario(), dto.getCipherTextBase64(), dto.getEncKeyBase64(), dto.getIvBase64());
        ent.setAsunto(dto.getAsunto()); // ← AÑADE ESTA LÍNEA

        MensajeSB saved = repo.save(ent);
        return new MensajeDTO(saved.getId(), saved.getRemitente(), saved.getDestinatario(), saved.getAsunto(),                // ← asunto aquí
                saved.getCipherTextBase64(), saved.getEncKeyBase64(), saved.getIvBase64(), saved.getFechaHora());
    }


    public List<MensajeDTO> getInbox(String destinatario) {
        return repo.findByDestinatario(destinatario).stream().map(m -> new MensajeDTO(m.getId(), m.getRemitente(), m.getDestinatario(), m.getAsunto(), m.getCipherTextBase64(), m.getEncKeyBase64(), m.getIvBase64(), m.getFechaHora())).collect(Collectors.toList());
    }

    public List<MensajeDTO> getSent(String remitente) {
        return repo.findByRemitente(remitente).stream().map(m -> new MensajeDTO(m.getId(), m.getRemitente(), m.getDestinatario(), m.getAsunto(),                // ← incluimos el asunto aquí
                m.getCipherTextBase64(), m.getEncKeyBase64(), m.getIvBase64(), m.getFechaHora())).collect(Collectors.toList());
    }


    public void deleteMessage(Long id) {
        repo.deleteById(id);
    }
}
