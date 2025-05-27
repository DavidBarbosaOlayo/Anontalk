package windows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import managers.PopUpInfo;
import managers.mensajes.Mensaje;
import managers.mensajes.MensajeDTO;
import managers.mensajes.MessageStore;
import security.encryption.HybridCrypto;
import security.encryption.RSAUtils;
import utils.LocaleManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.ResourceBundle;

public class ChatWindow {

    /* ---------------- inyección ---------------- */
    private final String currentUser;
    private final Mensaje mensaje;
    private final PopUpInfo pop = new PopUpInfo();

    /* ---------------- servicios ---------------- */
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /* ---------------- UI dinámico ---------------- */
    private Stage stage;
    private Label lblDateKey, lblDateValue;
    private Label lblSenderKey, lblSenderValue;
    private Label lblSubjectKey, lblSubjectValue;
    private TextArea txtReply;
    private Label lblEncryptState;
    private MenuButton mbTimer;
    private Button btnEncrypt, btnAttach, btnSend, btnClose;

    /* ---------------- iconos ---------------- */
    private final Image icoEncrypt = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cifrado.png")), 36, 36, true, true);
    private final Image icoTimer = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/timer.png")), 36, 36, true, true);
    private final Image icoAttach = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/añadir.png")), 36, 36, true, true);
    private final Image icoEncryptOn = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cifrado1.png")), 36, 36, true, true);
    private final Image icoTimerOn = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/timer1.png")), 36, 36, true, true);

    /* =================================================================================== */
    public ChatWindow(String currentUser, Mensaje mensaje) {
        this.currentUser = currentUser;
        this.mensaje = mensaje;
    }

    /* =================================================================================== */
    /*                                        UI                                          */
    /* =================================================================================== */
    public void show() {
        stage = new Stage();

        /* ───────── CABECERA ───────── */
        lblDateKey = new Label();
        lblDateValue = new Label();
        lblSenderKey = new Label();
        lblSenderValue = new Label();
        lblSubjectKey = new Label();
        lblSubjectValue = new Label();

        // estilos
        lblDateKey.getStyleClass().add("chat-header-date");
        lblDateValue.getStyleClass().addAll("chat-header-date", "chat-header-value");
        lblSenderKey.getStyleClass().add("chat-header-line");
        lblSenderValue.getStyleClass().addAll("chat-header-line", "chat-header-value");
        lblSubjectKey.getStyleClass().add("chat-header-line");
        lblSubjectValue.getStyleClass().addAll("chat-header-line", "chat-header-value");

        HBox dateBox = new HBox(4, lblDateKey, lblDateValue);
        HBox senderBox = new HBox(4, lblSenderKey, lblSenderValue);
        HBox subjectBox = new HBox(4, lblSubjectKey, lblSubjectValue);

        VBox headerBox = new VBox(2, dateBox, senderBox, subjectBox);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 10, 0));

        /* ───────── MENSAJE RECIBIDO ───────── */
        Label lblBody = new Label(mensaje.getContent());
        lblBody.setWrapText(true);
        lblBody.getStyleClass().add("chat-message-body");

        ScrollPane scroll = new ScrollPane(lblBody);
        scroll.setFitToWidth(true);

        /* ───────── ICONOS / HERRAMIENTAS ───────── */
        btnEncrypt = new Button(null, new ImageView(icoEncrypt));
        btnEncrypt.getStyleClass().add("icon-button");
        lblEncryptState = new Label("Sin cifrar");
        lblEncryptState.getStyleClass().add("tool-label");
        btnEncrypt.setOnAction(e -> {
            boolean on = "Sin cifrar".equals(lblEncryptState.getText());
            ((ImageView) btnEncrypt.getGraphic()).setImage(on ? icoEncryptOn : icoEncrypt);
            lblEncryptState.setText(on ? "Cifrado" : "Sin cifrar");
        });

        mbTimer = new MenuButton(null, new ImageView(icoTimer));
        mbTimer.getStyleClass().add("icon-button");
        Label lblTimerState = new Label();
        lblTimerState.getStyleClass().add("tool-label");

        for (String o : new String[]{"Sin tiempo", "30 s", "1 min", "5 min", "30 min"}) {
            MenuItem it = new MenuItem(o);
            it.setOnAction(ev -> {
                if ("Sin tiempo".equals(o)) {
                    lblTimerState.setText("");
                    ((ImageView) mbTimer.getGraphic()).setImage(icoTimer);
                } else {
                    lblTimerState.setText(o);
                    ((ImageView) mbTimer.getGraphic()).setImage(icoTimerOn);
                }
            });
            mbTimer.getItems().add(it);
        }

        btnAttach = new Button(null, new ImageView(icoAttach));
        btnAttach.getStyleClass().add("icon-button");

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
        tools.add(lblEncryptState, 0, 1);
        tools.add(lblTimerState, 1, 1);

        /* ───────── REDACTOR ───────── */
        txtReply = new TextArea();
        txtReply.setPrefRowCount(4);
        txtReply.getStyleClass().add("chat-textarea");

        VBox compose = new VBox(6, txtReply);
        compose.setPadding(new Insets(10));

        /* ───────── BOTONES INFERIORES ───────── */
        btnSend = new Button();
        btnClose = new Button();
        btnSend.setOnAction(e -> sendReply(txtReply.getText().trim()));
        btnClose.setOnAction(e -> stage.close());

        Region stretch = new Region();
        HBox.setHgrow(stretch, Priority.ALWAYS);

        HBox bottomBar = new HBox(10, tools, stretch, btnSend, btnClose);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10));

        /* ───────── ROOT / ESCENA ───────── */
        BorderPane root = new BorderPane(scroll, headerBox, null, new VBox(compose, bottomBar), null);
        root.setPadding(new Insets(10));
        root.getStyleClass().add("chat-root");

        Scene scene = new Scene(root, 700, 500);
        ThemeManager tm = ThemeManager.getInstance();
        scene.getStylesheets().setAll(tm.getCss());
        tm.themeProperty().addListener((o, oldT, n) -> scene.getStylesheets().setAll(tm.getCss()));

        LocaleManager.localeProperty().addListener((o, oldL, n) -> refreshTexts());
        refreshTexts();

        stage.setScene(scene);
        stage.show();
    }

    /* =================================================================================== */
    /*                              REFRESCO DE TEXTOS                                    */
    /* =================================================================================== */
    private void refreshTexts() {
        ResourceBundle b = LocaleManager.bundle();

        stage.setTitle(MessageFormat.format(b.getString("chat.window.title"), mensaje.getSender()));

        lblDateKey.setText(b.getString("chat.header.date"));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        lblDateValue.setText(LocalDateTime.now().format(fmt));

        lblSenderKey.setText(b.getString("chat.header.from"));
        lblSenderValue.setText(mensaje.getSender());

        lblSubjectKey.setText(b.getString("chat.header.subject"));
        lblSubjectValue.setText(mensaje.getAsunto());

        txtReply.setPromptText(b.getString("chat.prompt.reply"));
        btnSend.setText(b.getString("chat.button.send"));
        btnClose.setText(b.getString("chat.button.close"));
    }

    /* =================================================================================== */
    /*                               ENVÍO DE RESPUESTA                                   */
    /* =================================================================================== */
    private void sendReply(String plainText) {
        ResourceBundle b = LocaleManager.bundle();

        if (plainText.isEmpty()) {
            pop.mostrarAlertaError(b.getString("common.error"), b.getString("chat.alert.error.emptyMessage"));
            return;
        }
        String destinatario = mensaje.getSender();

        try {
            HttpRequest pkReq = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/" + destinatario + "/publicKey")).GET().build();
            String pubB64 = http.send(pkReq, HttpResponse.BodyHandlers.ofString()).body();
            PublicKey destPk = RSAUtils.publicKeyFromBase64(pubB64);

            HybridCrypto.HybridPayload p = HybridCrypto.encrypt(plainText, destPk);

            MensajeDTO dto = new MensajeDTO();
            dto.setRemitente(currentUser);
            dto.setDestinatario(destinatario);
            dto.setAsunto(mensaje.getAsunto());
            dto.setCipherTextBase64(p.cipherB64());
            dto.setEncKeyBase64(p.encKeyB64());
            dto.setIvBase64(p.ivB64());

            String json = mapper.writeValueAsString(dto);

            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/messages/send")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();

            http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(resp -> {
                if (resp.statusCode() == 200) {
                    try {
                        MensajeDTO saved = mapper.readValue(resp.body(), MensajeDTO.class);
                        Platform.runLater(() -> {
                            MessageStore.sentMessages.add(new Mensaje(saved.getId(), saved.getDestinatario(), saved.getAsunto(), plainText));
                            pop.mostrarAlertaInformativa(b.getString("common.success"), b.getString("chat.alert.info.sent"));
                            stage.close();
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> pop.mostrarAlertaError(b.getString("common.error"), b.getString("chat.alert.error.serverResponse")));
                    }
                } else {
                    Platform.runLater(() -> pop.mostrarAlertaError(b.getString("common.error"), b.getString("chat.alert.error.save")));
                }
            });

        } catch (Exception ex) {
            pop.mostrarAlertaError(b.getString("common.error"), b.getString("chat.alert.error.encrypt"));
        }
    }
}
