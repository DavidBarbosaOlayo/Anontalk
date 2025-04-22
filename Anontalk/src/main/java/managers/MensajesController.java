package managers;

import managers.MensajeDTO;
import managers.MensajeService;
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

    @GetMapping("/inbox/{destinatario}")
    public ResponseEntity<List<MensajeDTO>> getInbox(@PathVariable String destinatario) {
        return ResponseEntity.ok(mensajeService.getInbox(destinatario));
    }

    @GetMapping("/sent/{remitente}")
    public ResponseEntity<List<MensajeDTO>> getSent(@PathVariable String remitente) {
        return ResponseEntity.ok(mensajeService.getSent(remitente));
    }
}
