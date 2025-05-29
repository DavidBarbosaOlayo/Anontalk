// src/main/java/managers/mensajes/MensajeService.java
package managers.mensajes;

import managers.mensajes.adjuntos.AdjuntoDTO;
import managers.mensajes.adjuntos.AdjuntoSB;
import managers.mensajes.adjuntos.AdjuntoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MensajeService {

    private final MensajeRepository repo;
    private final AdjuntoRepository adjRepo;

    public MensajeService(MensajeRepository repo, AdjuntoRepository adjRepo) {
        this.repo = repo;
        this.adjRepo = adjRepo;
    }

    /**
     * Envío de mensaje (igual que antes)
     */
    public MensajeDTO sendMessage(MensajeDTO dto) {
        MensajeSB ent = new MensajeSB(dto.getRemitente(), dto.getDestinatario(), dto.getCipherTextBase64(), dto.getEncKeyBase64(), dto.getIvBase64());
        ent.setAsunto(dto.getAsunto());
        ent.setExpiryDate(dto.getExpiryDate()); // Añadir esta línea
        MensajeSB savedMsg = repo.save(ent);

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

        MensajeDTO resp = new MensajeDTO(savedMsg.getId(), savedMsg.getRemitente(), savedMsg.getDestinatario(), savedMsg.getAsunto(), savedMsg.getCipherTextBase64(), savedMsg.getEncKeyBase64(), savedMsg.getIvBase64(), savedMsg.getFechaHora(), savedMsg.getExpiryDate());
        resp.setAdjuntos(dto.getAdjuntos());
        resp.setRead(false);
        return resp;
    }

    /**
     * Borra un mensaje y sus adjuntos
     */
    public void deleteMessage(Long id) {
        repo.deleteById(id);
        adjRepo.deleteAll(adjRepo.findByMensajeId(id));
    }

    /**
     * LISTA LIGERA: devuelve sólo metadata sin adjuntos
     */
    public List<MensajeDTO> getInboxSummary(String destinatario) {
        return repo.findByDestinatario(destinatario).stream().map(m -> {
            MensajeDTO dto = new MensajeDTO(m.getId(), m.getRemitente(), m.getDestinatario(), m.getAsunto(), m.getCipherTextBase64(), m.getEncKeyBase64(), m.getIvBase64(), m.getFechaHora(), m.getExpiryDate());
            dto.setRead(m.isRead());
            return dto;
        }).collect(Collectors.toList());
    }


    /**
     * LISTA LIGERA: devuelve sólo metadata sin adjuntos
     */
    public List<MensajeDTO> getSentSummary(String remitente) {
        return repo.findByRemitente(remitente).stream().map(m -> new MensajeDTO(m.getId(), m.getRemitente(), m.getDestinatario(), m.getAsunto(), m.getCipherTextBase64(), m.getEncKeyBase64(), m.getIvBase64(), m.getFechaHora(), m.getExpiryDate())).collect(Collectors.toList());
    }

    /**
     * Carga únicamente los adjuntos de un mensaje dado
     */
    public List<AdjuntoDTO> getAttachments(Long mensajeId) {
        return adjRepo.findByMensajeId(mensajeId).stream().map(sb -> new AdjuntoDTO(sb.getFilename(), sb.getMimeType(), sb.getCipherB64(), sb.getEncKeyB64(), sb.getIvB64())).collect(Collectors.toList());
    }

    @Transactional
    public void markRead(Long id) {
        MensajeSB m = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!m.isRead()) {
            m.setRead(true);
            repo.save(m);
        }
    }


    @Scheduled(fixedRate = 30000) // Comprueba cada 30 segundos
    public void deleteExpiredMessages() {
        repo.deleteByExpiryDateBefore(LocalDateTime.now());
    }

}
