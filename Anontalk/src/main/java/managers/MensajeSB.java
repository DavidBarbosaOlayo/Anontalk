package managers;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes")
public class MensajeSB {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String remitente;
    private String destinatario;
    private String mensaje;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;

    public MensajeSB() {
        this.fechaHora = LocalDateTime.now();
    }

    public MensajeSB(String remitente, String destinatario, String mensaje) {
        this.remitente = remitente;
        this.destinatario = destinatario;
        this.mensaje = mensaje;
        this.fechaHora = LocalDateTime.now();
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public String getRemitente() {
        return remitente;
    }

    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }
}

