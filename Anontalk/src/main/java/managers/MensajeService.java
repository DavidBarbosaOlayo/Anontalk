package managers;

import org.springframework.stereotype.Service;
import security.AESUtils;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar mensajes: cifrar al guardar y descifrar al leer.
 */
@Service
public class MensajeService {

    private final MensajeRepository repo;
    private final SecretKey aesKey;

    public MensajeService(MensajeRepository repo) {
        this.repo = repo;
        // Carga la clave AES desde fichero o genera una si no existe
        this.aesKey = AESUtils.loadKey();
    }

    /**
     * Cifra el mensaje, lo persiste y devuelve un DTO con texto descifrado.
     */
    public MensajeDTO sendMessage(MensajeDTO dto) {
        try {
            // Cifrar
            String cifrado = AESUtils.encrypt(dto.getMensaje(), aesKey);
            // Persistir en entidad
            MensajeSB entidad = new MensajeSB(dto.getRemitente(), dto.getDestinatario(), cifrado);
            MensajeSB saved = repo.save(entidad);
            // Descifrar para respuesta
            String descifrado = AESUtils.decrypt(saved.getMensaje(), aesKey);
            return new MensajeDTO(saved.getId(), saved.getRemitente(), saved.getDestinatario(), descifrado, saved.getFechaHora());
        } catch (Exception e) {
            throw new RuntimeException("Error cifrando/guardando mensaje", e);
        }
    }

    /**
     * Recupera mensajes recibidos, descifra y mapea a DTO.
     */
    public List<MensajeDTO> getInbox(String destinatario) {
        return repo.findByDestinatario(destinatario).stream().map(m -> {
            try {
                String desc = AESUtils.decrypt(m.getMensaje(), aesKey);
                return new MensajeDTO(m.getId(), m.getRemitente(), m.getDestinatario(), desc, m.getFechaHora());
            } catch (Exception ex) {
                throw new RuntimeException("Error descifrando mensaje entrante", ex);
            }
        }).collect(Collectors.toList());
    }

    /**
     * Recupera mensajes enviados, descifra y mapea a DTO.
     */
    public List<MensajeDTO> getSent(String remitente) {
        return repo.findByRemitente(remitente).stream().map(m -> {
            try {
                String desc = AESUtils.decrypt(m.getMensaje(), aesKey);
                return new MensajeDTO(m.getId(), m.getRemitente(), m.getDestinatario(), desc, m.getFechaHora());
            } catch (Exception ex) {
                throw new RuntimeException("Error descifrando mensaje enviado", ex);
            }
        }).collect(Collectors.toList());
    }
}