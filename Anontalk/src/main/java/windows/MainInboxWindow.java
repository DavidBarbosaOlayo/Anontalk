// MainInboxWindow.java
package windows;

import javafx.application.Application;
import javafx.application.Platform;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class MainInboxWindow extends Application {
    private final String currentUser;
    private final PopUpMessages pum = new PopUpMessages();
    private Stage primaryStage;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper;

    public MainInboxWindow(String currentUser) {
        this.currentUser = currentUser;
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // 1) Limpiar y cargar de la API al arrancar
        MessageStore.inboxMessages.clear();
        MessageStore.sentMessages.clear();
        loadFromApi();

        // UI superior
        Label lblW = new Label("Bienvenido, " + currentUser);
        lblW.getStyleClass().add("label2");
        HBox left = new HBox(lblW); left.setAlignment(Pos.CENTER_LEFT);

        Button btnNew = new Button("Nuevo Mensaje");
        btnNew.setOnAction(e -> showSendDialog());

        Button btnLogout = new Button("Cerrar Sesión");
        btnLogout.setOnAction(e -> {
            primaryStage.close();
            pum.mostrarAlertaInformativa("Logout", "Sesión cerrada.");
            try { new LoginWindow().start(new Stage()); }
            catch (Exception ex){ throw new RuntimeException(ex); }
        });

        HBox right = new HBox(10, btnNew, btnLogout);
        right.setAlignment(Pos.CENTER_RIGHT);

        BorderPane top = new BorderPane();
        top.setLeft(left);
        top.setRight(right);
        top.setPadding(new Insets(10));

        // Pestañas
        TabPane tabs = new TabPane(
                new Tab("Bandeja de Entrada", createTable(MessageStore.inboxMessages, true)),
                new Tab("Mensajes Enviados", createTable(MessageStore.sentMessages, false))
        );
        tabs.getTabs().forEach(t->t.setClosable(false));

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(tabs);
        root.setPadding(new Insets(10));

        Scene sc = new Scene(root, 800, 600);
        sc.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        primaryStage.setScene(sc);
        primaryStage.show();
    }

    private <T extends Mensaje> TableView<Mensaje> createTable(javafx.collections.ObservableList<Mensaje> data, boolean inbox) {
        TableView<Mensaje> table = new TableView<>(data);
        TableColumn<Mensaje,String> col1 = new TableColumn<>(inbox?"Remitente":"Destinatario");
        col1.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getSender()));
        col1.setPrefWidth(150);

        TableColumn<Mensaje,String> col2 = new TableColumn<>("Mensaje");
        col2.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getContent()));
        col2.setPrefWidth(600);

        table.getColumns().addAll(col1, col2);
        table.setPlaceholder(new Label("No hay mensajes."));
        return table;
    }

    private void loadFromApi() {
        try {
            // Bandeja
            HttpRequest r1 = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/messages/inbox/" + currentUser))
                    .GET().build();
            HttpResponse<String> resp1 = httpClient.send(r1, HttpResponse.BodyHandlers.ofString());
            List<MensajeDTO> in = mapper.readValue(resp1.body(), new TypeReference<>(){});
            in.forEach(d -> MessageStore.inboxMessages.add(new Mensaje(d.getRemitente(), d.getMensaje())));

            // Enviados
            HttpRequest r2 = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/messages/sent/" + currentUser))
                    .GET().build();
            HttpResponse<String> resp2 = httpClient.send(r2, HttpResponse.BodyHandlers.ofString());
            List<MensajeDTO> out = mapper.readValue(resp2.body(), new TypeReference<>(){});
            out.forEach(d -> MessageStore.sentMessages.add(new Mensaje(d.getDestinatario(), d.getMensaje())));

        } catch (Exception e) {
            e.printStackTrace();
            pum.mostrarAlertaError("Error de red", "No se pudieron cargar los mensajes.");
        }
    }

    private void showSendDialog() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Enviar Mensaje");
        dlg.setHeaderText("Formato → destinatario:mensaje");
        dlg.setContentText("destinatario:texto");
        dlg.showAndWait().ifPresent(input -> {
            try {
                String[] p = input.split(":", 2);
                String dest = p[0].trim(), txt = p[1].trim();
                // Persistir
                MensajeDTO dto = new MensajeDTO();
                dto.setRemitente(currentUser);
                dto.setDestinatario(dest);
                dto.setMensaje(txt);
                String json = mapper.writeValueAsString(dto);

                HttpRequest rq = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:8080/api/messages/send"))
                        .header("Content-Type","application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                httpClient.sendAsync(rq, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(r -> {
                            if (r.statusCode() != 200)
                                Platform.runLater(() -> pum.mostrarAlertaError("Error","No guardado en BD."));
                            else
                                Platform.runLater(() -> MessageStore.sentMessages.add(new Mensaje(dest, txt)));
                        });

            } catch (Exception ex) {
                pum.mostrarAlertaError("Error", "Formato incorrecto. Usa destinatario:mensaje");
            }
        });
    }
}
