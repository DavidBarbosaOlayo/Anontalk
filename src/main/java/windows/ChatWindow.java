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

    private FileChooser chooser;

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
    private Label lblAttachState;
    private String timerSelection = "";
    private MenuButton mbTimer;
    private Button btnEncrypt, btnAttach, btnSend, btnClose, btnResponder;
    private MenuItem miTimerOff;
    private boolean encrypt = false;     // SIN cifrar predeterminado
    private List<File> selectedFiles = new ArrayList<>();

    private VBox chatArea;
    private ScrollPane chatScrollPane;
    private VBox replyBox; // Declarada aquí para que sea accesible

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
        this.chooser = new FileChooser();
    }

    /* =================================================================================== */
    /*                                        UI                                          */
    /* =================================================================================== */
    public void show() {
        stage = new Stage();

        try {
            Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/logo.png")));
            stage.getIcons().add(appIcon);
        } catch (Exception e) {
            System.err.println("Error cargando icono: " + e.getMessage());
        }

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

        // Establecemos aquí mismo la fecha real del mensaje en el encabezado:
        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        lblDateKey.setText(b.getString("chat.header.date"));
        if (mensaje.getFechaHora() != null) {
            lblDateValue.setText(mensaje.getFechaHora().format(fmtFecha));
        } else {
            lblDateValue.setText(LocalDateTime.now().format(fmtFecha));
        }

        lblSenderKey.setText(b.getString("chat.header.from"));
        lblSenderValue.setText(mensaje.getSender());

        lblSubjectKey.setText(b.getString("chat.header.subject"));
        lblSubjectValue.setText(mensaje.getAsunto());

        HBox dateBox = new HBox(4, lblDateKey, lblDateValue);
        HBox senderBox = new HBox(4, lblSenderKey, lblSenderValue);
        HBox subjectBox = new HBox(4, lblSubjectKey, lblSubjectValue);

        VBox headerBox = new VBox(2, dateBox, senderBox, subjectBox);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 10, 0));

        /* ───────── ÁREA DE CHAT ───────── */
        chatArea = new VBox(10);
        chatArea.setPadding(new Insets(10));

        // Agregar el mensaje inicial (del remitente) usando la fecha/hora real:
        addMessageToChat(mensaje.getSender(), mensaje.getContent(), false, mensaje.getFechaHora());

        chatScrollPane = new ScrollPane(chatArea);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chatScrollPane.getStyleClass().add("chat-area");

        /* ─── SECCIÓN DE ADJUNTOS (recibidos) ───────────────────────── */
        VBox attachmentsSection = new VBox(4);

        String loadingText;
        try {
            loadingText = b.getString("common.info") + ": " + b.getString("chat.attachments.loading");
        } catch (MissingResourceException e) {
            loadingText = "Loading attachments...";
        }
        Label loading = new Label(loadingText);
        attachmentsSection.getChildren().add(loading);

        HttpRequest attReq = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/messages/" + mensaje.getId() + "/attachments")).GET().build();
        http.sendAsync(attReq, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body).thenApply(body -> {
            try {
                return mapper.readValue(body, new com.fasterxml.jackson.core.type.TypeReference<List<AdjuntoDTO>>() {
                });
            } catch (Exception e) {
                return List.<AdjuntoDTO>of();
            }
        }).thenAccept(adjList -> Platform.runLater(() -> {
            attachmentsSection.getChildren().clear();
            if (adjList.isEmpty()) {
                return;
            }

            Label lblAtt = new Label(b.getString("chat.header.attachments"));
            lblAtt.getStyleClass().add("chat-header-line");
            attachmentsSection.getChildren().add(lblAtt);

            for (AdjuntoDTO a : adjList) {
                Button btnFile = new Button(a.getFilename());
                btnFile.getStyleClass().add("tool-button");
                btnFile.setOnAction(evt -> downloadAttachment(a));
                attachmentsSection.getChildren().add(btnFile);
            }
        }));

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
        lblTimerState = new Label(b.getString("chat.timer.off"));
        lblTimerState.getStyleClass().add("tool-label");
        miTimerOff = new MenuItem(b.getString("chat.timer.off"));
        miTimerOff.setOnAction(ev -> {
            timerSelection = "";
            lblTimerState.setText(b.getString("chat.timer.off"));
            updateIcons();
        });
        mbTimer.getItems().add(miTimerOff);
        for (String o : new String[]{"30 s", "1 min", "5 min", "30 min"}) {
            MenuItem it = new MenuItem(o);
            it.setOnAction(ev -> {
                timerSelection = o;
                lblTimerState.setText(o);
                updateIcons();
            });
            mbTimer.getItems().add(it);
        }

        // --- Nuevo label para estado de adjuntos ---
        lblAttachState = new Label(b.getString("chat.attach.off"));
        lblAttachState.getStyleClass().add("tool-label");

        chooser.setTitle(b.getString("chat.attach.select"));
        btnAttach = new Button(null, new ImageView(icoAttach));
        btnAttach.getStyleClass().add("icon-button");
        btnAttach.setOnAction(e -> {
            List<File> files = chooser.showOpenMultipleDialog(stage);
            if (files != null && !files.isEmpty()) {
                selectedFiles.addAll(files);
                pop.mostrarAlertaInformativa(b.getString("common.info"), b.getString("chat.attach.added").replace("{0}", String.valueOf(files.size())));
                lblAttachState.setText(b.getString("chat.attach.added").replace("{0}", String.valueOf(selectedFiles.size())));
                updateIcons();
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
        tools.add(lblAttachState, 2, 1);

        txtReply = new TextArea();
        txtReply.setPrefRowCount(4);
        txtReply.getStyleClass().add("chat-textarea");
        VBox compose = new VBox(6, txtReply);

        btnSend = new Button();
        btnClose = new Button();
        btnSend.setOnAction(e -> sendReply(txtReply.getText().trim()));

        Region stretch = new Region();
        HBox.setHgrow(stretch, Priority.ALWAYS);

        HBox bottomBar = new HBox(10, tools, stretch, btnSend, btnClose);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10));

        replyBox = new VBox(compose, bottomBar);
        replyBox.setVisible(false);
        replyBox.setManaged(false);

        btnClose.setOnAction(e -> {
            replyBox.setVisible(false);
            replyBox.setManaged(false);
            btnResponder.setVisible(true);
            btnResponder.setManaged(true);
            selectedFiles.clear();
            updateIcons();
        });

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

        BorderPane root = new BorderPane(chatScrollPane, topContent, null, bottomArea, null);
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

        // — TÍTULO DE LA VENTANA —
        stage.setTitle(MessageFormat.format(b.getString("chat.window.title"), mensaje.getSender()));

        // — ENCABEZADO: FECHA, REMITENTE Y ASUNTO —
        lblDateKey.setText(b.getString("chat.header.date"));

        // En lugar de "LocalDateTime.now()", usamos mensaje.getFechaHora():
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (mensaje.getFechaHora() != null) {
            lblDateValue.setText(mensaje.getFechaHora().format(fmt));
        } else {
            lblDateValue.setText(LocalDateTime.now().format(fmt));
        }

        lblSenderKey.setText(b.getString("chat.header.from"));
        lblSenderValue.setText(mensaje.getSender());

        lblSubjectKey.setText(b.getString("chat.header.subject"));
        lblSubjectValue.setText(mensaje.getAsunto());

        // — CAJA DE TEXTO Y BOTONES —
        txtReply.setPromptText(b.getString("chat.prompt.reply"));
        btnSend.setText(b.getString("chat.button.send"));
        btnClose.setText(b.getString("chat.button.cancel"));

        // — ESTADO DE CIFRADO, TEMPORIZADOR Y ADJUNTOS —
        lblEncryptState.setText(b.getString(encrypt ? "chat.encrypt.on" : "chat.encrypt.off"));
        miTimerOff.setText(b.getString("chat.timer.off"));
        btnResponder.setText(b.getString("chat.button.reply"));

        if (timerSelection.isEmpty()) {
            lblTimerState.setText(b.getString("chat.timer.off"));
        } else {
            lblTimerState.setText(timerSelection);
        }

        if (selectedFiles.isEmpty()) {
            lblAttachState.setText(b.getString("chat.attach.off"));
        } else {
            lblAttachState.setText(MessageFormat.format(b.getString("chat.attach.added"), selectedFiles.size()));
        }
    }


    /* =================================================================================== */
    /*                              GESTIÓN DE MENSAJES EN CHAT                          */
    /* =================================================================================== */
    private void addMessageToChat(String sender, String content, boolean isCurrentUser, LocalDateTime fechaHoraMensaje) {
        // Contenedor principal del mensaje (hora + rectángulo)
        VBox messageWithTime = new VBox(2); // Espacio vertical entre hora y mensaje
        messageWithTime.setMaxWidth(500);

        // Hora del mensaje (justo encima del rectángulo)
        DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm");
        String horaParaMostrar = (fechaHoraMensaje != null) ? fechaHoraMensaje.format(horaFmt) : LocalDateTime.now().format(horaFmt);

        Label timeLabel = new Label(horaParaMostrar);
        timeLabel.getStyleClass().add("chat-time-label");
        HBox timeContainer = new HBox();
        timeContainer.getChildren().add(timeLabel);

        // Rectángulo del mensaje (solo contenido)
        VBox messageContainer = new VBox();
        messageContainer.setPadding(new Insets(10));
        messageContainer.getStyleClass().add("message-box");

        // Contenido del mensaje
        Label messageContent = new Label(content);
        messageContent.setWrapText(true);
        messageContent.getStyleClass().add("chat-message-content");
        messageContainer.getChildren().add(messageContent);

        // Añadimos hora y mensaje al contenedor principal
        messageWithTime.getChildren().addAll(timeContainer, messageContainer);

        // Posicionamiento según quién envió el mensaje
        HBox messageRow = new HBox();
        if (isCurrentUser) {
            // Mensajes del usuario actual a la derecha
            timeContainer.setAlignment(Pos.CENTER_RIGHT);
            messageContainer.getStyleClass().add("message-sent");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            messageRow.getChildren().addAll(spacer, messageWithTime);
        } else {
            // Mensajes recibidos a la izquierda
            timeContainer.setAlignment(Pos.CENTER_LEFT);
            messageContainer.getStyleClass().add("message-received");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            messageRow.getChildren().addAll(messageWithTime, spacer);
        }

        chatArea.getChildren().add(messageRow);

        // Scroll automático al último mensaje
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    /* =================================================================================== */
    /*                               ENVÍO DE RESPUESTA                                   */
    /* =================================================================================== */
    private void sendReply(String plainText) {
        ResourceBundle b = LocaleManager.bundle();

        if (plainText.isEmpty() && selectedFiles.isEmpty()) {
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

                // Calcular fecha de expiración
                String timerSelection = lblTimerState.getText();
                dto.setExpiryDate(calculateExpiry(timerSelection));

                // Texto (cifrado o claro)
                if (!plainText.isEmpty()) {
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
                        adjuntosDto.add(new AdjuntoDTO(filename, mimeType, Base64.getEncoder().encodeToString(fileBytes), null, null));
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
                var request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/messages/send")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();

                http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(resp -> {
                    if (resp.statusCode() == 200) {
                        // parsear respuesta y actualizar UI
                        Platform.runLater(() -> {
                            try {
                                MensajeDTO saved = mapper.readValue(resp.body(), MensajeDTO.class);
                                MessageStore.sentMessages.add(new Mensaje(saved.getId(), saved.getDestinatario(), saved.getAsunto(), plainText));

                                // Agregar el mensaje enviado al chat
                                if (!plainText.isEmpty()) {
                                    addMessageToChat(currentUser, plainText, true, LocalDateTime.now());
                                }

                                // Limpiar el área de texto y archivos seleccionados
                                txtReply.clear();
                                selectedFiles.clear();

                                // Cerrar el área de respuesta
                                replyBox.setVisible(false);
                                replyBox.setManaged(false);
                                btnResponder.setVisible(true);
                                btnResponder.setManaged(true);

                            } catch (Exception ex) {
                                pop.mostrarAlertaError(b.getString("common.error"), b.getString("chat.alert.error.serverResponse"));
                            }
                        });
                    } else {
                        Platform.runLater(() -> pop.mostrarAlertaError(b.getString("common.error"), b.getString("chat.alert.error.save")));
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> pop.mostrarAlertaError(b.getString("common.error"), b.getString("chat.alert.error.encrypt")));
            }
        });

        buildTask.setOnFailed(evt -> Platform.runLater(() -> pop.mostrarAlertaError(b.getString("common.error"), buildTask.getException().getMessage())));

        // 3) Finalmente, lanzamos el Task en el pool de background
        MainApp.Background.POOL.submit(buildTask);
    }

    private void updateIcons() {
        // Actualización del icono de cifrado (candado)
        ImageView ivLock = (ImageView) btnEncrypt.getGraphic();
        ivLock.setImage(encrypt ? icoEncryptOn : (isDark() ? icoEncryptDark : icoEncrypt));

        // Actualización del icono de temporizador
        boolean timerActivo = !timerSelection.isEmpty();
        ImageView ivTimer = (ImageView) mbTimer.getGraphic();
        ivTimer.setImage(timerActivo ? icoTimerOn : (isDark() ? icoTimerDark : icoTimer));

        // Actualización del icono de adjuntos
        ImageView ivAttach = (ImageView) btnAttach.getGraphic();
        boolean hasFiles = !selectedFiles.isEmpty();
        ivAttach.setImage(hasFiles ? icoAttachOn : (isDark() ? icoAttachDark : icoAttach));

        // Actualización del texto bajo el icono de adjuntos
        if (hasFiles) {
            lblAttachState.setText(LocaleManager.bundle().getString("chat.attach.added").replace("{0}", String.valueOf(selectedFiles.size())));
        } else {
            lblAttachState.setText(LocaleManager.bundle().getString("chat.attach.off"));
        }
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
        chooser.setTitle(LocaleManager.bundle().getString("chat.attach.save") + " " + a.getFilename());
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