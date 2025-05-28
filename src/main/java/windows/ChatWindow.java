package windows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import managers.PopUpInfo;
import managers.mensajes.Mensaje;
import managers.mensajes.MensajeDTO;
import managers.mensajes.MessageStore;
import managers.mensajes.adjuntos.AdjuntoDTO;
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
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    private Label lblTimerState;
    private MenuButton mbTimer;
    private Button btnEncrypt, btnAttach, btnSend, btnClose, btnResponder;
    private MenuItem miTimerOff;
    private boolean encrypt = false;     // SIN cifrar predeterminado
    private List<File> selectedFiles = new ArrayList<>();


    /* ---------------- iconos ---------------- */
    private final Image icoEncrypt = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cifrado.png")), 36, 36, true, true);
    private final Image icoEncryptDark = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cifradoDarkTheme.png")), 36, 36, true, true);
    private final Image icoTimer = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/timer.png")), 36, 36, true, true);
    private final Image icoTimerDark = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/timerDarkTheme.png")), 36, 36, true, true);
    private final Image icoAttach = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/añadir.png")), 36, 36, true, true);
    private final Image icoAttachDark = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/añadirDarkTheme.png")), 36, 36, true, true);
    private final Image icoEncryptOn = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cifrado1.png")), 36, 36, true, true);
    private final Image icoTimerOn = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/timer1.png")), 36, 36, true, true);
    private final Image icoAttachOn = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/añadir1.png")), 36, 36, true, true);

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
        ResourceBundle b = LocaleManager.bundle();

        /* ───────── CABECERA ───────── */
        lblDateKey = new Label();
        lblDateValue = new Label();
        lblSenderKey = new Label();
        lblSenderValue = new Label();
        lblSubjectKey = new Label();
        lblSubjectValue = new Label();

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

        /* ─── SECCIÓN DE ADJUNTOS (recibidos) ───────────────────────── */
        VBox attachmentsSection = new VBox(4);
        if (mensaje.getAdjuntos() != null && !mensaje.getAdjuntos().isEmpty()) {
            Label lblAtt = new Label(b.getString("chat.header.attachments"));
            lblAtt.getStyleClass().add("chat-header-line");
            attachmentsSection.getChildren().add(lblAtt);

            for (AdjuntoDTO a : mensaje.getAdjuntos()) {
                Button btnFile = new Button(a.getFilename());
                btnFile.getStyleClass().add("tool-button");
                btnFile.setOnAction(evt -> downloadAttachment(a));
                attachmentsSection.getChildren().add(btnFile);
            }
        }

        /* ───────── MENSAJE RECIBIDO ───────── */
        Label lblBody = new Label(mensaje.getContent());
        lblBody.setWrapText(true);
        lblBody.getStyleClass().add("chat-message-body");
        ScrollPane scroll = new ScrollPane(lblBody);
        scroll.setFitToWidth(true);

        /* ========== ÁREA DE RESPUESTA (oculta) ========== */
        btnEncrypt = new Button(null, new ImageView(icoEncrypt));
        btnEncrypt.getStyleClass().add("icon-button");
        lblEncryptState = new Label(b.getString("chat.encrypt.off"));
        lblEncryptState.getStyleClass().add("tool-label");
        btnEncrypt.setOnAction(e -> {
            encrypt = !encrypt;
            lblEncryptState.setText(LocaleManager.bundle().getString(encrypt ? "chat.encrypt.on" : "chat.encrypt.off"));
            updateIcons();
        });

        mbTimer = new MenuButton(null, new ImageView(icoTimer));
        mbTimer.getStyleClass().add("icon-button");
        lblTimerState = new Label();
        lblTimerState.getStyleClass().add("tool-label");
        miTimerOff = new MenuItem(b.getString("chat.timer.off"));
        miTimerOff.setOnAction(ev -> {
            lblTimerState.setText("");
            updateIcons();
        });
        mbTimer.getItems().add(miTimerOff);
        for (String o : new String[]{"30 s", "1 min", "5 min", "30 min"}) {
            MenuItem it = new MenuItem(o);
            it.setOnAction(ev -> {
                lblTimerState.setText(o);
                updateIcons();
            });
            mbTimer.getItems().add(it);
        }

        btnAttach = new Button(null, new ImageView(icoAttach));
        btnAttach.getStyleClass().add("icon-button");
        // ¡Aquí está el manejador que faltaba!
        btnAttach.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(b.getString("chat.attach.select")); // añade esta clave en tus properties
            List<File> files = chooser.showOpenMultipleDialog(stage);
            if (files != null) {
                selectedFiles.addAll(files);
                pop.mostrarAlertaInformativa(b.getString("common.success"), b.getString("chat.attach.added").replace("{0}", String.valueOf(files.size())));
            }
        });

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

        txtReply = new TextArea();
        txtReply.setPrefRowCount(4);
        txtReply.getStyleClass().add("chat-textarea");
        VBox compose = new VBox(6, txtReply);

        btnSend = new Button();
        btnClose = new Button();
        btnSend.setOnAction(e -> sendReply(txtReply.getText().trim()));
        btnClose.setOnAction(e -> stage.close());

        Region stretch = new Region();
        HBox.setHgrow(stretch, Priority.ALWAYS);

        HBox bottomBar = new HBox(10, tools, stretch, btnSend, btnClose);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10));

        VBox replyBox = new VBox(compose, bottomBar);
        replyBox.setVisible(false);
        replyBox.setManaged(false);

        btnResponder = new Button(b.getString("chat.button.reply"));
        btnResponder.getStyleClass().add("primary-button");
        btnResponder.setOnAction(e -> {
            replyBox.setVisible(true);
            replyBox.setManaged(true);
            btnResponder.setVisible(false);
            btnResponder.setManaged(false);
            txtReply.requestFocus();
        });

        VBox bottomArea = new VBox(btnResponder, replyBox);
        bottomArea.setAlignment(Pos.CENTER_RIGHT);
        bottomArea.setSpacing(10);
        bottomArea.setPadding(new Insets(10, 0, 0, 0));

        VBox topContent = new VBox(headerBox, attachmentsSection);
        topContent.setPadding(new Insets(10, 0, 10, 0));

        BorderPane root = new BorderPane(scroll, topContent, null, bottomArea, null);
        root.setPadding(new Insets(10));
        root.getStyleClass().add("chat-root");

        Scene scene = new Scene(root, 700, 500);
        ThemeManager tm = ThemeManager.getInstance();
        scene.getStylesheets().setAll(tm.getCss());
        tm.themeProperty().addListener((o, oldT, n) -> {
            scene.getStylesheets().setAll(tm.getCss());
            updateIcons();
        });

        updateIcons();
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

        /* controles añadidos */
        lblEncryptState.setText(b.getString(encrypt ? "chat.encrypt.on" : "chat.encrypt.off"));
        miTimerOff.setText(b.getString("chat.timer.off"));
        btnResponder.setText(b.getString("chat.button.reply"));
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

        // 1) Creamos un Task que corre en background
        Task<MensajeDTO> buildTask = new Task<>() {
            @Override
            protected MensajeDTO call() throws Exception {
                String destinatario = mensaje.getSender();

                // Construir DTO
                MensajeDTO dto = new MensajeDTO();
                dto.setRemitente(currentUser);
                dto.setDestinatario(destinatario);
                dto.setAsunto(mensaje.getAsunto());

                // Texto (cifrado o claro)
                if (encrypt) {
                    PublicKey destPk = fetchDestPublicKey(destinatario);
                    var p = HybridCrypto.encrypt(plainText, destPk);
                    dto.setCipherTextBase64(p.cipherB64());
                    dto.setEncKeyBase64(p.encKeyB64());
                    dto.setIvBase64(p.ivB64());
                } else {
                    dto.setCipherTextBase64(Base64.getEncoder().encodeToString(plainText.getBytes(StandardCharsets.UTF_8)));
                    dto.setEncKeyBase64(null);
                    dto.setIvBase64(null);
                }

                // Adjuntos
                List<AdjuntoDTO> adjuntosDto = new ArrayList<>();
                for (File file : selectedFiles) {
                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    String filename = file.getName();
                    String mimeType = Files.probeContentType(file.toPath());

                    if (encrypt) {
                        PublicKey destPk = fetchDestPublicKey(destinatario);
                        String fileB64 = Base64.getEncoder().encodeToString(fileBytes);
                        var p = HybridCrypto.encrypt(fileB64, destPk);
                        adjuntosDto.add(new AdjuntoDTO(filename, mimeType, p.cipherB64(), p.encKeyB64(), p.ivB64()));
                    } else {
                        adjuntosDto.add(new AdjuntoDTO(
                                filename,
                                mimeType,
                                Base64.getEncoder().encodeToString(fileBytes),
                                null,
                                null
                        ));
                    }
                }
                dto.setAdjuntos(adjuntosDto);

                return dto;
            }
        };

        // 2) Cuando termine de construir el DTO, lo enviamos al servidor
        buildTask.setOnSucceeded(evt -> {
            MensajeDTO dto = buildTask.getValue();
            try {
                String json = mapper.writeValueAsString(dto);
                var request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/messages/send"))
                        .header("Content-Type","application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(resp -> {
                            if (resp.statusCode() == 200) {
                                // parsear respuesta y actualizar UI
                                Platform.runLater(() -> {
                                    try {
                                        MensajeDTO saved = mapper.readValue(resp.body(), MensajeDTO.class);
                                        MessageStore.sentMessages.add(
                                                new Mensaje(saved.getId(), saved.getDestinatario(), saved.getAsunto(), plainText)
                                        );
                                        pop.mostrarAlertaInformativa(b.getString("common.success"), b.getString("chat.alert.info.sent"));
                                        stage.close();
                                    } catch (Exception ex) {
                                        pop.mostrarAlertaError(b.getString("common.error"), b.getString("chat.alert.error.serverResponse"));
                                    }
                                });
                            } else {
                                Platform.runLater(() ->
                                        pop.mostrarAlertaError(b.getString("common.error"), b.getString("chat.alert.error.save"))
                                );
                            }
                        });

            } catch (Exception ex) {
                Platform.runLater(() ->
                        pop.mostrarAlertaError(b.getString("common.error"), b.getString("chat.alert.error.encrypt"))
                );
            }
        });

        buildTask.setOnFailed(evt ->
                Platform.runLater(() ->
                        pop.mostrarAlertaError(b.getString("common.error"), buildTask.getException().getMessage())
                )
        );

        // 3) Finalmente, lanzamos el Task en el pool de background
        MainApp.Background.POOL.submit(buildTask);
    }


    private void updateIcons() {
        /* Candado */
        ImageView ivLock = (ImageView) btnEncrypt.getGraphic();
        ivLock.setImage(encrypt ? icoEncryptOn : (isDark() ? icoEncryptDark : icoEncrypt));

        /* Temporizador */
        boolean timerActivo = !lblTimerState.getText().isBlank();
        ImageView ivTimer = (ImageView) mbTimer.getGraphic();
        ivTimer.setImage(timerActivo ? icoTimerOn : (isDark() ? icoTimerDark : icoTimer));

        /* Adjuntar (no tiene estado ON) */
        ImageView ivAttach = (ImageView) btnAttach.getGraphic();
        ivAttach.setImage(isDark() ? icoAttachDark : icoAttach);
    }

    private boolean isDark() {
        return "dark".equals(ThemeManager.getInstance().getTheme());
    }

    /**
     * Devuelve la clave pública RSA del destinatario
     */
    private PublicKey fetchDestPublicKey(String username) throws Exception {
        HttpRequest pkReq = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/" + username + "/publicKey")).GET().build();
        String pubB64 = http.send(pkReq, HttpResponse.BodyHandlers.ofString()).body();
        return RSAUtils.publicKeyFromBase64(pubB64);
    }

    private void downloadAttachment(AdjuntoDTO a) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar " + a.getFilename());
        chooser.setInitialFileName(a.getFilename());
        File dest = chooser.showSaveDialog(stage);
        if (dest == null) return;

        try {
            byte[] data;
            if (a.getEncKeyBase64() != null && !a.getEncKeyBase64().isBlank()) {
                // Descifrado híbrido AES-GCM + RSA
                HybridCrypto.HybridPayload p = new HybridCrypto.HybridPayload(a.getCipherTextBase64(), a.getEncKeyBase64(), a.getIvBase64());
                String base64 = HybridCrypto.decrypt(p, KeyManager.getPrivateKey());
                data = Base64.getDecoder().decode(base64);
            } else {
                // Solo Base64
                data = Base64.getDecoder().decode(a.getCipherTextBase64());
            }
            Files.write(dest.toPath(), data);
            pop.mostrarAlertaInformativa(LocaleManager.bundle().getString("chat.alert.info.downloaded"), dest.getAbsolutePath());
        } catch (Exception ex) {
            pop.mostrarAlertaError(LocaleManager.bundle().getString("common.error"), LocaleManager.bundle().getString("chat.alert.error.download"));
        }
    }
}
