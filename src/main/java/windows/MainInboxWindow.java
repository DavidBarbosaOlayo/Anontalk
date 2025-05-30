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
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
        idiomaMenu.getItems().addAll(new MenuItem(), new MenuItem(), new MenuItem());
        idiomaMenu.getItems().get(0).setOnAction(e -> LocaleManager.setLocale(new Locale("es", "ES")));
        idiomaMenu.getItems().get(1).setOnAction(e -> LocaleManager.setLocale(new Locale("ca", "ES")));
        idiomaMenu.getItems().get(2).setOnAction(e -> LocaleManager.setLocale(Locale.ENGLISH));

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

        idiomaMenu.getItems().get(0).setOnAction(e -> {
            LocaleManager.setLocale(new Locale("es", "ES"));
        });

        idiomaMenu.getItems().get(1).setOnAction(e -> {
            LocaleManager.setLocale(new Locale("ca", "ES"));
        });

        idiomaMenu.getItems().get(2).setOnAction(e -> {
            LocaleManager.setLocale(Locale.ENGLISH);
        });

        /* ───────── Top Bar ───────── */
        HBox leftBox = new HBox(10, logoView, lblWelcome);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        HBox rightBox = new HBox(2, btnNuevo, btnPerfil, btnSettings, btnLogout);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        BorderPane topBar = new BorderPane(leftBox, null, rightBox, null, null);
        topBar.setPadding(new Insets(10));
        topBar.getStyleClass().add("top-bar");

        /* ───────── TabPane ───────── */
        tabs = new TabPane(new Tab("", createTable(true)), new Tab("", createTable(false)));
        tabs.getTabs().forEach(t -> t.setClosable(false));
        tabs.getStyleClass().add("inbox-tabs");

        /* ───────── Root + drag window ───────── */
        BorderPane root = new BorderPane(tabs);
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
    private void showSendDialog() {
        encryptNew = false;                      // estado inicial

        List<File> selectedFiles = new ArrayList<>();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(bundle().getString("chat.attach.select"));
        Stage dlg = new Stage();
        dlg.initOwner(stage);

        try {
            Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/logo.png")));
            dlg.getIcons().clear();
            dlg.getIcons().add(appIcon);
        } catch (Exception e) {
            // Manejo de error silencioso si no se puede cargar el icono
        }

        /* ---------- NODOS PRINCIPALES ---------- */
        TextField txtTo = new TextField();
        TextField txtSubj = new TextField();
        TextArea txtBody = new TextArea();
        txtBody.getStyleClass().add("chat-textarea");
        txtBody.setWrapText(true);
        txtBody.setPrefRowCount(10);
        VBox header = new VBox(8, txtTo, txtSubj);
        header.setPadding(new Insets(0, 0, 10, 0));
        VBox.setVgrow(txtBody, Priority.ALWAYS);

        /* ---------- ICONOS / TOOLS ---------- */
        Button btnEncrypt = new Button(null, new ImageView(darkTheme ? icoEncryptDark : icoEncrypt));
        btnEncrypt.getStyleClass().add("icon-button");
        Label lblEncrypt = new Label();
        lblEncrypt.getStyleClass().add("tool-label");

        MenuButton mbTimer = new MenuButton(null, new ImageView(darkTheme ? icoTimerDark : icoTimer));
        mbTimer.getStyleClass().add("icon-button");
        Label lblTimer = new Label();
        lblTimer.getStyleClass().add("tool-label");

        Button btnAttach = new Button(null, new ImageView(darkTheme ? icoAttachDark : icoAttach));
        btnAttach.getStyleClass().add("icon-button");
        btnAttach.setOnAction(e -> {
            List<File> files = fileChooser.showOpenMultipleDialog(dlg);
            if (files != null && !files.isEmpty()) {
                selectedFiles.clear();
                selectedFiles.addAll(files);
                pop.mostrarAlertaInformativa(bundle().getString("common.info"), MessageFormat.format(bundle().getString("chat.attach.added"), files.size()));
            }
            updateSendIcons(btnEncrypt, mbTimer, btnAttach, lblTimer, selectedFiles);
        });


        /* ---- Timer items ---- */
        MenuItem miTimerOff = new MenuItem();
        mbTimer.getItems().add(miTimerOff);
        for (String t : new String[]{"30 s", "1 min", "5 min", "30 min"}) {
            MenuItem mi = new MenuItem(t);
            mi.setOnAction(e -> {
                lblTimer.setText(t);
                updateSendIcons(btnEncrypt, mbTimer, btnAttach, lblTimer, selectedFiles);
            });
            mbTimer.getItems().add(mi);
        }

        /* ---- Encrypt toggle ---- */
        btnEncrypt.setOnAction(e -> {
            encryptNew = !encryptNew;
            lblEncrypt.setText(bundle().getString(encryptNew ? "chat.encrypt.on" : "chat.encrypt.off"));
            updateSendIcons(btnEncrypt, mbTimer, btnAttach, lblTimer, selectedFiles);
        });

        /* ---- Timer OFF ---- */
        miTimerOff.setOnAction(e -> {
            lblTimer.setText("");
            updateSendIcons(btnEncrypt, mbTimer, btnAttach, lblTimer, selectedFiles);
        });

        /* ---- Tools grid ---- */
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

        /* ---------- BOTONES ---------- */
        Button btnSend = new Button();
        Button btnCanc = new Button();
        btnCanc.setOnAction(e -> dlg.close());

        /* ========== ENVÍO ========== */
        btnSend.setOnAction(e -> {
            String dest = txtTo.getText().trim();
            String plain = txtBody.getText().trim();
            ResourceBundle b = bundle();

            if (dest.isBlank() || plain.isBlank()) {
                pop.mostrarAlertaError(b.getString("common.error"), b.getString("dialog.newMessage.error.incomplete"));
                return;
            }



            try {
                /* 1) clave pública destinatario */
                HttpRequest pkReq = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/" + dest + "/publicKey")).GET().build();
                HttpResponse<String> pkRes = http.send(pkReq, HttpResponse.BodyHandlers.ofString());

                if (pkRes.statusCode() == 404) {
                    pop.mostrarAlertaError(b.getString("common.error"), MessageFormat.format(b.getString("dialog.newMessage.error.unknownUser"), dest));
                    return;
                }
                if (pkRes.statusCode() != 200) {
                    pop.mostrarAlertaError(b.getString("common.error"), b.getString("dialog.newMessage.error.server"));
                    return;
                }

                /* 2) DTO */
                MensajeDTO dto = new MensajeDTO();
                dto.setRemitente(currentUser);
                dto.setDestinatario(dest);
                dto.setAsunto(txtSubj.getText().trim());

                String timerSelection = lblTimer.getText();
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

                // 2.1) Adjuntos
                List<AdjuntoDTO> adjuntosDto = new ArrayList<>();
                for (File file : selectedFiles) {
                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    String filename = file.getName();
                    String mimeType = Files.probeContentType(file.toPath());

                    String cipherB64, encKeyB64, ivB64;
                    if (encryptNew) {
                        // cifrar el contenido Base64 del fichero
                        PublicKey destPk = RSAUtils.publicKeyFromBase64(pkRes.body());
                        String fileBase64 = Base64.getEncoder().encodeToString(fileBytes);
                        HybridCrypto.HybridPayload p = HybridCrypto.encrypt(fileBase64, destPk);
                        cipherB64 = p.cipherB64();
                        encKeyB64 = p.encKeyB64();
                        ivB64 = p.ivB64();
                    } else {
                        cipherB64 = Base64.getEncoder().encodeToString(fileBytes);
                        encKeyB64 = null;
                        ivB64 = null;
                    }

                    adjuntosDto.add(new AdjuntoDTO(filename, mimeType, cipherB64, encKeyB64, ivB64));
                }
                dto.setAdjuntos(adjuntosDto);

                /* 3) POST */
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

            } catch (Exception ex) {
                pop.mostrarAlertaError(bundle().getString("common.error"), bundle().getString("dialog.newMessage.error.encrypt"));
            }
        });

        /* ---------- BARRA INFERIOR ---------- */
        Region stretch = new Region();
        HBox.setHgrow(stretch, Priority.ALWAYS);
        HBox bottom = new HBox(10, tools, stretch, btnSend, btnCanc);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(10, 0, 0, 0));

        /* ---------- ROOT / SCENE ---------- */
        BorderPane root = new BorderPane(txtBody);
        root.setTop(header);
        root.setBottom(bottom);
        root.setPadding(new Insets(20));

        Scene sc = new Scene(root, 560, 460);
        sc.getStylesheets().setAll(ThemeManager.getInstance().getCss());

        /* --- Tema en caliente --- */
        ThemeManager.getInstance().themeProperty().addListener((o, oldT, n) -> {
            sc.getStylesheets().setAll(ThemeManager.getInstance().getCss());
            darkTheme = "dark".equals(n);
            updateSendIcons(btnEncrypt, mbTimer, btnAttach, lblTimer, selectedFiles);
        });

        /* --- TEXTOS en caliente --- */
        Runnable refreshTexts = () -> {
            ResourceBundle b = bundle();
            dlg.setTitle(b.getString("dialog.newMessage.title"));
            txtTo.setPromptText(b.getString("dialog.newMessage.field.to"));
            txtSubj.setPromptText(b.getString("dialog.newMessage.field.subject"));
            txtBody.setPromptText(b.getString("dialog.newMessage.field.body"));
            lblEncrypt.setText(b.getString(encryptNew ? "chat.encrypt.on" : "chat.encrypt.off"));
            miTimerOff.setText(b.getString("chat.timer.off"));
            btnSend.setText(b.getString("dialog.newMessage.button.send"));
            btnCanc.setText(b.getString("dialog.newMessage.button.cancel"));
        };
        refreshTexts.run();

        ChangeListener<Locale> locL = (o, oldL, newL) -> refreshTexts.run();
        LocaleManager.localeProperty().addListener(locL);
        dlg.setOnHidden(e -> LocaleManager.localeProperty().removeListener(locL));

        /* --- Iconos iniciales --- */
        updateSendIcons(btnEncrypt, mbTimer, btnAttach, lblTimer, selectedFiles);

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

    private void updateSendIcons(Button btnEncrypt, MenuButton mbTimer, Button btnAttach, Label lblTimer, List<File> selectedFiles) {
        /* 🔒  Candado */
        ((ImageView) btnEncrypt.getGraphic()).setImage(encryptNew ? icoEncryptOn : (darkTheme ? icoEncryptDark : icoEncrypt));

        /* ⏳  Temporizador */
        boolean timerActivo = !lblTimer.getText().isBlank();
        ((ImageView) mbTimer.getGraphic()).setImage(timerActivo ? icoTimerOn : (darkTheme ? icoTimerDark : icoTimer));

        /* 📎  Adjuntar: rojo si hay archivos seleccionados */
        ImageView attachIcon = (ImageView) btnAttach.getGraphic();
        if (!selectedFiles.isEmpty()) {
            attachIcon.setImage(icoAttachOn); // Icono rojo
        } else {
            attachIcon.setImage(darkTheme ? icoAttachDark : icoAttach);
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
        Tab inboxTab = tabs.getTabs().get(0);
        String tabLabel = bundle().getString("tab.inbox");

        if (count > 0) {
            Label name = new Label(tabLabel);
            Label badge = new Label(String.valueOf(count));
            badge.getStyleClass().add("badge");

            StackPane sp = new StackPane(name, badge);
            // Texto anclado a la izquierda
            StackPane.setAlignment(name, Pos.CENTER_LEFT);
            // Badge arriba-derecha
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            // Lo “tira” un poco hacia arriba y a la derecha
            StackPane.setMargin(badge, new Insets(-4, -4, 0, 0));

            inboxTab.setGraphic(sp);
            inboxTab.setText(null);
        } else {
            inboxTab.setGraphic(null);
            inboxTab.setText(tabLabel);
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
