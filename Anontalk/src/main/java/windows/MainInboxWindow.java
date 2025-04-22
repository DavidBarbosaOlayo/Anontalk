// MainInboxWindow.java
package windows;

import connections.TCPController;
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
    private static final int DEFAULT_PORT = 1212;
    private final TCPController tcpController = TCPController.getInstance(DEFAULT_PORT);

    // HTTP
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper;

    public MainInboxWindow(String currentUser) {
        this.currentUser = currentUser;
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;

        // 1) Limpiar & cargar de la API
        MessageStore.inboxMessages.clear();
        MessageStore.sentMessages.clear();
        loadMessagesFromApi();

        // 2) Iniciar servidor TCP y listener
        tcpController.startServer();
        tcpController.addMessageListener(this::onTcpMessage);

        // UI: creación de pestañas...
        Label lblWelcome = new Label("Bienvenido, " + currentUser);
        lblWelcome.getStyleClass().add("label2");
        HBox topLeft = new HBox(lblWelcome); topLeft.setAlignment(Pos.CENTER_LEFT);

        Button btnNuevo = new Button("Nuevo Mensaje");
        btnNuevo.setOnAction(e -> showSendDialog());

        Button btnCerrar = new Button("Cerrar Sesión");
        btnCerrar.setOnAction(e -> {
            tcpController.stopServer();
            primaryStage.close();
            pum.mostrarAlertaInformativa("Cerrar Sesión","Has cerrado sesión correctamente.");
            try { new LoginWindow().start(new Stage()); }
            catch (Exception ex){ throw new RuntimeException(ex); }
        });

        HBox topRight = new HBox(10, btnNuevo, btnCerrar); topRight.setAlignment(Pos.CENTER_RIGHT);

        BorderPane topBar = new BorderPane();
        topBar.setLeft(topLeft);
        topBar.setRight(topRight);
        topBar.setPadding(new Insets(10));

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                new Tab("Bandeja de Entrada", createInboxTable()),
                new Tab("Mensajes Enviados", createSentTable())
        );
        tabs.getTabs().forEach(t -> t.setClosable(false));

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabs);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private TableView<Mensaje> createInboxTable() {
        TableView<Mensaje> table = new TableView<>(MessageStore.inboxMessages);
        TableColumn<Mensaje,String> colSender = new TableColumn<>("Contacto");
        colSender.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getSender()));
        colSender.setPrefWidth(150);

        TableColumn<Mensaje,String> colMsg = new TableColumn<>("Mensaje");
        colMsg.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty("Pulsa para leer"));
        colMsg.setPrefWidth(600);
        colMsg.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String txt, boolean empty) {
                super.updateItem(txt, empty);
                if (empty||getTableRow().getItem()==null) { setText(null); return; }
                setText(txt);
                setOnMouseClicked(e -> {
                    Mensaje m = getTableRow().getItem();
                    new ChatWindow(currentUser, m, tcpController, DEFAULT_PORT, primaryStage).show();
                });
            }
        });

        table.getColumns().addAll(colSender, colMsg);
        table.setPlaceholder(new Label("No hay mensajes en la bandeja."));
        return table;
    }

    private TableView<Mensaje> createSentTable() {
        TableView<Mensaje> table = new TableView<>(MessageStore.sentMessages);
        TableColumn<Mensaje,String> colDest = new TableColumn<>("Destinatario");
        colDest.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getSender()));
        colDest.setPrefWidth(150);

        TableColumn<Mensaje,String> colMsg = new TableColumn<>("Mensaje");
        colMsg.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty("Pulsa para ver"));
        colMsg.setPrefWidth(600);
        colMsg.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String txt, boolean empty) {
                super.updateItem(txt, empty);
                if (empty||getTableRow().getItem()==null) { setText(null); return; }
                setText(txt);
                setOnMouseClicked(e -> {
                    Mensaje m = getTableRow().getItem();
                    new ChatWindow(currentUser, m, tcpController, DEFAULT_PORT, primaryStage).show();
                });
            }
        });

        table.getColumns().addAll(colDest, colMsg);
        table.setPlaceholder(new Label("No hay mensajes enviados."));
        return table;
    }

    private void loadMessagesFromApi() {
        try {
            // INBOX
            HttpRequest r1 = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/messages/inbox/" + currentUser))
                    .GET().build();
            var resp1 = httpClient.send(r1, HttpResponse.BodyHandlers.ofString());
            List<MensajeDTO> in = mapper.readValue(resp1.body(), new TypeReference<>(){});
            in.forEach(d -> MessageStore.inboxMessages.add(new Mensaje(d.getRemitente(), d.getMensaje())));

            // SENT
            HttpRequest r2 = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/messages/sent/" + currentUser))
                    .GET().build();
            var resp2 = httpClient.send(r2, HttpResponse.BodyHandlers.ofString());
            List<MensajeDTO> out = mapper.readValue(resp2.body(), new TypeReference<>(){});
            out.forEach(d -> MessageStore.sentMessages.add(new Mensaje(d.getDestinatario(), d.getMensaje())));

        } catch (Exception ex) {
            ex.printStackTrace();
            pum.mostrarAlertaError("Error de red","No se pudieron cargar los mensajes.");
        }
    }

    private void onTcpMessage(String sender, String message) {
        // 1) Persistir como mensaje entrante
        persistMensaje(sender, currentUser, message);
        // 2) Mostrar en UI
        Platform.runLater(() -> MessageStore.inboxMessages.add(new Mensaje(sender, message)));
    }

    private void showSendDialog() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Enviar Mensaje");
        dlg.setHeaderText("Introduce IP:mensaje");
        dlg.setContentText("Formato:");
        dlg.showAndWait().ifPresent(input -> {
            try {
                String[] p = input.split(":",2);
                String host = p[0], msg = p[1];
                tcpController.sendMessage(host, DEFAULT_PORT, msg);
                MessageStore.sentMessages.add(new Mensaje(host, msg));
                persistMensaje(currentUser, host, msg);
            } catch (Exception ex) {
                pum.mostrarAlertaError("Error","Formato incorrecto.");
            }
        });
    }

    private void persistMensaje(String remitente, String destinatario, String mensaje) {
        try {
            MensajeDTO dto = new MensajeDTO();
            dto.setRemitente(remitente);
            dto.setDestinatario(destinatario);
            dto.setMensaje(mensaje);
            String j = mapper.writeValueAsString(dto);
            HttpRequest rq = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/messages/send"))
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(j))
                    .build();
            httpClient.sendAsync(rq, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(r -> {
                        if (r.statusCode()!=200) pum.mostrarAlertaError("Error","No guardado en BD.");
                    });
        } catch (Exception e) {
            e.printStackTrace();
            pum.mostrarAlertaError("Error","Fallo guardado.");
        }
    }
}
