// ChatWindow.java
package windows;

import connections.TCPController;
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
    private static final int DEFAULT_PORT = 1212;
    private final String currentUser;
    private final Mensaje mensaje;
    private final TCPController tcpController;
    private final Stage parentStage;
    private final PopUpMessages pum = new PopUpMessages();

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper;

    public ChatWindow(String currentUser,
                      Mensaje mensaje,
                      TCPController tcpController,
                      Stage parentStage) {
        this.currentUser = currentUser;
        this.mensaje = mensaje;
        this.tcpController = tcpController;
        this.parentStage = parentStage;

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
        sp.getStyleClass().add("chat-scrollpane");

        TextArea reply = new TextArea();
        reply.setPromptText("Redacta tu respuesta...");
        reply.setPrefRowCount(4);
        reply.getStyleClass().add("chat-textarea");

        Button btnBold = new Button("B");
        btnBold.setOnAction(e -> reply.appendText(" **Texto en negrita** "));
        HBox toolbar = new HBox(10, btnBold);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("chat-toolbar");

        VBox compose = new VBox(5, toolbar, reply);
        compose.setPadding(new Insets(10));
        compose.getStyleClass().add("chat-compose-area");

        Button btnEnviar = new Button("Enviar");
        btnEnviar.setOnAction(e -> {
            String resp = reply.getText().trim();
            if (resp.isEmpty()) {
                pum.mostrarAlertaError("Error", "No puedes enviar un mensaje vacío.");
                return;
            }
            // 1) Persistir siempre en BD
            persistMensaje(currentUser, mensaje.getSender(), resp);
            // 2) Enviar TCP si está online
            tcpController.sendMessage(mensaje.getSender(), DEFAULT_PORT, resp);
            // 3) Añadir a UI local
            MessageStore.sentMessages.add(new Mensaje(mensaje.getSender(), resp));

            pum.mostrarAlertaInformativa("Mensaje enviado", "Tu respuesta ha sido enviada.");
            chatStage.close();
            parentStage.show();
        });

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setOnAction(e -> {
            chatStage.close();
            parentStage.show();
        });

        HBox btnBar = new HBox(10, btnEnviar, btnCerrar);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10));

        BorderPane layout = new BorderPane();
        layout.setTop(hdr);
        layout.setCenter(sp);
        layout.setBottom(new VBox(compose, btnBar));
        layout.setPadding(new Insets(10));
        layout.getStyleClass().add("chat-root");

        Scene sc = new Scene(layout, 800, 600);
        sc.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        chatStage.setScene(sc);
        chatStage.show();
    }

    private void persistMensaje(String remitente, String destinatario, String texto) {
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
                    .thenAccept(resp -> {
                        if (resp.statusCode() != 200) {
                            Platform.runLater(() ->
                                    pum.mostrarAlertaError("Error","No se guardó el mensaje en base de datos."));
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() ->
                    pum.mostrarAlertaError("Error","Fallo al guardar el mensaje."));
        }
    }
}
