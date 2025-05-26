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
import javafx.stage.StageStyle;
import javafx.util.Duration;
import managers.PopUpInfo;
import managers.mensajes.Mensaje;
import managers.mensajes.MensajeDTO;
import managers.mensajes.MessageStore;
import org.springframework.context.ConfigurableApplicationContext;
import security.encryption.HybridCrypto;
import security.encryption.KeyManager;
import security.encryption.RSAUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.util.*;

public class MainInboxWindow extends Application {

    private final String currentUser;
    private final PopUpInfo pop = new PopUpInfo();
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private Timeline refresher;

    // Iconos claro/oscuro
    private final Image userIconLight = new Image(getClass().getResourceAsStream("/user.png"), 30, 30, true, true);
    private final Image userIconDark = new Image(getClass().getResourceAsStream("/user2.png"), 30, 30, true, true);
    private final Image trashIconLight = new Image(getClass().getResourceAsStream("/papelera.png"), 16, 16, true, true);
    private final Image trashIconDark = new Image(getClass().getResourceAsStream("/papelera2.png"), 16, 16, true, true);
    private final Image settingsIconLight = new Image(getClass().getResourceAsStream("/ajustes.png"), 30, 30, true, true);
    private final Image settingsIconDark = new Image(getClass().getResourceAsStream("/ajustes2.png"), 30, 30, true, true);
    private final Image newMsgIconLight = new Image(getClass().getResourceAsStream("/newM.png"), 30, 30, true, true);
    private final Image newMsgIconDark = new Image(getClass().getResourceAsStream("/newM2.png"), 30, 30, true, true);
    private final Image logoutIconLight = new Image(getClass().getResourceAsStream("/logOut.png"), 30, 28, true, true);
    private final Image logoutIconDark = new Image(getClass().getResourceAsStream("/logOut2.png"), 30, 28, true, true);

    private ImageView profileIconView;
    private ImageView settingsIconView;
    private ImageView newMsgIconView;
    private ImageView logoutIconView;

    private final List<Button> trashButtons = new ArrayList<>();
    private boolean darkTheme = false;
    private final ConfigurableApplicationContext springCtx;

    private Stage stage;
    private Scene scene;
    private ResourceBundle b;
    private double xOffset = 0;
    private double yOffset = 0;

    public MainInboxWindow(String currentUser, ConfigurableApplicationContext springCtx) {
        this.currentUser = currentUser;
        this.springCtx = springCtx;
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        // Inicializa en inglés por defecto y carga ResourceBundle
        changeLanguage(Locale.ENGLISH);

        // Quitar la decoración nativa
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setOnCloseRequest(e -> {
            if (refresher != null) refresher.stop();
            Platform.exit();
            System.exit(0);
        });

        // Construir interfaz
        BorderPane root = buildUI();
        this.scene = new Scene(root, 791, 600);
        ThemeManager tm = ThemeManager.getInstance();
        scene.getStylesheets().setAll(tm.getCss());
        tm.themeProperty().addListener((o, old, ne) -> scene.getStylesheets().setAll(tm.getCss()));

        stage.setScene(scene);
        stage.show();

        // Cargar y refrescar mensajes periódicamente
        loadMessages();
        refresher = new Timeline(new KeyFrame(Duration.seconds(5), ev -> refreshInbox()));
        refresher.setCycleCount(Timeline.INDEFINITE);
        refresher.play();
    }

    private void changeLanguage(Locale locale) {
        Locale.setDefault(locale);
        b = ResourceBundle.getBundle("i18n/messages", locale);
        // Si la UI ya está mostrada, reconstruir conservando tamaño
        if (stage != null && scene != null) {
            double w = scene.getWidth();
            double h = scene.getHeight();
            BorderPane root = buildUI();
            this.scene = new Scene(root, w, h);
            ThemeManager tm = ThemeManager.getInstance();
            scene.getStylesheets().setAll(tm.getCss());
            tm.themeProperty().addListener((o, old, ne) -> scene.getStylesheets().setAll(tm.getCss()));
            stage.setScene(scene);
        }
    }

    private BorderPane buildUI() {
        // Título de la ventana
        stage.setTitle(b.getString("inbox.window.title"));

        // Label de bienvenida
        Label lblWelcome = new Label(MessageFormat.format(b.getString("inbox.welcome"), currentUser));
        lblWelcome.getStyleClass().add("welcome-label");

        // Botón Perfil
        profileIconView = new ImageView(darkTheme ? userIconDark : userIconLight);
        Button btnPerfil = new Button();
        btnPerfil.setGraphic(profileIconView);
        btnPerfil.getStyleClass().add("icon-button");
        btnPerfil.setOnAction(e -> {
            try {
                new ProfileWindow(currentUser, stage, springCtx).show();
            } catch (Exception ex) {
                pop.mostrarAlertaError(b.getString("common.error"), b.getString("profile.alert.error.openProfile"));
            }
        });

        // Botón Ajustes (Idioma/Tema)
        settingsIconView = new ImageView(darkTheme ? settingsIconDark : settingsIconLight);
        Menu idiomaMenu = new Menu(b.getString("menu.language"), null, new MenuItem(b.getString("menu.language.spanish")), new MenuItem(b.getString("menu.language.catalan")), new MenuItem(b.getString("menu.language.english")));
        Menu temaMenu = new Menu(b.getString("menu.theme"), null, new MenuItem(b.getString("menu.theme.dark")), new MenuItem(b.getString("menu.theme.light")));
        MenuButton btnSettings = new MenuButton();
        btnSettings.setGraphic(settingsIconView);
        btnSettings.getStyleClass().add("icon-button");
        btnSettings.getItems().addAll(idiomaMenu, temaMenu);

        // Handlers Idioma
        idiomaMenu.getItems().get(0).setOnAction(e -> changeLanguage(new Locale("es", "ES")));
        idiomaMenu.getItems().get(1).setOnAction(e -> changeLanguage(new Locale("ca", "ES")));
        idiomaMenu.getItems().get(2).setOnAction(e -> changeLanguage(Locale.ENGLISH));

        // Handlers Tema
        temaMenu.getItems().get(0).setOnAction(e -> setDarkTheme());
        temaMenu.getItems().get(1).setOnAction(e -> setLightTheme());

        // Botón Nuevo Mensaje
        newMsgIconView = new ImageView(darkTheme ? newMsgIconDark : newMsgIconLight);
        Button btnNuevo = new Button();
        btnNuevo.setGraphic(newMsgIconView);
        btnNuevo.getStyleClass().add("icon-button");
        btnNuevo.setOnAction(e -> showSendDialog());

        // Botón Cerrar Sesión
        logoutIconView = new ImageView(darkTheme ? logoutIconDark : logoutIconLight);
        Button btnCerrar = new Button();
        btnCerrar.setGraphic(logoutIconView);
        btnCerrar.getStyleClass().add("icon-button");
        btnCerrar.setOnAction(e -> {
            if (refresher != null) refresher.stop();
            stage.close();
            pop.mostrarAlertaInformativa(b.getString("session.logout.title"), b.getString("session.logout.info"));
            try {
                new LoginWindow(springCtx).start(new Stage());
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.exit();
            }
        });

        // Top bar layout
        HBox leftBox = new HBox(lblWelcome);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        HBox rightIcons = new HBox(2, btnNuevo, btnPerfil, btnSettings, btnCerrar);
        rightIcons.setAlignment(Pos.CENTER_RIGHT);
        BorderPane topBar = new BorderPane();
        topBar.setLeft(leftBox);
        topBar.setRight(rightIcons);
        topBar.setPadding(new Insets(10));
        topBar.getStyleClass().add("top-bar");

        // Pestañas Inbox/Sent
        TabPane tabs = new TabPane(new Tab(b.getString("tab.inbox"), createTable(true)), new Tab(b.getString("tab.sent"), createTable(false)));
        tabs.getTabs().forEach(t -> t.setClosable(false));
        tabs.getStyleClass().add("inbox-tabs");

        // Root layout
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabs);
        root.setPadding(new Insets(10));
        root.getStyleClass().add("main-root");

        // Ventana arrastrable
        root.setOnMousePressed(ev -> {
            xOffset = ev.getSceneX();
            yOffset = ev.getSceneY();
        });
        root.setOnMouseDragged(ev -> {
            stage.setX(ev.getScreenX() - xOffset);
            stage.setY(ev.getScreenY() - yOffset);
        });

        return root;
    }

    private void setDarkTheme() {
        ThemeManager tm = ThemeManager.getInstance();
        tm.setTheme("dark");
        darkTheme = true;
        scene.getStylesheets().setAll(tm.getCss());
        profileIconView.setImage(userIconDark);
        settingsIconView.setImage(settingsIconDark);
        trashButtons.forEach(btn -> btn.setGraphic(new ImageView(trashIconDark)));
        newMsgIconView.setImage(newMsgIconDark);
        logoutIconView.setImage(logoutIconDark);
    }

    private void setLightTheme() {
        ThemeManager tm = ThemeManager.getInstance();
        tm.setTheme("light");
        darkTheme = false;
        scene.getStylesheets().setAll(tm.getCss());
        profileIconView.setImage(userIconLight);
        settingsIconView.setImage(settingsIconLight);
        trashButtons.forEach(btn -> btn.setGraphic(new ImageView(trashIconLight)));
        newMsgIconView.setImage(newMsgIconLight);
        logoutIconView.setImage(logoutIconLight);
    }

    private TableView<Mensaje> createTable(boolean inbox) {
        TableView<Mensaje> table = new TableView<>(inbox ? MessageStore.inboxMessages : MessageStore.sentMessages);
        table.getStyleClass().add("bandeja-tabla");

        String partyKey = inbox ? "table.column.sender" : "table.column.recipient";
        TableColumn<Mensaje, String> colParty = new TableColumn<>(b.getString(partyKey));
        colParty.setCellValueFactory(new PropertyValueFactory<>("sender"));
        colParty.setPrefWidth(150);

        TableColumn<Mensaje, String> colSubject = new TableColumn<>(b.getString("table.column.subject"));
        colSubject.setCellValueFactory(new PropertyValueFactory<>("asunto"));
        colSubject.setPrefWidth(580);

        TableColumn<Mensaje, Void> colDel = new TableColumn<>("");
        colDel.setPrefWidth(40);
        colDel.setCellFactory(tc -> new TableCell<>() {
            private final Button btn = new Button();

            {
                btn.setGraphic(new ImageView(darkTheme ? trashIconDark : trashIconLight));
                btn.setStyle("-fx-background-color: transparent;");
                btn.setOnAction(e -> deleteMessage(getTableView().getItems().get(getIndex())));
                trashButtons.add(btn);
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(colParty, colSubject, colDel);
        table.setPlaceholder(new Label(inbox ? b.getString("table.placeholder.inbox") : b.getString("table.placeholder.sent")));
        table.setRowFactory(tv -> {
            TableRow<Mensaje> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (!row.isEmpty() && ev.getClickCount() == 2) new ChatWindow(currentUser, row.getItem()).show();
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
        dialog.setTitle(b.getString("dialog.newMessage.title"));

        TextField txtPara = new TextField();
        txtPara.setPromptText(b.getString("dialog.newMessage.field.to"));
        TextField txtAsunto = new TextField();
        txtAsunto.setPromptText(b.getString("dialog.newMessage.field.subject"));
        TextArea txtCuerpo = new TextArea();
        txtCuerpo.setPromptText(b.getString("dialog.newMessage.field.body"));
        txtCuerpo.setWrapText(true);
        txtCuerpo.setPrefHeight(200);
        txtCuerpo.getStyleClass().add("chat-textarea");

        Button btnEnviar = new Button(b.getString("dialog.newMessage.button.send"));
        Button btnCancelar = new Button(b.getString("dialog.newMessage.button.cancel"));
        btnCancelar.setOnAction(e -> dialog.close());

        btnEnviar.setOnAction(e -> {
            String dest = txtPara.getText().trim();
            String cuerpo = txtCuerpo.getText().trim();
            if (dest.isBlank() || cuerpo.isBlank()) {
                pop.mostrarAlertaError(b.getString("common.error"), b.getString("dialog.newMessage.error.incomplete"));
                return;
            }
            try {
                HttpRequest pkReq = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/" + dest + "/publicKey")).GET().build();
                HttpResponse<String> pkRes = http.send(pkReq, HttpResponse.BodyHandlers.ofString());
                if (pkRes.statusCode() == 404) {
                    pop.mostrarAlertaError(b.getString("common.error"), MessageFormat.format(b.getString("dialog.newMessage.error.unknownUser"), dest));
                    return;
                } else if (pkRes.statusCode() != 200) {
                    pop.mostrarAlertaError(b.getString("common.error"), b.getString("dialog.newMessage.error.server"));
                    return;
                }
                PublicKey destPk = RSAUtils.publicKeyFromBase64(pkRes.body());
                var payload = HybridCrypto.encrypt(cuerpo, destPk);

                MensajeDTO dto = new MensajeDTO();
                dto.setRemitente(currentUser);
                dto.setDestinatario(dest);
                dto.setAsunto(txtAsunto.getText().trim());
                dto.setCipherTextBase64(payload.cipherB64());
                dto.setEncKeyBase64(payload.encKeyB64());
                dto.setIvBase64(payload.ivB64());

                String json = mapper.writeValueAsString(dto);
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/messages/send")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();
                http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(r -> Platform.runLater(() -> {
                    if (r.statusCode() == 200) {
                        refreshSent();
                        dialog.close();
                        pop.mostrarAlertaInformativa(b.getString("common.success"), b.getString("dialog.newMessage.info.sent"));
                    } else {
                        pop.mostrarAlertaError(b.getString("common.error"), b.getString("dialog.newMessage.error.send"));
                    }
                }));
            } catch (Exception ex) {
                pop.mostrarAlertaError(b.getString("common.error"), b.getString("dialog.newMessage.error.send"));
            }
        });

        HBox botones = new HBox(10, btnEnviar, btnCancelar);
        botones.setAlignment(Pos.CENTER_RIGHT);
        VBox layout = new VBox(10, txtPara, txtAsunto, txtCuerpo, botones);
        layout.setPadding(new Insets(20));
        layout.setPrefWidth(500);

        Scene dlgScene = new Scene(layout);
        ThemeManager tm = ThemeManager.getInstance();
        dlgScene.getStylesheets().setAll(tm.getCss());
        tm.themeProperty().addListener((o, old, ne) -> dlgScene.getStylesheets().setAll(tm.getCss()));

        dialog.setScene(dlgScene);
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