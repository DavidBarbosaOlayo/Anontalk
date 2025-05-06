package windows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import managers.PopUpInfo;
import managers.mensajes.Mensaje;
import managers.mensajes.MensajeDTO;
import managers.mensajes.MessageStore;
import security.encryption.HybridCrypto;
import security.encryption.RSAUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.time.LocalDateTime;

public class ChatWindow {

    /* -------- estado -------- */
    private final String currentUser;
    private final Mensaje mensaje;          // mensaje “recibido” que se muestra arriba
    private final PopUpInfo pop = new PopUpInfo();

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public ChatWindow(String currentUser, Mensaje mensaje) {
        this.currentUser = currentUser;
        this.mensaje = mensaje;
    }

    public void show() {
        Stage chatStage = new Stage();
        chatStage.setTitle("Chat con " + mensaje.getSender());

        /* ---------- cabecera ---------- */
        Label lblHeader = new Label("De: " + mensaje.getSender() + "  |  Asunto: " + mensaje.getAsunto() + "  |  Fecha: " + LocalDateTime.now());

        lblHeader.getStyleClass().add("chat-header");

        /* ---------- cuerpo recibido ---------- */
        Label lblBody = new Label(mensaje.getContent());
        lblBody.setWrapText(true);
        lblBody.getStyleClass().add("chat-message-body");
        ScrollPane scroll = new ScrollPane(lblBody);
        scroll.setFitToWidth(true);

        /* ---------- redactar respuesta ---------- */
        TextArea txtReply = new TextArea();
        txtReply.setPromptText("Redacta tu respuesta…");
        txtReply.setPrefRowCount(4);
        txtReply.getStyleClass().add("chat-textarea");

        Button btnBold = new Button("B");
        btnBold.setOnAction(e -> txtReply.appendText(" **texto en negrita** "));
        HBox toolbar = new HBox(10, btnBold);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox compose = new VBox(5, toolbar, txtReply);
        compose.setPadding(new Insets(10));

        /* ---------- botones ---------- */
        Button btnEnviar = new Button("Enviar");
        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setOnAction(e -> chatStage.close());

        btnEnviar.setOnAction(e -> sendReply(txtReply.getText().trim(), chatStage));

        HBox bar = new HBox(10, btnEnviar, btnCerrar);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(10));

        /* ---------- layout raíz ---------- */
        BorderPane root = new BorderPane();
        root.setTop(lblHeader);
        root.setCenter(scroll);
        root.setBottom(new VBox(compose, bar));
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        chatStage.setScene(scene);
        chatStage.show();
    }

    /* =========================================================== */
    /* ===================  LÓGICA DE ENVÍO  ===================== */
    /* =========================================================== */

    private void sendReply(String plainText, Stage chatStage) {
        if (plainText.isEmpty()) {
            pop.mostrarAlertaError("Error", "No puedes enviar un mensaje vacío.");
            return;
        }
        String destinatario = mensaje.getSender();

        try {
            /* obtener clave pública del destinatario */
            HttpRequest pkReq = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/" + destinatario + "/publicKey")).GET().build();
            String pubB64 = http.send(pkReq, HttpResponse.BodyHandlers.ofString()).body();
            PublicKey destPk = RSAUtils.publicKeyFromBase64(pubB64);

            /* cifrar híbrido */
            HybridCrypto.HybridPayload p = HybridCrypto.encrypt(plainText, destPk);

            /* construir DTO */
            MensajeDTO dto = new MensajeDTO();
            dto.setRemitente(currentUser);
            dto.setDestinatario(destinatario);
            dto.setAsunto(mensaje.getAsunto());
            dto.setCipherTextBase64(p.cipherB64());
            dto.setEncKeyBase64(p.encKeyB64());
            dto.setIvBase64(p.ivB64());

            String json = mapper.writeValueAsString(dto);

            /* POST /messages/send */
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/messages/send")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();

            http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(resp -> {
                if (resp.statusCode() == 200) {
                    try {
                        MensajeDTO saved = mapper.readValue(resp.body(), MensajeDTO.class);
                        Platform.runLater(() -> {
                            MessageStore.sentMessages.add(new Mensaje(saved.getId(), saved.getDestinatario(), saved.getAsunto(), plainText));

                            pop.mostrarAlertaInformativa("Mensaje enviado", "Tu respuesta ha sido enviada.");
                            chatStage.close();
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> pop.mostrarAlertaError("Error", "No se pudo procesar la respuesta del servidor."));
                    }
                } else {
                    Platform.runLater(() -> pop.mostrarAlertaError("Error", "No se pudo guardar el mensaje en el servidor."));
                }
            });

        } catch (Exception ex) {
            pop.mostrarAlertaError("Error", "Falló el cifrado o la conexión.");
        }
    }
}
