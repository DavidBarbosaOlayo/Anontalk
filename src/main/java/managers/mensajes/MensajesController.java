// src/main/java/managers/mensajes/MensajesController.java
package managers.mensajes;

import managers.mensajes.adjuntos.AdjuntoDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MensajesController {

    private final MensajeService mensajeService;

    public MensajesController(MensajeService mensajeService) {
        this.mensajeService = mensajeService;
    }

    @PostMapping("/send")
    public ResponseEntity<MensajeDTO> sendMessage(@RequestBody MensajeDTO mensajeDTO) {
        return ResponseEntity.ok(mensajeService.sendMessage(mensajeDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        mensajeService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Nuevo endpoint: lista ligera de inbox sin adjuntos
     */
    @GetMapping("/inbox/{destinatario}")
    public ResponseEntity<List<MensajeDTO>> getInbox(@PathVariable String destinatario) {
        return ResponseEntity.ok(mensajeService.getInboxSummary(destinatario));
    }

    /**
     * Nuevo endpoint: lista ligera de sent sin adjuntos
     */
    @GetMapping("/sent/{remitente}")
    public ResponseEntity<List<MensajeDTO>> getSent(@PathVariable String remitente) {
        return ResponseEntity.ok(mensajeService.getSentSummary(remitente));
    }

    /**
     * Nuevo endpoint: carga solo adjuntos de un mensaje
     */
    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<AdjuntoDTO>> getAttachments(@PathVariable("id") Long id) {
        return ResponseEntity.ok(mensajeService.getAttachments(id));
    }
}
