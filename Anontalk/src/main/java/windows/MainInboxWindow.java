package windows;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import managers.mensajes.MessageStore;
import managers.mensajes.Mensaje;
import managers.PopUpInfo;
import managers.mensajes.MensajeDTO;

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
    private final PopUpInfo pum = new PopUpInfo();
    private Stage primaryStage;

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private Timeline refresher;
    private final Image trashIcon = new Image(getClass().getResourceAsStream("/papelera.png"), 16, 16, true, true);

    public MainInboxWindow(String currentUser) {
        this.currentUser = currentUser;
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Bandeja de Entrada - Anontalk");
        primaryStage.setOnCloseRequest(e -> {
            if (refresher != null) refresher.stop();
            Platform.exit();
            System.exit(0);
        });

        // Barra superior
        Label lblWelcome = new Label("Bienvenido, " + currentUser);
        lblWelcome.getStyleClass().add("label2");
        HBox leftBox = new HBox(lblWelcome);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        Button btnNuevo = new Button("Nuevo Mensaje");
        btnNuevo.setOnAction(e -> showSendDialog());

        Button btnCerrar = new Button("Cerrar Sesión");
        btnCerrar.setOnAction(e -> {
            if (refresher != null) refresher.stop();
            primaryStage.close();
            pum.mostrarAlertaInformativa("Cerrar Sesión", "Has cerrado sesión correctamente.");
            try {
                new LoginWindow().start(new Stage());
            } catch (Exception ex) {
            }
        });

        HBox rightBox = new HBox(10, btnNuevo, btnCerrar);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        BorderPane topBar = new BorderPane();
        topBar.setLeft(leftBox);
        topBar.setRight(rightBox);
        topBar.setPadding(new Insets(10));

        // Pestañas
        TabPane tabPane = new TabPane();
        Tab tabInbox = new Tab("Bandeja de Entrada", createTable(true));
        Tab tabSent = new Tab("Mensajes Enviados", createTable(false));
        tabInbox.setClosable(false);
        tabSent.setClosable(false);
        tabPane.getTabs().addAll(tabInbox, tabSent);

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabPane);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();

        // Carga inicial y polling
        loadMessagesFromApi();
        refresher = new Timeline(new KeyFrame(Duration.seconds(5), ev -> refreshInbox()));
        refresher.setCycleCount(Timeline.INDEFINITE);
        refresher.play();
    }

    private TableView<Mensaje> createTable(boolean inbox) {
        var list = inbox ? MessageStore.inboxMessages : MessageStore.sentMessages;

        TableView<Mensaje> table = new TableView<>(list);
        table.setEditable(false);

        // Columna Remitente / Destinatario
        TableColumn<Mensaje, String> colParty = new TableColumn<>(inbox ? "Remitente" : "Destinatario");
        colParty.setCellValueFactory(new PropertyValueFactory<>("sender"));
        colParty.setPrefWidth(150);
        colParty.setResizable(false);
        colParty.setEditable(false);

        // Columna Mensaje
        TableColumn<Mensaje, String> colContent = new TableColumn<>("Mensaje");
        colContent.setCellValueFactory(new PropertyValueFactory<>("content"));
        colContent.setPrefWidth(580);
        colContent.setResizable(false);
        colContent.setEditable(false);

        // Columna Papelera
        TableColumn<Mensaje, Void> colDelete = new TableColumn<>("");
        colDelete.setPrefWidth(40);
        colDelete.setResizable(false);
        colDelete.setEditable(false);
        colDelete.setCellFactory(tc -> new TableCell<>() {
            private final Button btn = new Button();

            {
                btn.setGraphic(new ImageView(trashIcon));
                btn.setStyle("-fx-background-color: transparent; -fx-padding: 2;");
                btn.setPrefSize(24, 24);
                btn.setOnAction(evt -> {
                    Mensaje msg = getTableRow().getItem();
                    if (msg != null) deleteMessage(msg);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(colParty, colContent, colDelete);
        table.setPlaceholder(new Label(inbox ? "No hay mensajes en la bandeja." : "No hay mensajes enviados."));

        // Doble clic abre chat
        table.setRowFactory(tv -> {
            TableRow<Mensaje> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (!row.isEmpty() && ev.getClickCount() == 2) {
                    new ChatWindow(currentUser, row.getItem()).show();
                }
            });
            return row;
        });

        return table;
    }

    private void loadMessagesFromApi() {
        refreshInbox();
        refreshSent();
    }

    private void refreshInbox() {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(new URI("http://localhost:8080/api/messages/inbox/" + currentUser)).GET().build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            List<MensajeDTO> inbox = mapper.readValue(res.body(), new TypeReference<>() {
            });
            Platform.runLater(() -> MessageStore.inboxMessages.setAll(inbox.stream().map(dto -> new Mensaje(dto.getId(), dto.getRemitente(), dto.getMensaje())).toList()));
        } catch (Exception e) { /* log o ignora */ }
    }

    private void refreshSent() {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(new URI("http://localhost:8080/api/messages/sent/" + currentUser)).GET().build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            List<MensajeDTO> sent = mapper.readValue(res.body(), new TypeReference<>() {
            });
            Platform.runLater(() -> MessageStore.sentMessages.setAll(sent.stream().map(dto -> new Mensaje(dto.getId(), dto.getDestinatario(), dto.getMensaje())).toList()));
        } catch (Exception e) { /* log o ignora */ }
    }

    private void deleteMessage(Mensaje msg) {
        MessageStore.inboxMessages.removeIf(m -> m.getId().equals(msg.getId()));
        MessageStore.sentMessages.removeIf(m -> m.getId().equals(msg.getId()));
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(new URI("http://localhost:8080/api/messages/" + msg.getId())).DELETE().build();
            httpClient.sendAsync(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) { /* log */ }
    }

    private void showSendDialog() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Enviar Mensaje");
        dlg.setHeaderText("Introduce destinatario y mensaje (usuario:mensaje)");
        dlg.setContentText("Formato: usuario:tu mensaje");
        dlg.showAndWait().ifPresent(input -> {
            try {
                String[] parts = input.split(":", 2);
                String dest = parts[0].trim();
                String txt = parts[1].trim();

                MensajeDTO dto = new MensajeDTO();
                dto.setRemitente(currentUser);
                dto.setDestinatario(dest);
                dto.setMensaje(txt);
                String json = mapper.writeValueAsString(dto);

                HttpRequest req = HttpRequest.newBuilder().uri(new URI("http://localhost:8080/api/messages/send")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();
                httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(r -> {
                    if (r.statusCode() == 200) {
                        Platform.runLater(this::refreshSent);
                    } else {
                        Platform.runLater(() -> pum.mostrarAlertaError("Error", "No se guardó el mensaje en BD."));
                    }
                });
            } catch (Exception ex) {
                pum.mostrarAlertaError("Error", "Formato incorrecto. Usa usuario:mensaje");
            }
        });
    }
}
