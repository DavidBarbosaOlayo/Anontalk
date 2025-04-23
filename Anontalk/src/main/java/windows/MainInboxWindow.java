// src/main/java/windows/MainInboxWindow.java
package windows;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import managers.PopUpInfo;
import managers.mensajes.Mensaje;
import managers.mensajes.MensajeDTO;
import managers.mensajes.MessageStore;
import security.HybridCrypto;
import security.KeyManager;
import security.RSAUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.List;

public class MainInboxWindow extends Application {

    /* -------- estado -------- */
    private final String currentUser;
    private final PopUpInfo pop = new PopUpInfo();
    private Stage stage;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private Timeline refresher;
    private final Image trashIcon = new Image(getClass().getResourceAsStream("/papelera.png"), 16, 16, true, true);

    public MainInboxWindow(String currentUser) { this.currentUser = currentUser; }

    /* =========================================================== */

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Bandeja de Entrada - Anontalk");
        stage.setOnCloseRequest(e -> {
            if (refresher != null) refresher.stop();
            Platform.exit();
            System.exit(0);
        });

        /* ---------- barra superior ---------- */
        Label lblWelcome = new Label("Bienvenido, " + currentUser);
        lblWelcome.getStyleClass().add("label2");

        Button btnNuevo = new Button("Nuevo Mensaje");
        btnNuevo.setOnAction(e -> showSendDialog());

        Button btnCerrar = new Button("Cerrar Sesión");
        btnCerrar.setOnAction(e -> {
            if (refresher != null) refresher.stop();
            stage.close();
            pop.mostrarAlertaInformativa("Cerrar Sesión", "Has cerrado sesión correctamente.");
            try { new LoginWindow().start(new Stage()); } catch (Exception ignored) { }
        });

        BorderPane topBar = new BorderPane(
                new HBox(lblWelcome),
                null,
                new HBox(10, btnNuevo, btnCerrar),
                null, null
        );
        BorderPane.setAlignment(lblWelcome, Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10));

        /* ---------- tablas ---------- */
        TabPane tabs = new TabPane(
                new Tab("Bandeja de Entrada", createTable(true)),
                new Tab("Mensajes Enviados", createTable(false))
        );
        tabs.getTabs().forEach(t -> t.setClosable(false));

        /* ---------- escena ---------- */
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabs);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        stage.setScene(scene);
        stage.show();

        /* ---------- carga + polling ---------- */
        loadMessages();
        refresher = new Timeline(new KeyFrame(Duration.seconds(5), ev -> refreshInbox()));
        refresher.setCycleCount(Timeline.INDEFINITE);
        refresher.play();
    }

    /* =========================================================== */
    /*  tablas y botones  */
    /* =========================================================== */

    private TableView<Mensaje> createTable(boolean inbox) {
        var list = inbox ? MessageStore.inboxMessages : MessageStore.sentMessages;
        TableView<Mensaje> table = new TableView<>(list);

        TableColumn<Mensaje, String> colParty = new TableColumn<>(inbox ? "Remitente" : "Destinatario");
        colParty.setCellValueFactory(new PropertyValueFactory<>("sender"));
        colParty.setPrefWidth(150);

        TableColumn<Mensaje, String> colMsg = new TableColumn<>("Mensaje");
        colMsg.setCellValueFactory(new PropertyValueFactory<>("content"));
        colMsg.setPrefWidth(580);

        TableColumn<Mensaje, Void> colDel = new TableColumn<>("");
        colDel.setPrefWidth(40);
        colDel.setCellFactory(tc -> new TableCell<>() {
            private final Button btn = new Button();

            {
                btn.setGraphic(new ImageView(trashIcon));
                btn.setStyle("-fx-background-color: transparent;");
                btn.setOnAction(e -> {
                    Mensaje m = getTableRow().getItem();
                    if (m != null) deleteMessage(m);
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(colParty, colMsg, colDel);
        table.setPlaceholder(new Label(inbox ? "No hay mensajes." : "Nada enviado."));
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

    /* =========================================================== */
    /*  Carga de mensajes  */
    /* =========================================================== */

    private void loadMessages() { refreshInbox(); refreshSent(); }

    private void refreshInbox() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/messages/inbox/" + currentUser))
                    .GET().build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            List<MensajeDTO> inbox = mapper.readValue(res.body(), new TypeReference<>() {});
            Platform.runLater(() -> MessageStore.inboxMessages.setAll(
                    inbox.stream().map(this::dtoToMensajeInbox).toList()));
        } catch (Exception ignored) { }
    }

    private void refreshSent() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/messages/sent/" + currentUser))
                    .GET().build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            List<MensajeDTO> sent = mapper.readValue(res.body(), new TypeReference<>() {});
            Platform.runLater(() -> MessageStore.sentMessages.setAll(
                    sent.stream().map(this::dtoToMensajeSent).toList()));
        } catch (Exception ignored) { }
    }

    /* ---------- descifrado helpers ---------- */

    private Mensaje dtoToMensajeInbox(MensajeDTO dto) {
        String plain;
        try {
            plain = HybridCrypto.decrypt(new HybridCrypto.HybridPayload(
                            dto.getCipherTextBase64(), dto.getEncKeyBase64(), dto.getIvBase64()),
                    KeyManager.getPrivateKey());
        } catch (Exception ex) { plain = "[Error al descifrar]"; }
        return new Mensaje(dto.getId(), dto.getRemitente(), plain);
    }

    private Mensaje dtoToMensajeSent(MensajeDTO dto) {
        String plain;
        try {
            plain = HybridCrypto.decrypt(new HybridCrypto.HybridPayload(
                            dto.getCipherTextBase64(), dto.getEncKeyBase64(), dto.getIvBase64()),
                    KeyManager.getPrivateKey());
        } catch (Exception ex) { plain = "[Error al descifrar]"; }
        return new Mensaje(dto.getId(), dto.getDestinatario(), plain);
    }

    /* =========================================================== */
    /*  enviar “nuevo mensaje rápido”  */
    /* =========================================================== */

    private void showSendDialog() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Enviar Mensaje");
        dlg.setHeaderText("usuario:mensaje");
        dlg.setContentText("Formato:");

        dlg.showAndWait().ifPresent(input -> {
            try {
                String[] parts = input.split(":", 2);
                String dest = parts[0].trim();
                String txt = parts[1].trim();
                if (dest.isBlank() || txt.isBlank()) throw new IllegalArgumentException();

                /* 1 · clave pública del destinatario */
                HttpRequest pkReq = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/users/" + dest + "/publicKey"))
                        .GET().build();
                String pubB64 = http.send(pkReq, HttpResponse.BodyHandlers.ofString()).body();
                PublicKey destPk = RSAUtils.publicKeyFromBase64(pubB64);

                /* 2 · cifrado híbrido */
                HybridCrypto.HybridPayload p = HybridCrypto.encrypt(txt, destPk);

                /* 3 · DTO */
                MensajeDTO dto = new MensajeDTO();
                dto.setRemitente(currentUser);
                dto.setDestinatario(dest);
                dto.setCipherTextBase64(p.cipherB64());
                dto.setEncKeyBase64(p.encKeyB64());
                dto.setIvBase64(p.ivB64());

                String json = mapper.writeValueAsString(dto);

                /* 4 · POST */
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/messages/send"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(r -> {
                            if (r.statusCode() == 200) Platform.runLater(this::refreshSent);
                            else Platform.runLater(() ->
                                    pop.mostrarAlertaError("Error", "No se guardó el mensaje."));
                        });

            } catch (Exception ex) {
                pop.mostrarAlertaError("Error", "Formato incorrecto o fallo de cifrado.");
            }
        });
    }

    /* =========================================================== */
    /*  eliminar mensaje  */
    /* =========================================================== */

    private void deleteMessage(Mensaje msg) {
        MessageStore.inboxMessages.removeIf(m -> m.getId().equals(msg.getId()));
        MessageStore.sentMessages.removeIf(m -> m.getId().equals(msg.getId()));
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/messages/" + msg.getId()))
                    .DELETE().build();
            http.sendAsync(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) { }
    }
}
