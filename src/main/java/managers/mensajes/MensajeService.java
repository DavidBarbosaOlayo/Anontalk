package managers.mensajes;

import managers.mensajes.adjuntos.AdjuntoDTO;
import managers.mensajes.adjuntos.AdjuntoSB;
import managers.mensajes.adjuntos.AdjuntoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio “ciego”: almacenamos blobs cifrados sin conocer su contenido.
 */
@Service
public class MensajeService {

    private final MensajeRepository repo;
    private final AdjuntoRepository adjRepo;

    public MensajeService(MensajeRepository repo, AdjuntoRepository adjRepo) {
        this.repo = repo;
        this.adjRepo = adjRepo;
    }

    public MensajeDTO sendMessage(MensajeDTO dto) {
        // 1) Persistir el mensaje
        MensajeSB ent = new MensajeSB(dto.getRemitente(), dto.getDestinatario(), dto.getCipherTextBase64(), dto.getEncKeyBase64(), dto.getIvBase64());
        ent.setAsunto(dto.getAsunto());
        MensajeSB savedMsg = repo.save(ent);

        // 2) Persistir adjuntos
        if (dto.getAdjuntos() != null) {
            for (AdjuntoDTO a : dto.getAdjuntos()) {
                AdjuntoSB sb = new AdjuntoSB();
                sb.setMensaje(savedMsg);
                sb.setFilename(a.getFilename());
                sb.setMimeType(a.getMimeType());
                sb.setCipherB64(a.getCipherTextBase64());
                sb.setEncKeyB64(a.getEncKeyBase64());
                sb.setIvB64(a.getIvBase64());
                adjRepo.save(sb);
            }
        }

        // 3) Construir DTO de respuesta
        MensajeDTO resp = new MensajeDTO(savedMsg.getId(), savedMsg.getRemitente(), savedMsg.getDestinatario(), savedMsg.getAsunto(), savedMsg.getCipherTextBase64(), savedMsg.getEncKeyBase64(), savedMsg.getIvBase64(), savedMsg.getFechaHora());
        // Incluir lista vacía o la lista original:
        resp.setAdjuntos(dto.getAdjuntos());

        return resp;
    }

    public List<MensajeDTO> getInbox(String destinatario) {
        return repo.findByDestinatario(destinatario).stream().map(m -> {
            MensajeDTO dto = new MensajeDTO(m.getId(), m.getRemitente(), m.getDestinatario(), m.getAsunto(), m.getCipherTextBase64(), m.getEncKeyBase64(), m.getIvBase64(), m.getFechaHora());
            // cargar adjuntos
            List<AdjuntoDTO> adj = adjRepo.findByMensajeId(m.getId()).stream().map(sb -> new AdjuntoDTO(sb.getFilename(), sb.getMimeType(), sb.getCipherB64(), sb.getEncKeyB64(), sb.getIvB64())).collect(Collectors.toList());
            dto.setAdjuntos(adj);
            return dto;
        }).collect(Collectors.toList());
    }

    public List<MensajeDTO> getSent(String remitente) {
        return repo.findByRemitente(remitente).stream().map(m -> {
            MensajeDTO dto = new MensajeDTO(m.getId(), m.getRemitente(), m.getDestinatario(), m.getAsunto(), m.getCipherTextBase64(), m.getEncKeyBase64(), m.getIvBase64(), m.getFechaHora());
            // cargar adjuntos
            List<AdjuntoDTO> adj = adjRepo.findByMensajeId(m.getId()).stream().map(sb -> new AdjuntoDTO(sb.getFilename(), sb.getMimeType(), sb.getCipherB64(), sb.getEncKeyB64(), sb.getIvB64())).collect(Collectors.toList());
            dto.setAdjuntos(adj);
            return dto;
        }).collect(Collectors.toList());
    }

    public void deleteMessage(Long id) {
        repo.deleteById(id);
        // opcional: borrar adjuntos
        adjRepo.deleteAll(adjRepo.findByMensajeId(id));
    }
}
