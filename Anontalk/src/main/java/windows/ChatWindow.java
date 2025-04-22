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
import managers.mensajes.MessageStore;
import managers.mensajes.Mensaje;
import managers.PopUpInfo;
import managers.mensajes.MensajeDTO;

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
    private final PopUpInfo pum = new PopUpInfo();

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public ChatWindow(String currentUser, Mensaje mensaje) {
        this.currentUser = currentUser;
        this.mensaje = mensaje;
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void show() {
        Stage chatStage = new Stage();
        chatStage.setTitle("Chat con " + mensaje.getSender());

        Label lblHeader = new Label(
                "De: " + mensaje.getSender() + "  |  Fecha: " + LocalDateTime.now()
        );
        lblHeader.getStyleClass().add("chat-header");

        Label lblBody = new Label(mensaje.getContent());
        lblBody.setWrapText(true);
        lblBody.getStyleClass().add("chat-message-body");
        ScrollPane scroll = new ScrollPane(lblBody);
        scroll.setFitToWidth(true);

        TextArea txtReply = new TextArea();
        txtReply.setPromptText("Redacta tu respuesta...");
        txtReply.setPrefRowCount(4);
        txtReply.getStyleClass().add("chat-textarea");

        Button btnBold = new Button("B");
        btnBold.setOnAction(e -> txtReply.appendText(" **Texto en negrita** "));
        HBox toolbar = new HBox(10, btnBold);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("chat-toolbar");

        VBox composeArea = new VBox(5, toolbar, txtReply);
        composeArea.setPadding(new Insets(10));
        composeArea.getStyleClass().add("chat-compose-area");

        Button btnEnviar = new Button("Enviar");
        btnEnviar.setOnAction(e -> {
            String respuesta = txtReply.getText().trim();
            if (respuesta.isEmpty()) {
                pum.mostrarAlertaError("Error", "No puedes enviar un mensaje vacío.");
                return;
            }
            // Persistir vía API REST
            MensajeDTO dto = new MensajeDTO();
            dto.setRemitente(currentUser);
            dto.setDestinatario(mensaje.getSender());
            dto.setMensaje(respuesta);
            try {
                String json = mapper.writeValueAsString(dto);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:8080/api/messages/send"))
                        .header("Content-Type","application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString());
            } catch (Exception ex) {
                Platform.runLater(() -> pum.mostrarAlertaError(
                        "Error", "No se pudo guardar el mensaje en la base de datos."
                ));
            }
            // Actualizar UI local
            MessageStore.sentMessages.add(new Mensaje(mensaje.getSender(), respuesta));
            pum.mostrarAlertaInformativa("Mensaje enviado", "Tu respuesta ha sido enviada.");
            chatStage.close();
        });

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setOnAction(e -> chatStage.close());

        HBox buttonBar = new HBox(10, btnEnviar, btnCerrar);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10));

        BorderPane layout = new BorderPane();
        layout.setTop(lblHeader);
        layout.setCenter(scroll);
        layout.setBottom(new VBox(composeArea, buttonBar));
        layout.setPadding(new Insets(10));
        layout.getStyleClass().add("chat-root");

        Scene scene = new Scene(layout, 800, 600);
        scene.getStylesheets().add(
                getClass().getResource("/temas.css").toExternalForm()
        );
        chatStage.setScene(scene);
        chatStage.show();
    }
}
