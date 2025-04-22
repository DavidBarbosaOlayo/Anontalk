// ChatWindow.java
package windows;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import managers.MessageStore;
import managers.Mensaje;
import managers.PopUpMessages;
import managers.MensajeDTO;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

public class ChatWindow {
    private final String currentUser;
    private final Mensaje mensaje;
    private final PopUpMessages pum = new PopUpMessages();

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper;

    public ChatWindow(String currentUser, Mensaje mensaje) {
        this.currentUser = currentUser;
        this.mensaje = mensaje;
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void show() {
        Stage chatStage = new Stage();
        chatStage.setTitle("Chat con " + mensaje.getSender());

        Label hdr = new Label("De: " + mensaje.getSender() + "  |  Fecha: " + LocalDateTime.now());
        hdr.getStyleClass().add("chat-header");

        Label body = new Label(mensaje.getContent());
        body.setWrapText(true);
        body.getStyleClass().add("chat-message-body");
        ScrollPane sp = new ScrollPane(body);
        sp.setFitToWidth(true);

        TextArea reply = new TextArea();
        reply.setPromptText("Redacta tu respuesta...");
        reply.setPrefRowCount(4);
        reply.getStyleClass().add("chat-textarea");

        HBox toolbar = new HBox(10, new Button("B") {{
            setOnAction(e -> reply.appendText(" **Texto en negrita** "));
        }});
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("chat-toolbar");

        VBox compose = new VBox(5, toolbar, reply);
        compose.setPadding(new Insets(10));

        Button btnEnviar = new Button("Enviar");
        btnEnviar.setOnAction(e -> {
            String resp = reply.getText().trim();
            if (resp.isEmpty()) {
                pum.mostrarAlertaError("Error", "No puedes enviar un mensaje vacío.");
                return;
            }
            // 1) Persistir en BD vía API
            sendViaApi(currentUser, mensaje.getSender(), resp);
            // 2) Poner en UI local
            MessageStore.sentMessages.add(new Mensaje(mensaje.getSender(), resp));

            pum.mostrarAlertaInformativa("Mensaje enviado", "Se ha guardado en la base de datos.");
            chatStage.close();
        });

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setOnAction(e -> chatStage.close());

        HBox btnBar = new HBox(10, btnEnviar, btnCerrar);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10));

        BorderPane layout = new BorderPane();
        layout.setTop(hdr);
        layout.setCenter(sp);
        layout.setBottom(new VBox(compose, btnBar));
        layout.setPadding(new Insets(10));

        Scene sc = new Scene(layout, 800, 600);
        sc.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        chatStage.setScene(sc);
        chatStage.show();
    }

    private void sendViaApi(String remitente, String destinatario, String texto) {
        try {
            MensajeDTO dto = new MensajeDTO();
            dto.setRemitente(remitente);
            dto.setDestinatario(destinatario);
            dto.setMensaje(texto);
            String json = mapper.writeValueAsString(dto);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/messages/send"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(r -> {
                        if (r.statusCode() != 200) {
                            Platform.runLater(() ->
                                    pum.mostrarAlertaError("Error", "No se pudo guardar en la BD.")
                            );
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            pum.mostrarAlertaError("Error", "Fallo al guardar el mensaje.");
        }
    }
}
