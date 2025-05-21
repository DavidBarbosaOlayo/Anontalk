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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import managers.PopUpInfo;
import managers.mensajes.Mensaje;
import managers.mensajes.MensajeDTO;
import managers.mensajes.MessageStore;
import security.encryption.HybridCrypto;
import security.encryption.KeyManager;
import security.encryption.RSAUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainInboxWindow extends Application {

    /* -------- estado -------- */
    private final String currentUser;
    private final PopUpInfo pop = new PopUpInfo();
    private Stage stage;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private Timeline refresher;
    // Iconos claro/oscuro
    private final Image userIconLight = new Image(getClass().getResourceAsStream("/user.png"), 26, 26, true, true);
    private final Image userIconDark = new Image(getClass().getResourceAsStream("/user2.png"), 26, 26, true, true);
    private final Image trashIconLight = new Image(getClass().getResourceAsStream("/papelera.png"), 16, 16, true, true);
    private final Image trashIconDark = new Image(getClass().getResourceAsStream("/papelera2.png"), 16, 16, true, true);
    private final Image settingsIconLight = new Image(getClass().getResourceAsStream("/ajustes.png"), 26, 26, true, true);
    private final Image settingsIconDark = new Image(getClass().getResourceAsStream("/ajustes2.png"), 26, 26, true, true);

    // Views y estado de tema
    private ImageView profileIconView;
    private ImageView settingsIconView;
    private final List<Button> trashButtons = new ArrayList<>();
    private boolean darkTheme = false;


    public MainInboxWindow(String currentUser) {
        this.currentUser = currentUser;
    }

    // MainInboxWindow.java
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Bandeja de Entrada - Anontalk");
        stage.setOnCloseRequest(e -> {
            if (refresher != null) refresher.stop();
            Platform.exit();
            System.exit(0);
        });

        // Texto de bienvenida
        Label lblWelcome = new Label("Bienvenido, " + currentUser);
        lblWelcome.getStyleClass().add("welcome-label");

        // Icono de perfil
        profileIconView = new ImageView(userIconLight);
        Button btnPerfil = new Button();
        btnPerfil.setGraphic(profileIconView);
        btnPerfil.setStyle("-fx-background-color: transparent;");
        btnPerfil.setOnAction(e -> {
            try {
                new ProfileWindow(currentUser, this.stage).show();
            } catch (Exception ex) {
                pop.mostrarAlertaError("Error", "No se pudo abrir la ventana de perfil.");
            }
        });

        // Menú Idioma
        Menu idiomaMenu = new Menu("Idioma");
        MenuItem miCastellano = new MenuItem("Castellano");
        MenuItem miCatalan   = new MenuItem("Catalán");
        MenuItem miIngles    = new MenuItem("Inglés");
        idiomaMenu.getItems().addAll(miCastellano, miCatalan, miIngles);

        // Menú Tema
        Menu temaMenu = new Menu("Tema");
        MenuItem miOscuro = new MenuItem("Oscuro");
        MenuItem miClaro  = new MenuItem("Claro");
        temaMenu.getItems().addAll(miOscuro, miClaro);

        // Botón Ajustes
        settingsIconView = new ImageView(settingsIconLight);
        MenuButton btnSettings = new MenuButton();
        btnSettings.setGraphic(settingsIconView);
        btnSettings.setStyle("-fx-background-color: transparent;");
        btnSettings.getItems().addAll(idiomaMenu, temaMenu);

        // Handlers de idioma (TODO: recargar recursos)
        miCastellano.setOnAction(e -> Locale.setDefault(new Locale("es", "ES")));
        miCatalan.setOnAction(e -> Locale.setDefault(new Locale("ca", "ES")));
        miIngles.setOnAction(e -> Locale.setDefault(new Locale("en", "US")));

        // Agrupación de iconos
        HBox iconsBox = new HBox(2, btnPerfil, btnSettings);
        iconsBox.setAlignment(Pos.CENTER_LEFT);

        // Barra superior
        HBox leftBox  = new HBox(12, lblWelcome, iconsBox);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        Button btnNuevo = new Button("Nuevo Mensaje");
        btnNuevo.setOnAction(e -> showSendDialog());
        Button btnCerrar = new Button("Cerrar Sesión");
        btnCerrar.setOnAction(e -> {
            if (refresher != null) refresher.stop();
            stage.close();
            pop.mostrarAlertaInformativa("Cerrar Sesión", "Has cerrado sesión correctamente.");
            try { new LoginWindow().start(new Stage()); } catch (Exception ignored) {}
        });
        HBox rightBox = new HBox(10, btnNuevo, btnCerrar);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        BorderPane topBar = new BorderPane(leftBox, null, rightBox, null, null);
        topBar.setPadding(new Insets(10));
        topBar.getStyleClass().add("top-bar");

        // Pestañas bandeja/enviados
        TabPane tabs = new TabPane(
                new Tab("Bandeja de Entrada", createTable(true)),
                new Tab("Mensajes Enviados", createTable(false))
        );
        tabs.getTabs().forEach(t -> t.setClosable(false));
        tabs.getStyleClass().add("inbox-tabs");

        // Layout principal
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabs);
        root.setPadding(new Insets(10));
        root.getStyleClass().add("main-root");

        // Escena y gestión de tema
        Scene scene = new Scene(root, 800, 600);
        // Inicializa con el CSS actual y añade listener para cambios
        ThemeManager tm = ThemeManager.getInstance();
        scene.getStylesheets().setAll(tm.getCss());
        tm.themeProperty().addListener((obs, oldT, newT) -> {
            scene.getStylesheets().setAll(tm.getCss());
        });

        // Handlers de tema
        miOscuro.setOnAction(e -> {
            tm.setTheme("dark");
            profileIconView.setImage(userIconDark);
            settingsIconView.setImage(settingsIconDark);
            trashButtons.forEach(btn -> btn.setGraphic(new ImageView(trashIconDark)));
        });
        miClaro.setOnAction(e -> {
            tm.setTheme("light");
            profileIconView.setImage(userIconLight);
            settingsIconView.setImage(settingsIconLight);
            trashButtons.forEach(btn -> btn.setGraphic(new ImageView(trashIconLight)));
        });

        // Mostrar y arrancar refresco
        stage.setScene(scene);
        stage.show();
        loadMessages();
        refresher = new Timeline(new KeyFrame(Duration.seconds(5), ev -> refreshInbox()));
        refresher.setCycleCount(Timeline.INDEFINITE);
        refresher.play();
    }

    private TableView<Mensaje> createTable(boolean inbox) {
        var list = inbox ? MessageStore.inboxMessages : MessageStore.sentMessages;
        TableView<Mensaje> table = new TableView<>(list);
        table.getStyleClass().add("bandeja-tabla");

        // Columna Remitente/Destinatario
        TableColumn<Mensaje, String> colParty = new TableColumn<>(inbox ? "Remitente" : "Destinatario");
        colParty.setCellValueFactory(new PropertyValueFactory<>("sender"));
        colParty.setPrefWidth(150);

        // Columna Asunto
        TableColumn<Mensaje, String> colAsunto = new TableColumn<>("Asunto");
        colAsunto.setCellValueFactory(new PropertyValueFactory<>("asunto"));
        colAsunto.setPrefWidth(580);

        // Columna Eliminar
        TableColumn<Mensaje, Void> colDel = new TableColumn<>("");
        colDel.setPrefWidth(40);
        colDel.setCellFactory(tc -> new TableCell<>() {
            private final Button btn = new Button();

            {
                Image icon = darkTheme ? trashIconDark : trashIconLight;
                btn.setGraphic(new ImageView(icon));
                btn.setStyle("-fx-background-color: transparent;");
                btn.setOnAction(e -> {
                    Mensaje m = getTableRow().getItem();
                    if (m != null) deleteMessage(m);
                });
                trashButtons.add(btn);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(colParty, colAsunto, colDel);
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


    private void loadMessages() {
        refreshInbox();
        refreshSent();
    }

    private void refreshInbox() {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/messages/inbox/" + currentUser)).GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            List<MensajeDTO> inbox = mapper.readValue(res.body(), new TypeReference<>() {
            });
            Platform.runLater(() -> MessageStore.inboxMessages.setAll(inbox.stream().map(this::dtoToMensajeInbox).toList()));
        } catch (Exception ignored) {
        }
    }

    private void refreshSent() {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/messages/sent/" + currentUser)).GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            List<MensajeDTO> sent = mapper.readValue(res.body(), new TypeReference<>() {
            });
            Platform.runLater(() -> MessageStore.sentMessages.setAll(sent.stream().map(this::dtoToMensajeSent).toList()));
        } catch (Exception ignored) {
        }
    }

    private Mensaje dtoToMensajeInbox(MensajeDTO dto) {
        String plain;
        try {
            plain = HybridCrypto.decrypt(new HybridCrypto.HybridPayload(dto.getCipherTextBase64(), dto.getEncKeyBase64(), dto.getIvBase64()), KeyManager.getPrivateKey());
        } catch (Exception ex) {
            plain = "[Error al descifrar]";
        }
        return new Mensaje(dto.getId(), dto.getRemitente(), dto.getAsunto(), plain);
    }

    private Mensaje dtoToMensajeSent(MensajeDTO dto) {
        String plain;
        try {
            plain = HybridCrypto.decrypt(new HybridCrypto.HybridPayload(dto.getCipherTextBase64(), dto.getEncKeyBase64(), dto.getIvBase64()), KeyManager.getPrivateKey());
        } catch (Exception ex) {
            plain = "[Error al descifrar]";
        }
        return new Mensaje(dto.getId(), dto.getDestinatario(), dto.getAsunto(), plain);
    }

    private void showSendDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("Redactar mensaje");

        TextField txtPara = new TextField();
        txtPara.setPromptText("Para");

        TextField txtAsunto = new TextField();
        txtAsunto.setPromptText("Asunto");

        TextArea txtCuerpo = new TextArea();
        txtCuerpo.setPromptText("Escribe tu mensaje...");
        txtCuerpo.setWrapText(true);
        txtCuerpo.setPrefHeight(200);

        Button btnEnviar = new Button("Enviar");
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> dialog.close());

        btnEnviar.setOnAction(e -> {
            String dest = txtPara.getText().trim();
            String cuerpo = txtCuerpo.getText().trim();
            if (dest.isBlank() || cuerpo.isBlank()) {
                pop.mostrarAlertaError("Error", "Completa al menos el destinatario y el cuerpo.");
                return;
            }
            try {
                HttpRequest pkReq = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/" + dest + "/publicKey")).GET().build();
                HttpResponse<String> pkRes = http.send(pkReq, HttpResponse.BodyHandlers.ofString());

                if (pkRes.statusCode() == 404) {
                    pop.mostrarAlertaError("Usuario desconocido", "El usuario '" + dest + "' no existe.");
                    return;
                } else if (pkRes.statusCode() != 200) {
                    pop.mostrarAlertaError("Error servidor", "No se pudo verificar el usuario.");
                    return;
                }

                PublicKey destPk = RSAUtils.publicKeyFromBase64(pkRes.body());
                HybridCrypto.HybridPayload p = HybridCrypto.encrypt(cuerpo, destPk);

                MensajeDTO dto = new MensajeDTO();
                dto.setRemitente(currentUser);
                dto.setDestinatario(dest);
                dto.setAsunto(txtAsunto.getText().trim());
                dto.setCipherTextBase64(p.cipherB64());
                dto.setEncKeyBase64(p.encKeyB64());
                dto.setIvBase64(p.ivB64());

                String json = mapper.writeValueAsString(dto);
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/messages/send")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();

                http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(r -> {
                    if (r.statusCode() == 200) {
                        Platform.runLater(() -> {
                            refreshSent();
                            dialog.close();
                            pop.mostrarAlertaInformativa("Enviado", "Mensaje enviado con éxito.");
                        });
                    } else {
                        Platform.runLater(() -> pop.mostrarAlertaError("Error", "No se pudo enviar el mensaje."));
                    }
                });
            } catch (Exception ex) {
                pop.mostrarAlertaError("Error", "Fallo al cifrar o enviar el mensaje.");
            }
        });

        HBox botones = new HBox(10, btnEnviar, btnCancelar);
        botones.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(10, txtPara, txtAsunto, txtCuerpo, botones);
        layout.setPadding(new Insets(20));
        layout.setPrefWidth(500);

        Scene scene = new Scene(layout);
        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }

    private void deleteMessage(Mensaje msg) {
        MessageStore.inboxMessages.removeIf(m -> m.getId().equals(msg.getId()));
        MessageStore.sentMessages.removeIf(m -> m.getId().equals(msg.getId()));
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/messages/" + msg.getId())).DELETE().build();
            http.sendAsync(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }
}
