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
import utils.LocaleManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.util.*;

/**
 * Bandeja principal (inbox + enviados) con cambio de idioma en caliente.
 */
public class MainInboxWindow extends Application {

    /* ======= inyección ======= */
    private final String currentUser;
    private final ConfigurableApplicationContext springCtx;

    /* ======= servicios/estado ======= */
    private final PopUpInfo pop = new PopUpInfo();
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private Timeline refresher;               // refresco 5 s “inbox”
    private boolean darkTheme = false;

    /* ======= nodos que cambian con el idioma ======= */
    private Label lblWelcome;
    private Menu idiomaMenu;
    private Menu temaMenu;
    private TabPane tabs;

    /* ======= iconos ======= */
    private final Image userIconLight     = new Image(getClass().getResourceAsStream("/user.png"),     30, 30, true, true);
    private final Image userIconDark      = new Image(getClass().getResourceAsStream("/user2.png"),    30, 30, true, true);
    private final Image trashIconLight    = new Image(getClass().getResourceAsStream("/papelera.png"), 16, 16, true, true);
    private final Image trashIconDark     = new Image(getClass().getResourceAsStream("/papelera2.png"),16, 16, true, true);
    private final Image settingsIconLight = new Image(getClass().getResourceAsStream("/ajustes.png"),  30, 30, true, true);
    private final Image settingsIconDark  = new Image(getClass().getResourceAsStream("/ajustes2.png"), 30, 30, true, true);
    private final Image newMsgIconLight   = new Image(getClass().getResourceAsStream("/newM.png"),     30, 30, true, true);
    private final Image newMsgIconDark    = new Image(getClass().getResourceAsStream("/newM2.png"),    30, 30, true, true);
    private final Image logoutIconLight   = new Image(getClass().getResourceAsStream("/logOut.png"),   30, 28, true, true);
    private final Image logoutIconDark    = new Image(getClass().getResourceAsStream("/logOut2.png"),  30, 28, true, true);

    private ImageView profileIconView;
    private ImageView settingsIconView;
    private ImageView newMsgIconView;
    private ImageView logoutIconView;

    /* para cambiar el icono de la papelera cuando alternamos tema */
    private final List<Button> trashButtons = new ArrayList<>();

    /* ======= ventana/escena ======= */
    private Stage stage;
    private Scene scene;
    private double xOffset, yOffset;          // drag-window

    /* ---------------------------------------------------------------------------------------- */

    public MainInboxWindow(String currentUser, ConfigurableApplicationContext springCtx) {
        this.currentUser = currentUser;
        this.springCtx   = springCtx;
    }

    /* ======================================================================================== */
    /*                                         start()                                          */
    /* ======================================================================================== */

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        /* idioma por defecto */
        LocaleManager.setLocale(Locale.ENGLISH);

        /* cerrar app */
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setOnCloseRequest(e -> {
            if (refresher != null) refresher.stop();
            Platform.exit();
            System.exit(0);
        });

        /* construimos UI */
        BorderPane root = buildUI();
        scene = new Scene(root, 791, 600);
        ThemeManager tm = ThemeManager.getInstance();
        scene.getStylesheets().setAll(tm.getCss());
        tm.themeProperty().addListener((o,oldT,n)-> scene.getStylesheets().setAll(tm.getCss()));

        /* listener de idioma */
        LocaleManager.localeProperty().addListener((o,oldL,newL)-> refreshTexts());
        refreshTexts();            // 1ª vez

        stage.setScene(scene);
        stage.show();

        /* carga + refresco periódico */
        loadMessages();
        refresher = new Timeline(new KeyFrame(Duration.seconds(5), ev -> refreshInbox()));
        refresher.setCycleCount(Timeline.INDEFINITE);
        refresher.play();
    }

    /* ======================================================================================== */
    /*                                     CONSTRUCCIÓN UI                                      */
    /* ======================================================================================== */

    private BorderPane buildUI() {
        /* ───────── Label Bienvenida ───────── */
        lblWelcome = new Label();
        lblWelcome.getStyleClass().add("welcome-label");

        /* ───────── Botón Perfil ───────── */
        profileIconView = new ImageView(darkTheme ? userIconDark : userIconLight);
        Button btnPerfil = new Button(null, profileIconView);
        btnPerfil.getStyleClass().add("icon-button");
        btnPerfil.setOnAction(e -> {
            try { new ProfileWindow(currentUser, stage, springCtx).show(); }
            catch (Exception ex) { pop.mostrarAlertaError(bundle().getString("common.error"),
                    bundle().getString("profile.alert.error.openProfile")); }
        });

        /* ───────── Menú Ajustes (Idioma / Tema) ───────── */
        settingsIconView = new ImageView(darkTheme ? settingsIconDark : settingsIconLight);

        idiomaMenu = new Menu();
        idiomaMenu.getItems().addAll(
                new MenuItem(), new MenuItem(), new MenuItem()
        );
        idiomaMenu.getItems().get(0).setOnAction(e -> LocaleManager.setLocale(new Locale("es","ES")));
        idiomaMenu.getItems().get(1).setOnAction(e -> LocaleManager.setLocale(new Locale("ca","ES")));
        idiomaMenu.getItems().get(2).setOnAction(e -> LocaleManager.setLocale(Locale.ENGLISH));

        temaMenu = new Menu();
        temaMenu.getItems().addAll(
                new MenuItem(), new MenuItem()
        );
        temaMenu.getItems().get(0).setOnAction(e -> setDarkTheme());
        temaMenu.getItems().get(1).setOnAction(e -> setLightTheme());

        MenuButton btnSettings = new MenuButton(null, settingsIconView, idiomaMenu, temaMenu);
        btnSettings.getStyleClass().add("icon-button");

        /* ───────── Botón Nuevo / Logout ───────── */
        newMsgIconView = new ImageView(darkTheme ? newMsgIconDark : newMsgIconLight);
        Button btnNuevo = new Button(null, newMsgIconView);
        btnNuevo.getStyleClass().add("icon-button");
        btnNuevo.setOnAction(e -> showSendDialog());

        logoutIconView = new ImageView(darkTheme ? logoutIconDark : logoutIconLight);
        Button btnLogout = new Button(null, logoutIconView);
        btnLogout.getStyleClass().add("icon-button");
        btnLogout.setOnAction(e -> {
            if (refresher != null) refresher.stop();
            stage.close();
            pop.mostrarAlertaInformativa(bundle().getString("session.logout.title"),
                    bundle().getString("session.logout.info"));
            try { new LoginWindow(springCtx).start(new Stage()); }
            catch (Exception ex) { ex.printStackTrace(); Platform.exit(); }
        });

        /* ───────── Top Bar ───────── */
        HBox leftBox  = new HBox(lblWelcome);                        leftBox.setAlignment(Pos.CENTER_LEFT);
        HBox rightBox = new HBox(2, btnNuevo, btnPerfil, btnSettings, btnLogout); rightBox.setAlignment(Pos.CENTER_RIGHT);

        BorderPane topBar = new BorderPane(leftBox, null, rightBox, null, null);
        topBar.setPadding(new Insets(10));
        topBar.getStyleClass().add("top-bar");

        /* ───────── TabPane ───────── */
        tabs = new TabPane(
                new Tab("", createTable(true)),
                new Tab("", createTable(false))
        );
        tabs.getTabs().forEach(t -> t.setClosable(false));
        tabs.getStyleClass().add("inbox-tabs");

        /* ───────── Root + drag window ───────── */
        BorderPane root = new BorderPane(tabs);
        root.setTop(topBar);
        root.setPadding(new Insets(10));
        root.getStyleClass().add("main-root");

        root.setOnMousePressed(ev -> { xOffset = ev.getSceneX(); yOffset = ev.getSceneY(); });
        root.setOnMouseDragged(ev -> { stage.setX(ev.getScreenX() - xOffset); stage.setY(ev.getScreenY() - yOffset); });

        return root;
    }

    /* ======================================================================================== */
    /*                                   REFRESH TEXTS / THEME                                  */
    /* ======================================================================================== */

    private void refreshTexts() {
        ResourceBundle b = bundle();

        stage.setTitle(b.getString("inbox.window.title"));
        lblWelcome.setText(MessageFormat.format(b.getString("inbox.welcome"), currentUser));

        idiomaMenu.setText(b.getString("menu.language"));
        idiomaMenu.getItems().get(0).setText(b.getString("menu.language.spanish"));
        idiomaMenu.getItems().get(1).setText(b.getString("menu.language.catalan"));
        idiomaMenu.getItems().get(2).setText(b.getString("menu.language.english"));

        temaMenu.setText(b.getString("menu.theme"));
        temaMenu.getItems().get(0).setText(b.getString("menu.theme.dark"));
        temaMenu.getItems().get(1).setText(b.getString("menu.theme.light"));

        tabs.getTabs().get(0).setText(b.getString("tab.inbox"));
        tabs.getTabs().get(1).setText(b.getString("tab.sent"));

        updateTableTexts((TableView<?>) tabs.getTabs().get(0).getContent(), true);
        updateTableTexts((TableView<?>) tabs.getTabs().get(1).getContent(), false);
    }

    private void updateTableTexts(TableView<?> tv, boolean inbox) {
        ResourceBundle b = bundle();
        String partyKey = inbox ? "table.column.sender" : "table.column.recipient";
        tv.getColumns().get(0).setText(b.getString(partyKey));
        tv.getColumns().get(1).setText(b.getString("table.column.subject"));
        ((Label) tv.getPlaceholder()).setText(
                inbox ? b.getString("table.placeholder.inbox")
                        : b.getString("table.placeholder.sent")
        );
    }

    private void setDarkTheme() {
        ThemeManager tm = ThemeManager.getInstance();
        tm.setTheme("dark");  darkTheme = true;
        scene.getStylesheets().setAll(tm.getCss());
        profileIconView.setImage(userIconDark); settingsIconView.setImage(settingsIconDark);
        newMsgIconView.setImage(newMsgIconDark); logoutIconView.setImage(logoutIconDark);
        trashButtons.forEach(b -> b.setGraphic(new ImageView(trashIconDark)));
    }

    private void setLightTheme() {
        ThemeManager tm = ThemeManager.getInstance();
        tm.setTheme("light"); darkTheme = false;
        scene.getStylesheets().setAll(tm.getCss());
        profileIconView.setImage(userIconLight); settingsIconView.setImage(settingsIconLight);
        newMsgIconView.setImage(newMsgIconLight); logoutIconView.setImage(logoutIconLight);
        trashButtons.forEach(b -> b.setGraphic(new ImageView(trashIconLight)));
    }

    /* ======================================================================================== */
    /*                                        TABLAS                                            */
    /* ======================================================================================== */

    private TableView<Mensaje> createTable(boolean inbox) {
        TableView<Mensaje> table = new TableView<>(inbox ? MessageStore.inboxMessages : MessageStore.sentMessages);
        table.getStyleClass().add("bandeja-tabla");

        String partyKey = inbox ? "table.column.sender" : "table.column.recipient";
        TableColumn<Mensaje,String> colParty = new TableColumn<>(bundle().getString(partyKey));
        colParty.setCellValueFactory(new PropertyValueFactory<>("sender"));
        colParty.setPrefWidth(150);

        TableColumn<Mensaje,String> colSubject = new TableColumn<>(bundle().getString("table.column.subject"));
        colSubject.setCellValueFactory(new PropertyValueFactory<>("asunto"));
        colSubject.setPrefWidth(580);

        TableColumn<Mensaje,Void> colDel = new TableColumn<>("");
        colDel.setPrefWidth(40);
        colDel.setCellFactory(tc -> new TableCell<>() {
            private final Button btn = new Button();

            {   // ctor de la celda
                btn.setGraphic(new ImageView(darkTheme ? trashIconDark : trashIconLight));
                btn.setStyle("-fx-background-color: transparent;");
                btn.setOnAction(e -> deleteMessage(getTableView().getItems().get(getIndex())));
                trashButtons.add(btn);
                setAlignment(Pos.CENTER);
            }
            @Override protected void updateItem(Void it, boolean empty) {
                super.updateItem(it, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(colParty, colSubject, colDel);
        table.setPlaceholder(new Label(
                inbox ? bundle().getString("table.placeholder.inbox")
                        : bundle().getString("table.placeholder.sent")
        ));
        table.setRowFactory(tv -> {
            TableRow<Mensaje> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (!row.isEmpty() && ev.getClickCount() == 2)
                    new ChatWindow(currentUser, row.getItem()).show();
            });
            return row;
        });
        return table;
    }

    /* ======================================================================================== */
    /*                                    CARGA DE MENSAJES                                     */
    /* ======================================================================================== */

    private void loadMessages() { refreshInbox(); refreshSent(); }

    private void refreshInbox() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/messages/inbox/" + currentUser))
                    .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            List<MensajeDTO> inbox = mapper.readValue(res.body(), new TypeReference<>() {});
            Platform.runLater(() ->
                    MessageStore.inboxMessages.setAll(inbox.stream().map(this::mapInbox).toList()));
        } catch (Exception ignored) {}
    }

    private void refreshSent() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/messages/sent/" + currentUser))
                    .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            List<MensajeDTO> sent = mapper.readValue(res.body(), new TypeReference<>() {});
            Platform.runLater(() ->
                    MessageStore.sentMessages.setAll(sent.stream().map(this::mapSent).toList()));
        } catch (Exception ignored) {}
    }

    private Mensaje mapInbox(MensajeDTO dto) {
        String plain;
        try {
            plain = HybridCrypto.decrypt(
                    new HybridCrypto.HybridPayload(dto.getCipherTextBase64(),
                            dto.getEncKeyBase64(),
                            dto.getIvBase64()),
                    KeyManager.getPrivateKey());
        } catch (Exception ex) { plain = "[Error al descifrar]"; }
        return new Mensaje(dto.getId(), dto.getRemitente(), dto.getAsunto(), plain);
    }

    private Mensaje mapSent(MensajeDTO dto) {
        String plain;
        try {
            plain = HybridCrypto.decrypt(
                    new HybridCrypto.HybridPayload(dto.getCipherTextBase64(),
                            dto.getEncKeyBase64(),
                            dto.getIvBase64()),
                    KeyManager.getPrivateKey());
        } catch (Exception ex) { plain = "[Error al descifrar]"; }
        return new Mensaje(dto.getId(), dto.getDestinatario(), dto.getAsunto(), plain);
    }

    /* ======================================================================================== */
    /*                                   ENVÍO / BORRADO                                        */
    /* ======================================================================================== */

    private void deleteMessage(Mensaje msg) {
        MessageStore.inboxMessages.removeIf(m -> m.getId().equals(msg.getId()));
        MessageStore.sentMessages .removeIf(m -> m.getId().equals(msg.getId()));
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/messages/" + msg.getId()))
                    .DELETE().build();
            http.sendAsync(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {}
    }

    private void showSendDialog() {
        ResourceBundle b = bundle();

        Stage dlg = new Stage();
        dlg.setTitle(b.getString("dialog.newMessage.title"));

        TextField txtTo     = new TextField(); txtTo    .setPromptText(b.getString("dialog.newMessage.field.to"));
        TextField txtSubj   = new TextField(); txtSubj  .setPromptText(b.getString("dialog.newMessage.field.subject"));
        TextArea  txtBody   = new TextArea (); txtBody  .setPromptText(b.getString("dialog.newMessage.field.body"));
        txtBody.setPrefHeight(200); txtBody.setWrapText(true); txtBody.getStyleClass().add("chat-textarea");

        Button btnSend = new Button(b.getString("dialog.newMessage.button.send"));
        Button btnCanc = new Button(b.getString("dialog.newMessage.button.cancel"));
        btnCanc.setOnAction(e -> dlg.close());

        btnSend.setOnAction(e -> {                       /* … igual que antes … */
            String dest  = txtTo  .getText().trim();
            String plain = txtBody.getText().trim();
            if (dest.isBlank() || plain.isBlank()) {
                pop.mostrarAlertaError(b.getString("common.error"), b.getString("dialog.newMessage.error.incomplete"));
                return;
            }
            try {
                HttpRequest pkReq = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/users/" + dest + "/publicKey"))
                        .GET().build();
                HttpResponse<String> pkRes = http.send(pkReq, HttpResponse.BodyHandlers.ofString());
                if (pkRes.statusCode() == 404) {
                    pop.mostrarAlertaError(b.getString("common.error"),
                            MessageFormat.format(b.getString("dialog.newMessage.error.unknownUser"), dest));
                    return;
                }
                if (pkRes.statusCode() != 200) {
                    pop.mostrarAlertaError(b.getString("common.error"), b.getString("dialog.newMessage.error.server"));
                    return;
                }
                PublicKey pkDest = RSAUtils.publicKeyFromBase64(pkRes.body());
                var payload = HybridCrypto.encrypt(plain, pkDest);

                MensajeDTO dto = new MensajeDTO();
                dto.setRemitente(currentUser);
                dto.setDestinatario(dest);
                dto.setAsunto(txtSubj.getText().trim());
                dto.setCipherTextBase64(payload.cipherB64());
                dto.setEncKeyBase64(payload.encKeyB64());
                dto.setIvBase64(payload.ivB64());

                String json = mapper.writeValueAsString(dto);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/messages/send"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build();

                http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(res -> Platform.runLater(() -> {
                            if (res.statusCode() == 200) {
                                refreshSent(); dlg.close();
                                pop.mostrarAlertaInformativa(b.getString("common.success"),
                                        b.getString("dialog.newMessage.info.sent"));
                            } else {
                                pop.mostrarAlertaError(b.getString("common.error"),
                                        b.getString("dialog.newMessage.error.send"));
                            }
                        }));
            } catch (Exception ex) {
                pop.mostrarAlertaError(b.getString("common.error"), b.getString("dialog.newMessage.error.send"));
            }
        });

        HBox bar = new HBox(10, btnSend, btnCanc); bar.setAlignment(Pos.CENTER_RIGHT);
        VBox lay = new VBox(10, txtTo, txtSubj, txtBody, bar); lay.setPadding(new Insets(20));

        Scene sc = new Scene(lay, 500, 350);
        scene.getStylesheets().forEach(css -> sc.getStylesheets().add(css));   // mismo tema actual
        dlg.setScene(sc); dlg.show();
    }

    /* ======================================================================================== */
    private static ResourceBundle bundle() { return LocaleManager.bundle(); }
}
