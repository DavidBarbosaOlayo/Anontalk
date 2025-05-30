package windows;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import managers.PopUpInfo;
import managers.mensajes.Mensaje;
import managers.mensajes.MensajeDTO;
import managers.mensajes.MessageStore;
import managers.mensajes.adjuntos.AdjuntoDTO;
import org.springframework.context.ConfigurableApplicationContext;
import security.encryption.HybridCrypto;
import security.encryption.KeyManager;
import security.encryption.RSAUtils;
import utils.LocaleManager;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.time.LocalDateTime;
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
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private Timeline refresher;               // refresco 5 s “inbox”
    private boolean darkTheme = false;

    /* ======= nodos que cambian con el idioma ======= */
    private Label lblWelcome;
    private Menu idiomaMenu;
    private Menu temaMenu;
    private TabPane tabs;
    private StackPane tabsWrapper;

    // >>> CAMBIO 1: nuevos campos para la pestaña Inbox (texto + badge) <<<
    private Label inboxTabTextLabel;
    private Label inboxBadgeLabel;

    /* ======= iconos ======= */
    private final Image userIconLight = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/user.png")), 30, 30, true, true);
    private final Image userIconDark = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/user2.png")), 30, 30, true, true);
    private final Image logoImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/logo.png")), 42, 42, true, true);
    private final Image logoImgDark = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/logoDark.png")), 42, 42, true, true);
    private final Image trashIconLight = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/papelera.png")), 16, 16, true, true);
    private final Image trashIconDark = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/papelera2.png")), 16, 16, true, true);
    private final Image settingsIconLight = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/ajustes.png")), 30, 30, true, true);
    private final Image settingsIconDark = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/ajustes2.png")), 30, 30, true, true);
    private final Image newMsgIconLight = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/newM.png")), 30, 30, true, true);
    private final Image newMsgIconDark = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/newM2.png")), 30, 30, true, true);
    private final Image logoutIconLight = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/logOut.png")), 30, 28, true, true);
    private final Image logoutIconDark = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/logOut2.png")), 30, 28, true, true);
    /* ───────── iconos redactar ───────── */
    private final Image icoEncrypt = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cifrado.png")), 36, 36, true, true);
    private final Image icoEncryptOn = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cifrado1.png")), 36, 36, true, true);
    private final Image icoTimer = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/timer.png")), 36, 36, true, true);
    private final Image icoTimerOn = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/timer1.png")), 36, 36, true, true);
    private final Image icoAttach = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/añadir.png")), 36, 36, true, true);
    private final Image icoAttachOn = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/añadir1.png")), 36, 36, true, true);
    /* ───────── iconos redactar (DARK) ───────── */
    private final Image icoEncryptDark = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cifradoDarkTheme.png")), 36, 36, true, true);
    private final Image icoTimerDark = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/timerDarkTheme.png")), 36, 36, true, true);
    private final Image icoAttachDark = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/añadirDarkTheme.png")), 36, 36, true, true);


    private ImageView profileIconView;
    private ImageView settingsIconView;
    private ImageView newMsgIconView;
    private ImageView logoutIconView;
    private ImageView logoView;

    /* para cambiar el icono de la papelera cuando alternamos tema */
    private final List<Button> trashButtons = new ArrayList<>();

    /* ======= ventana/escena ======= */
    private Stage stage;
    private Scene scene;
    private double xOffset, yOffset;          // drag-window
    private boolean encryptNew = false;
    private String timerSelection = "";

    public MainInboxWindow(String currentUser, ConfigurableApplicationContext springCtx) {
        this.currentUser = currentUser;
        this.springCtx = springCtx;
        this.darkTheme = "dark".equals(ThemeManager.getInstance().getTheme());
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        try {
            Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/logo.png")));
            stage.getIcons().clear();
            stage.getIcons().add(appIcon);
        } catch (Exception e) {
            // Manejo de error
        }

        /* idioma por defecto */
        LocaleManager.localeProperty().addListener((o, oldL, newL) -> {
            refreshTexts();
            loadMessages(); // <--- Recarga los mensajes y los decodifica con el nuevo idioma
        });
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
        actualizarTemaUI();
        ThemeManager tm = ThemeManager.getInstance();
        scene.getStylesheets().setAll(tm.getCss());
        tm.themeProperty().addListener((o, oldT, n) -> scene.getStylesheets().setAll(tm.getCss()));

        /* listener de idioma */
        LocaleManager.localeProperty().addListener((o, oldL, newL) -> refreshTexts());
        refreshTexts();            // 1ª vez

        stage.setScene(scene);
        stage.show();

        /* carga + refresco periódico */
        loadMessages();
        refresher = new Timeline(new KeyFrame(Duration.seconds(5), ev -> {
            refreshInbox();
            refreshSent(); // Agregar esta línea para refrescar también los mensajes enviados
        }));
        refresher.setCycleCount(Timeline.INDEFINITE);
        refresher.play();
    }

    private BorderPane buildUI() {
        /* ───────── Label Bienvenida ───────── */
        lblWelcome = new Label();
        lblWelcome.getStyleClass().add("welcome-label");

        /* ───────── Logo ───────── */
        logoView = new ImageView(darkTheme ? logoImgDark : logoImg);

        /* ───────── Botón Perfil ───────── */
        profileIconView = new ImageView(darkTheme ? userIconDark : userIconLight);
        Button btnPerfil = new Button(null, profileIconView);
        btnPerfil.getStyleClass().add("icon-button");
        btnPerfil.setOnAction(e -> {
            try {
                new ProfileWindow(currentUser, stage, springCtx).show();
            } catch (Exception ex) {
                pop.mostrarAlertaError(bundle().getString("common.error"), bundle().getString("profile.alert.error.openProfile"));
            }
        });

        /* ───────── Menú Ajustes (Idioma / Tema) ───────── */
        settingsIconView = new ImageView(darkTheme ? settingsIconDark : settingsIconLight);

        idiomaMenu = new Menu();
        idiomaMenu.getItems().addAll(new MenuItem(), new MenuItem(), new MenuItem(), new MenuItem(), new MenuItem(), new MenuItem(), new MenuItem(), new MenuItem());
        idiomaMenu.getItems().get(0).setOnAction(e -> LocaleManager.setLocale(new Locale("es", "ES")));
        idiomaMenu.getItems().get(1).setOnAction(e -> LocaleManager.setLocale(new Locale("ca", "ES")));
        idiomaMenu.getItems().get(2).setOnAction(e -> LocaleManager.setLocale(Locale.ENGLISH));
        idiomaMenu.getItems().get(3).setOnAction(e -> LocaleManager.setLocale(new Locale("fr", "FR")));
        idiomaMenu.getItems().get(4).setOnAction(e -> LocaleManager.setLocale(new Locale("it", "IT")));
        idiomaMenu.getItems().get(5).setOnAction(e -> LocaleManager.setLocale(new Locale("pt", "PT")));
        idiomaMenu.getItems().get(6).setOnAction(e -> LocaleManager.setLocale(new Locale("nl", "NL")));
        idiomaMenu.getItems().get(7).setOnAction(e -> LocaleManager.setLocale(new Locale("de", "DE")));

        temaMenu = new Menu();
        temaMenu.getItems().addAll(new MenuItem(), new MenuItem());
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
            pop.mostrarAlertaInformativa(bundle().getString("session.logout.title"), bundle().getString("session.logout.info"));
            try {
                new LoginWindow(springCtx).start(new Stage());
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.exit();
            }
        });

        idiomaMenu.getItems().get(0).setText(bundle().getString("menu.language.spanish"));
        idiomaMenu.getItems().get(1).setText(bundle().getString("menu.language.catalan"));
        idiomaMenu.getItems().get(2).setText(bundle().getString("menu.language.english"));
        idiomaMenu.getItems().get(3).setText(bundle().getString("menu.language.french")); // AÑADIR ESTA LÍNEA
        idiomaMenu.getItems().get(4).setText(bundle().getString("menu.language.italian")); // AÑADIR ESTA LÍNEA
        idiomaMenu.getItems().get(5).setText(bundle().getString("menu.language.portuguese")); // AÑADIR ESTA LÍNEA
        idiomaMenu.getItems().get(6).setText(bundle().getString("menu.language.dutch")); // AÑADIR ESTA LÍNEA
        idiomaMenu.getItems().get(7).setText(bundle().getString("menu.language.german")); // AÑADIR ESTA LÍNEA

        idiomaMenu.setText(bundle().getString("menu.language"));

        temaMenu.getItems().get(0).setText(bundle().getString("menu.theme.dark"));
        temaMenu.getItems().get(1).setText(bundle().getString("menu.theme.light"));
        temaMenu.setText(bundle().getString("menu.theme"));

        /* ───────── Top Bar ───────── */
        HBox leftBox = new HBox(10, logoView, lblWelcome);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        HBox rightBox = new HBox(2, btnNuevo, btnPerfil, btnSettings, btnLogout);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        BorderPane topBar = new BorderPane(leftBox, null, rightBox, null, null);
        topBar.setPadding(new Insets(10));
        topBar.getStyleClass().add("top-bar");

        /* ───────── TabPane + Wrapper ───────── */

        // >>> CAMBIO 2: Sustituir la creación original de tabs por un HBox con texto + badge <<<
        /*
        // Antes (eliminar o comentar):
        tabs = new TabPane(new Tab(bundle().getString("tab.inbox"), createTable(true)), new Tab(bundle().getString("tab.sent"), createTable(false)));
        tabs.getTabs().forEach(t -> t.setClosable(false));
        tabs.getStyleClass().add("inbox-tabs");
        */

        // 2.1 Creamos la tabla para la pestaña Inbox
        TableView<Mensaje> inboxTable = createTable(true);

        // 2.2 Construimos los Labels: uno para el texto, otro para el badge
        inboxTabTextLabel = new Label(bundle().getString("tab.inbox"));
        inboxBadgeLabel = new Label("0");                  // Empieza en “0”
        inboxBadgeLabel.getStyleClass().add("badge");      // Aplicamos la clase .badge
        inboxBadgeLabel.setVisible(false);                 // Oculto mientras no haya mensajes sin leer
        inboxBadgeLabel.setManaged(false);

        // 2.3 Empaquetamos ambos Labels dentro de un HBox
        HBox inboxGraphic = new HBox(4, inboxTabTextLabel, inboxBadgeLabel);
        inboxGraphic.setAlignment(Pos.CENTER_LEFT);

        // 2.4 Creamos la pestaña Inbox y le asignamos el HBox como graphic (texto vacío)
        Tab inboxTab = new Tab("", inboxTable);
        inboxTab.setGraphic(inboxGraphic);
        inboxTab.setClosable(false);

        // 2.5 Ahora creamos la pestaña “Sent” (solo con texto, sin badge)
        TableView<Mensaje> sentTable = createTable(false);
        Label sentTabTextLabel = new Label(bundle().getString("tab.sent"));
        HBox sentGraphic = new HBox(4, sentTabTextLabel);
        sentGraphic.setAlignment(Pos.CENTER_LEFT);
        Tab sentTab = new Tab("", sentTable);
        sentTab.setGraphic(sentGraphic);
        sentTab.setClosable(false);

        // 2.6 Finalmente, instanciamos el TabPane con ambas pestañas
        tabs = new TabPane(inboxTab, sentTab);
        tabs.getStyleClass().add("inbox-tabs");
        // <<< FIN CAMBIO 2 >>>

        // Refresca texto de pestañas al inicio
        refreshTexts();

        tabsWrapper = new StackPane(tabs);
        tabsWrapper.setAlignment(Pos.TOP_LEFT);

        /* ───────── Root + drag window ───────── */
        BorderPane root = new BorderPane(tabsWrapper);
        root.setTop(topBar);
        root.setPadding(new Insets(10));
        root.getStyleClass().add("main-root");

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


    private void refreshTexts() {
        ResourceBundle b = bundle();

        stage.setTitle(b.getString("inbox.window.title"));
        lblWelcome.setText(MessageFormat.format(b.getString("inbox.welcome"), currentUser));

        idiomaMenu.setText(b.getString("menu.language"));
        idiomaMenu.getItems().get(0).setText(b.getString("menu.language.spanish"));
        idiomaMenu.getItems().get(1).setText(b.getString("menu.language.catalan"));
        idiomaMenu.getItems().get(2).setText(b.getString("menu.language.english"));
        idiomaMenu.getItems().get(3).setText(b.getString("menu.language.french")); // AÑADIR ESTA LÍNEA
        idiomaMenu.getItems().get(4).setText(b.getString("menu.language.italian")); // AÑADIR ESTA LÍNEA
        idiomaMenu.getItems().get(5).setText(b.getString("menu.language.portuguese")); // AÑADIR ESTA LÍNEA
        idiomaMenu.getItems().get(6).setText(b.getString("menu.language.dutch")); // AÑADIR ESTA LÍNEA
        idiomaMenu.getItems().get(7).setText(b.getString("menu.language.german")); // AÑADIR ESTA LÍNEA

        temaMenu.setText(b.getString("menu.theme"));
        temaMenu.getItems().get(0).setText(b.getString("menu.theme.dark"));
        temaMenu.getItems().get(1).setText(b.getString("menu.theme.light"));

        // >>> CAMBIO 3: en lugar de tabs.getTabs().get(0).setText(...), actualizamos el Label <<<
        inboxTabTextLabel.setText(b.getString("tab.inbox"));

        // Para la pestaña "Sent", obtenemos el Label interno del graphic:
        HBox sentGraphic = (HBox) tabs.getTabs().get(1).getGraphic();
        Label sentTabTextLabel = (Label) sentGraphic.getChildren().get(0);
        sentTabTextLabel.setText(b.getString("tab.sent"));
        // <<< FIN CAMBIO 3 >>>

        updateTableTexts((TableView<?>) tabs.getTabs().get(0).getContent(), true);
        updateTableTexts((TableView<?>) tabs.getTabs().get(1).getContent(), false);

    }

    private void updateTableTexts(TableView<?> tv, boolean inbox) {
        ResourceBundle b = bundle();
        String partyKey = inbox ? "table.column.sender" : "table.column.recipient";
        tv.getColumns().get(0).setText(b.getString(partyKey));
        tv.getColumns().get(1).setText(b.getString("table.column.subject"));
        ((Label) tv.getPlaceholder()).setText(inbox ? b.getString("table.placeholder.inbox") : b.getString("table.placeholder.sent"));
    }


    private TableView<Mensaje> createTable(boolean inbox) {
        TableView<Mensaje> table = new TableView<>(inbox ? MessageStore.inboxMessages : MessageStore.sentMessages);
        table.getStyleClass().add("bandeja-tabla");

        String partyKey = inbox ? "table.column.sender" : "table.column.recipient";
        TableColumn<Mensaje, String> colParty = new TableColumn<>(bundle().getString(partyKey));
        colParty.setCellValueFactory(new PropertyValueFactory<>("sender"));
        colParty.setPrefWidth(150);

        TableColumn<Mensaje, String> colSubject = new TableColumn<>(bundle().getString("table.column.subject"));
        colSubject.setCellValueFactory(new PropertyValueFactory<>("asunto"));
        colSubject.setPrefWidth(580);

        TableColumn<Mensaje, Void> colDel = new TableColumn<>("");
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

            @Override
            protected void updateItem(Void it, boolean empty) {
                super.updateItem(it, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(colParty, colSubject, colDel);
        table.setPlaceholder(new Label(inbox ? bundle().getString("table.placeholder.inbox") : bundle().getString("table.placeholder.sent")));
        table.setRowFactory(tv -> {
            TableRow<Mensaje> row = new TableRow<>();

            row.itemProperty().addListener((obs, oldMsg, newMsg) -> {
                // 1) quitamos siempre la clase…
                row.getStyleClass().remove("read-row");
                // 2) …y sólo si hay mensaje Y está leído, la volvemos a añadir
                if (newMsg != null && newMsg.isRead()) {
                    row.getStyleClass().add("read-row");
                }
            });

            row.setOnMouseClicked(ev -> {
                if (!row.isEmpty() && ev.getClickCount() == 2) {
                    Mensaje msg = row.getItem();
                    markMessageReadOnServer(msg.getId());
                    new ChatWindow(currentUser, msg).show();
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
        javafx.concurrent.Task<List<MensajeDTO>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<MensajeDTO> call() throws Exception {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/messages/inbox/" + currentUser)).GET().build();
                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                return mapper.readValue(res.body(), new com.fasterxml.jackson.core.type.TypeReference<>() {
                });
            }
        };

        task.setOnSucceeded(evt -> {
            List<Mensaje> mensajes = task.getValue().stream().map(this::mapInbox).sorted((m1, m2) -> m2.getFechaHora().compareTo(m1.getFechaHora())) // Orden inverso por fecha
                    .toList();

            Platform.runLater(() -> MessageStore.inboxMessages.setAll(mensajes));
            long unreadCount = mensajes.stream().filter(m -> !m.isRead()).count();
            updateInboxTabBadge(unreadCount);
        });

        task.setOnFailed(evt -> {
            // opcional: log.error("Error refrescando inbox", task.getException());
        });
        MainApp.Background.POOL.submit(task);
    }

    private void refreshSent() {
        Task<List<MensajeDTO>> task = new Task<>() {
            @Override
            protected List<MensajeDTO> call() throws Exception {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/messages/sent/" + currentUser)).GET().build();
                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                return mapper.readValue(res.body(), new TypeReference<>() {
                });
            }
        };

        task.setOnSucceeded(evt -> {
            List<MensajeDTO> sent = task.getValue();
            List<Mensaje> mensajes = sent.stream().map(this::mapSent).sorted((m1, m2) -> m2.getFechaHora().compareTo(m1.getFechaHora())) // Orden inverso por fecha
                    .toList();

            // actualizar en el hilo FX
            Platform.runLater(() -> MessageStore.sentMessages.setAll(mensajes));
        });

        task.setOnFailed(evt -> {
            // opcional: log.error("Error refrescando sent", task.getException());
        });
        MainApp.Background.POOL.submit(task);
    }


    private Mensaje mapInbox(MensajeDTO dto) {
        String plain = decodeBody(dto);
        Mensaje m = new Mensaje(dto.getId(), dto.getRemitente(), dto.getAsunto(), plain, dto.getAdjuntos(), dto.getFechaHora());
        m.setRead(dto.isRead());
        return m;
    }

    private Mensaje mapSent(MensajeDTO dto) {
        String plain = decodeBody(dto);
        Mensaje m = new Mensaje(dto.getId(), dto.getDestinatario(), dto.getAsunto(), plain, dto.getAdjuntos(), dto.getFechaHora());
        m.setRead(dto.isRead());
        return m;
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

    /**
     * Muestra la ventana “Redactar mensaje”.
     * • No es modal → el usuario puede cambiar idioma/tema mientras está abierta.
     * • Refresca textos e iconos en caliente.
     */
    /**
     * Muestra la ventana “Redactar mensaje” con:
     * - Traducción en caliente de todos los textos
     * - Estado persistente de cifrado, temporizador y adjuntos
     * - Resaltado de iconos sólo cuando corresponde
     */
    private void showSendDialog() {
        // Estado inicial
        encryptNew = false;
        List<File> newSelectedFiled = new ArrayList<>();

        // FileChooser para adjuntos
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(bundle().getString("chat.attach.select"));

        // Crear diálogo
        Stage dlg = new Stage();
        dlg.initOwner(stage);
        dlg.initModality(Modality.NONE);
        try {
            Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/logo.png")));
            dlg.getIcons().setAll(appIcon);
        } catch (Exception ignored) {
        }

        // Campos de mensaje
        TextField txtTo = new TextField();
        TextField txtSubj = new TextField();
        TextArea txtBody = new TextArea();
        txtBody.getStyleClass().add("chat-textarea");
        txtBody.setWrapText(true);
        txtBody.setPrefRowCount(10);

        // Header
        VBox header = new VBox(8, txtTo, txtSubj);
        header.setPadding(new Insets(0, 0, 10, 0));
        VBox.setVgrow(txtBody, Priority.ALWAYS);

        // Botón cifrado + label
        Button btnEncrypt = new Button(null, new ImageView(darkTheme ? icoEncryptDark : icoEncrypt));
        btnEncrypt.getStyleClass().add("icon-button");
        Label lblEncrypt = new Label();
        lblEncrypt.getStyleClass().add("tool-label");

        // Menú temporizador + label
        MenuButton mbTimer = new MenuButton(null, new ImageView(darkTheme ? icoTimerDark : icoTimer));
        mbTimer.getStyleClass().add("icon-button");
        Label lblTimer = new Label();
        lblTimer.getStyleClass().add("tool-label");

        // Botón adjuntos + label
        Button btnAttach = new Button(null, new ImageView(darkTheme ? icoAttachDark : icoAttach));
        btnAttach.getStyleClass().add("icon-button");
        Label lblAttach = new Label();
        lblAttach.getStyleClass().add("tool-label");

        // Textos por defecto (se retraducirán en caliente)
        lblEncrypt.setText(bundle().getString("chat.encrypt.off"));
        lblTimer.setText(bundle().getString("chat.timer.off"));
        lblAttach.setText(bundle().getString("chat.attach.off"));

        // Handler cifrado
        btnEncrypt.setOnAction(e -> {
            encryptNew = !encryptNew;
            lblEncrypt.setText(bundle().getString(encryptNew ? "chat.encrypt.on" : "chat.encrypt.off"));
            updateSendIcons(btnEncrypt, mbTimer, btnAttach, lblTimer, newSelectedFiled);
        });

        // Handler temporizador: OFF
        MenuItem miTimerOff = new MenuItem(bundle().getString("chat.timer.off"));
        miTimerOff.setOnAction(e -> {
            timerSelection = "";
            lblTimer.setText(bundle().getString("chat.timer.off"));
            updateSendIcons(btnEncrypt, mbTimer, btnAttach, lblTimer, newSelectedFiled);
        });
        mbTimer.getItems().add(miTimerOff);

        // Opciones de temporizador
        for (String t : new String[]{"30 s", "1 min", "5 min", "30 min"}) {
            MenuItem mi = new MenuItem(t);
            mi.setOnAction(evt -> {
                timerSelection = t;
                lblTimer.setText(t);
                updateSendIcons(btnEncrypt, mbTimer, btnAttach, lblTimer, newSelectedFiled);
            });
            mbTimer.getItems().add(mi);
        }

        // Handler adjuntos
        btnAttach.setOnAction(e -> {
            List<File> files = fileChooser.showOpenMultipleDialog(dlg);
            if (files != null && !files.isEmpty()) {
                newSelectedFiled.clear();
                newSelectedFiled.addAll(files);
                pop.mostrarAlertaInformativa(bundle().getString("common.info"), MessageFormat.format(bundle().getString("chat.attach.added"), files.size()));
                lblAttach.setText(MessageFormat.format(bundle().getString("chat.attach.added"), newSelectedFiled.size()));
            }
            updateSendIcons(btnEncrypt, mbTimer, btnAttach, lblTimer, newSelectedFiled);
        });

        // GridTool
        GridPane tools = new GridPane();
        tools.setHgap(14);
        tools.setVgap(2);
        tools.setAlignment(Pos.TOP_LEFT);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHalignment(HPos.CENTER);
            tools.getColumnConstraints().add(cc);
        }
        tools.add(btnEncrypt, 0, 0);
        tools.add(mbTimer, 1, 0);
        tools.add(btnAttach, 2, 0);
        tools.add(lblEncrypt, 0, 1);
        tools.add(lblTimer, 1, 1);
        tools.add(lblAttach, 2, 1);

        // Botones enviar / cancelar
        Button btnSend = new Button(bundle().getString("dialog.newMessage.button.send"));
        Button btnCanc = new Button(bundle().getString("dialog.newMessage.button.cancel"));
        btnCanc.setOnAction(e -> dlg.close());

        // Acción enviar
        btnSend.setOnAction(e -> {
            ResourceBundle b = bundle();
            String dest = txtTo.getText().trim();
            String subj = txtSubj.getText().trim();
            String plain = txtBody.getText().trim();

            if (dest.isBlank() || plain.isBlank()) {
                pop.mostrarAlertaError(b.getString("common.error"), b.getString("dialog.newMessage.error.incomplete"));
                return;
            }

            // Construcción y envío del DTO en background
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    // 1) Obtener clave pública
                    HttpRequest pkReq = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/" + dest + "/publicKey")).GET().build();
                    HttpResponse<String> pkRes = http.send(pkReq, HttpResponse.BodyHandlers.ofString());

                    if (pkRes.statusCode() == 404) {
                        Platform.runLater(() -> pop.mostrarAlertaError(b.getString("common.error"), MessageFormat.format(b.getString("dialog.newMessage.error.unknownUser"), dest)));
                        return null;
                    }
                    if (pkRes.statusCode() != 200) {
                        Platform.runLater(() -> pop.mostrarAlertaError(b.getString("common.error"), b.getString("dialog.newMessage.error.server")));
                        return null;
                    }

                    // 2) Crear DTO
                    MensajeDTO dto = new MensajeDTO();
                    dto.setRemitente(currentUser);
                    dto.setDestinatario(dest);
                    dto.setAsunto(subj);

                    if (!timerSelection.isEmpty()) {
                        dto.setExpiryDate(calculateExpiry(timerSelection));
                    }

                    if (encryptNew) {
                        PublicKey pkDest = RSAUtils.publicKeyFromBase64(pkRes.body());
                        var p = HybridCrypto.encrypt(plain, pkDest);
                        dto.setCipherTextBase64(p.cipherB64());
                        dto.setEncKeyBase64(p.encKeyB64());
                        dto.setIvBase64(p.ivB64());
                    } else {
                        dto.setCipherTextBase64(Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8)));
                        dto.setEncKeyBase64(null);
                        dto.setIvBase64(null);
                    }

                    // Adjuntos
                    List<AdjuntoDTO> adjDTOs = new ArrayList<>();
                    for (File file : newSelectedFiled) {
                        byte[] bytes = Files.readAllBytes(file.toPath());
                        String mime = Files.probeContentType(file.toPath());
                        if (encryptNew) {
                            PublicKey pkDest = RSAUtils.publicKeyFromBase64(pkRes.body());
                            String b64 = Base64.getEncoder().encodeToString(bytes);
                            var p = HybridCrypto.encrypt(b64, pkDest);
                            adjDTOs.add(new AdjuntoDTO(file.getName(), mime, p.cipherB64(), p.encKeyB64(), p.ivB64()));
                        } else {
                            adjDTOs.add(new AdjuntoDTO(file.getName(), mime, Base64.getEncoder().encodeToString(bytes), null, null));
                        }
                    }
                    dto.setAdjuntos(adjDTOs);

                    // 3) Envío HTTP
                    String json = mapper.writeValueAsString(dto);
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/messages/send")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();
                    http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(res -> Platform.runLater(() -> {
                        if (res.statusCode() == 200) {
                            refreshSent();
                            dlg.close();
                            pop.mostrarAlertaInformativa(b.getString("common.success"), b.getString("dialog.newMessage.info.sent"));
                        } else {
                            pop.mostrarAlertaError(b.getString("common.error"), b.getString("dialog.newMessage.error.send"));
                        }
                    }));

                    return null;
                }
            };
            MainApp.Background.POOL.submit(task);
        });

        // Layout
        Region stretch = new Region();
        HBox.setHgrow(stretch, Priority.ALWAYS);
        HBox bottom = new HBox(10, tools, stretch, btnSend, btnCanc);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(10, 0, 0, 0));

        BorderPane root = new BorderPane(txtBody);
        root.setTop(header);
        root.setBottom(bottom);
        root.setPadding(new Insets(20));

        Scene sc = new Scene(root, 560, 460);
        sc.getStylesheets().setAll(ThemeManager.getInstance().getCss());

        // Tema en caliente
        ThemeManager.getInstance().themeProperty().addListener((o, oldT, newT) -> {
            sc.getStylesheets().setAll(ThemeManager.getInstance().getCss());
            darkTheme = "dark".equals(newT);
            updateSendIcons(btnEncrypt, mbTimer, btnAttach, lblTimer, newSelectedFiled);
        });

        // Textos en caliente
        Runnable refreshTexts = () -> {
            ResourceBundle b = bundle();
            dlg.setTitle(b.getString("dialog.newMessage.title"));
            txtTo.setPromptText(b.getString("dialog.newMessage.field.to"));
            txtSubj.setPromptText(b.getString("dialog.newMessage.field.subject"));
            txtBody.setPromptText(b.getString("dialog.newMessage.field.body"));
            btnSend.setText(b.getString("dialog.newMessage.button.send"));
            btnCanc.setText(b.getString("dialog.newMessage.button.cancel"));
            lblEncrypt.setText(b.getString(encryptNew ? "chat.encrypt.on" : "chat.encrypt.off"));
            miTimerOff.setText(b.getString("chat.timer.off"));
            lblTimer.setText(timerSelection.isEmpty() ? b.getString("chat.timer.off") : timerSelection);
            lblAttach.setText(newSelectedFiled.isEmpty() ? b.getString("chat.attach.off") : MessageFormat.format(b.getString("chat.attach.added"), newSelectedFiled.size()));
        };
        refreshTexts.run();
        ChangeListener<Locale> locL = (o, oldL, newL) -> refreshTexts.run();
        LocaleManager.localeProperty().addListener(locL);
        dlg.setOnHidden(e -> LocaleManager.localeProperty().removeListener(locL));

        // Inicializar iconos
        updateSendIcons(btnEncrypt, mbTimer, btnAttach, lblTimer, newSelectedFiled);

        dlg.setScene(sc);
        dlg.show();
    }

    // -------------------------------------------------------------
    //  Descifra o simplemente decodifica Base64 según corresponda
    // -------------------------------------------------------------
    private String decodeBody(MensajeDTO dto) {
        ResourceBundle b = bundle();
        try {
            if (dto.getEncKeyBase64() == null || dto.getEncKeyBase64().isBlank()) {
                /* Mensaje en claro → solo Base64-decode */
                return new String(Base64.getDecoder().decode(dto.getCipherTextBase64()), StandardCharsets.UTF_8);
            }
            /* Mensaje cifrado híbrido */
            return HybridCrypto.decrypt(new HybridCrypto.HybridPayload(dto.getCipherTextBase64(), dto.getEncKeyBase64(), dto.getIvBase64()), KeyManager.getPrivateKey());
        } catch (Exception ex) {
            return b.getString("chat.alert.error.encrypt.reply");
        }
    }

    private void updateSendIcons(Button btnEncrypt, MenuButton mbTimer, Button btnAttach, Label lblTimer, List<File> newSelectedFiled) {
        ((ImageView) btnEncrypt.getGraphic()).setImage(encryptNew ? icoEncryptOn : (darkTheme ? icoEncryptDark : icoEncrypt));

        ResourceBundle b = bundle();
        boolean timerActivo = !lblTimer.getText().equals(b.getString("chat.timer.off"));
        ((ImageView) mbTimer.getGraphic()).setImage(timerActivo ? icoTimerOn : (darkTheme ? icoTimerDark : icoTimer));

        ImageView ivAttach = (ImageView) btnAttach.getGraphic();
        if (!newSelectedFiled.isEmpty()) {
            ivAttach.setImage(icoAttachOn);
        } else {
            ivAttach.setImage(darkTheme ? icoAttachDark : icoAttach);
        }
    }

    private static ResourceBundle bundle() {
        return LocaleManager.bundle();
    }

    private void setDarkTheme() {
        ThemeManager.getInstance().setTheme("dark", currentUser);
        actualizarTemaUI();
    }

    private void setLightTheme() {
        ThemeManager.getInstance().setTheme("light", currentUser);
        actualizarTemaUI();
    }

    private void actualizarTemaUI() {
        boolean isDark = ThemeManager.getInstance().getTheme().equals("dark");

        // Actualizar iconos
        profileIconView.setImage(isDark ? userIconDark : userIconLight);
        settingsIconView.setImage(isDark ? settingsIconDark : settingsIconLight);
        newMsgIconView.setImage(isDark ? newMsgIconDark : newMsgIconLight);
        logoutIconView.setImage(isDark ? logoutIconDark : logoutIconLight);
        logoView.setImage(isDark ? logoImgDark : logoImg);

        // Actualizar botones de papelera
        trashButtons.forEach(b -> b.setGraphic(new ImageView(isDark ? trashIconDark : trashIconLight)));

        // Actualizar CSS
        scene.getStylesheets().setAll(ThemeManager.getInstance().getCss());
    }

    private void markMessageReadOnServer(Long id) {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/messages/" + id + "/mark-read")).method("PATCH", HttpRequest.BodyPublishers.noBody()).build();
        http.sendAsync(req, HttpResponse.BodyHandlers.discarding()).thenRun(this::refreshInbox);  // refresca la bandeja al confirmar
    }

    private void updateInboxTabBadge(long count) {
        if (count <= 0) {
            inboxBadgeLabel.setVisible(false);
            inboxBadgeLabel.setManaged(false);
        } else {
            inboxBadgeLabel.setText(String.valueOf(count));
            inboxBadgeLabel.setVisible(true);
            inboxBadgeLabel.setManaged(true);
        }
    }

    private LocalDateTime calculateExpiry(String selection) {
        return switch (selection) {
            case "30 s" -> LocalDateTime.now().plusSeconds(30);
            case "1 min" -> LocalDateTime.now().plusMinutes(1);
            case "5 min" -> LocalDateTime.now().plusMinutes(5);
            case "30 min" -> LocalDateTime.now().plusMinutes(30);
            default -> null;
        };
    }

}
