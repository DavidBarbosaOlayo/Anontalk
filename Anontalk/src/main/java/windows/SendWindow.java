package windows;

import connections.TCPController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
    private final TCPController tcpController = TCPController.getInstance(1212);
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper;
    private Stage primaryStage;

    public SendWindow() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void show(String currentUser, Stage stage) throws Exception {
        this.primaryStage = stage;
        stage.setTitle("Mensajes Enviados - Anontalk");
        // Configura tu tabla aquí…

        Button btnNuevo = new Button("Nuevo Mensaje");
        btnNuevo.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog();
            dlg.setTitle("Enviar Mensaje");
            dlg.setHeaderText("Introduce IP:mensaje");
            dlg.setContentText("Formato:");
            dlg.showAndWait().ifPresent(input -> {
                try {
                    var p = input.split(":",2);
                    String host = p[0], msg = p[1];
                    tcpController.sendMessage(host, 1212, msg);
                    MessageStore.sentMessages.add(new Mensaje(host, msg));
                    // Persistir
                    MensajeDTO dto = new MensajeDTO();
                    dto.setRemitente(currentUser);
                    dto.setDestinatario(host);
                    dto.setMensaje(msg);
                    String j = mapper.writeValueAsString(dto);
                    HttpRequest rq = HttpRequest.newBuilder()
                            .uri(new URI("http://localhost:8080/api/messages/send"))
                            .header("Content-Type","application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(j))
                            .build();
                    httpClient.sendAsync(rq, HttpResponse.BodyHandlers.ofString());
                } catch (Exception ex) {
                    pum.mostrarAlertaError("Error","Formato incorrecto.");
                }
            });
        });

        // Resto de la UI…
        BorderPane root = new BorderPane();
        HBox top = new HBox(10, btnNuevo);
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(10));
        root.setTop(top);
        // … tabla de mensajes enviados …

        Scene sc = new Scene(root,800,600);
        stage.setScene(sc);
        stage.show();
    }
}
