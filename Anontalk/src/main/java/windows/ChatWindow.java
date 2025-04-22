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
    private final String currentUser;           // nombre de usuario local
    private final Mensaje mensaje;              // mensaje entrante
    private final PopUpMessages pum = new PopUpMessages();
    private final TCPController tcpController;
    private final Stage parentStage;

    // HTTP
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper;

    public ChatWindow(String currentUser, Mensaje mensaje, TCPController tcpController, int port, Stage parentStage) {
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

        Label lblHeader = new Label("De: " + mensaje.getSender() + "  |  Fecha: " + LocalDateTime.now());
        lblHeader.getStyleClass().add("chat-header");

        Label lblMensaje = new Label(mensaje.getContent());
        lblMensaje.setWrapText(true);
        lblMensaje.getStyleClass().add("chat-message-body");

        ScrollPane scrollMessage = new ScrollPane(lblMensaje);
        scrollMessage.setFitToWidth(true);
        scrollMessage.getStyleClass().add("chat-scrollpane");

        TextArea txtRespuesta = new TextArea();
        txtRespuesta.setPromptText("Redacta tu respuesta...");
        txtRespuesta.setPrefRowCount(4);
        txtRespuesta.getStyleClass().add("chat-textarea");

        HBox toolbar = new HBox(10);
        Button btnBold = new Button("B");
        btnBold.setOnAction(e -> txtRespuesta.appendText(" **Texto en negrita** "));
        toolbar.getChildren().add(btnBold);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("chat-toolbar");

        VBox composeArea = new VBox(5, toolbar, txtRespuesta);
        composeArea.setPadding(new Insets(10));
        composeArea.getStyleClass().add("chat-compose-area");

        Button btnEnviar = new Button("Enviar");
        btnEnviar.setOnAction(e -> {
            String respuesta = txtRespuesta.getText().trim();
            if (respuesta.isEmpty()) {
                pum.mostrarAlertaError("Error", "No puedes enviar un mensaje vacío.");
                return;
            }

            // 1) Enviar por TCP
            tcpController.sendMessage(mensaje.getSender(), DEFAULT_PORT, respuesta);
            // 2) Guardar en memoria
            MessageStore.sentMessages.add(new Mensaje(mensaje.getSender(), respuesta));
            // 3) Persistir vía API REST
            persistMensaje(currentUser, mensaje.getSender(), respuesta);

            pum.mostrarAlertaInformativa("Mensaje enviado", "Tu respuesta ha sido enviada.");
            chatStage.close();
            parentStage.show();
        });

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setOnAction(e -> {
            chatStage.close();
            parentStage.show();
        });

        HBox buttonBar = new HBox(10, btnEnviar, btnCerrar);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10));

        BorderPane chatLayout = new BorderPane();
        chatLayout.setTop(lblHeader);
        chatLayout.setCenter(scrollMessage);
        chatLayout.setBottom(new VBox(composeArea, buttonBar));
        chatLayout.setPadding(new Insets(10));
        chatLayout.getStyleClass().add("chat-root");

        Scene scene = new Scene(chatLayout, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        chatStage.setScene(scene);
        chatStage.show();
    }

    private void persistMensaje(String remitente, String destinatario, String mensaje) {
        try {
            MensajeDTO dto = new MensajeDTO();
            dto.setRemitente(remitente);
            dto.setDestinatario(destinatario);
            dto.setMensaje(mensaje);
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
                                    pum.mostrarAlertaError("Error", "No se pudo guardar el mensaje en base de datos.")
                            );
                        }
                    });
        } catch (Exception ex) {
            ex.printStackTrace();
            Platform.runLater(() ->
                    pum.mostrarAlertaError("Error", "Fallo al intentar guardar el mensaje.")
            );
        }
    }
}
