package windows;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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

public class SendWindow {
    private final PopUpMessages pum = new PopUpMessages();
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private Stage primaryStage;
    private String currentUser;

    public SendWindow() {
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void show(String currentUser, Stage stage) {
        this.primaryStage = stage;
        this.currentUser = currentUser;
        stage.setTitle("Enviar Mensaje - Anontalk");

        Button btnBack = new Button("← Volver");
        btnBack.setOnAction(e -> primaryStage.close());

        Button btnNuevo = new Button("Nuevo Mensaje");
        btnNuevo.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog();
            dlg.setTitle("Enviar Mensaje");
            dlg.setHeaderText("Introduce destinatario y mensaje (usuario:mensaje)");
            dlg.setContentText("Formato: usuario:tu mensaje");
            dlg.showAndWait().ifPresent(input -> {
                try {
                    String[] parts = input.split(":", 2);
                    String dest = parts[0].trim();
                    String txt  = parts[1].trim();

                    MensajeDTO dto = new MensajeDTO();
                    dto.setRemitente(currentUser);
                    dto.setDestinatario(dest);
                    dto.setMensaje(txt);
                    String json = mapper.writeValueAsString(dto);

                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(new URI("http://localhost:8080/api/messages/send"))
                            .header("Content-Type","application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json))
                            .build();
                    httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                            .thenAccept(r -> {
                                if (r.statusCode() == 200) {
                                    Platform.runLater(() ->
                                            MessageStore.sentMessages.add(new Mensaje(dest, txt))
                                    );
                                } else {
                                    Platform.runLater(() ->
                                            pum.mostrarAlertaError("Error", "No se guardó el mensaje en BD.")
                                    );
                                }
                            });
                } catch (Exception ex) {
                    pum.mostrarAlertaError("Error", "Formato incorrecto. Usa usuario:mensaje");
                }
            });
        });

        HBox top = new HBox(10, btnBack, btnNuevo);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(new Label("Los mensajes enviados se mostrarán en la pestaña correspondiente."));
        Scene sc = new Scene(root, 800, 600);
        stage.setScene(sc);
        stage.show();
    }
}
