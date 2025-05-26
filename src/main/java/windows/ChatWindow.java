package windows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
import java.util.ResourceBundle;

/**
 * Ventana de conversación (lectura + respuesta) con cambio de idioma en caliente.
 */
public class ChatWindow {

    /* ======= dependencias / estado ======= */
    private final String currentUser;
    private final Mensaje mensaje;          // mensaje que abrimos
    private final PopUpInfo pop = new PopUpInfo();

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /* ======= nodos que cambian con el idioma ======= */
    private Stage stage;
    private Label lblHeader;
    private TextArea txtReply;
    private Button btnBold;
    private Button btnEnviar;
    private Button btnCerrar;

    /* ------------------------------------------------------------------------------------ */

    public ChatWindow(String currentUser, Mensaje mensaje) {
        this.currentUser = currentUser;
        this.mensaje = mensaje;
    }

    /* ==================================================================================== */
    /*                                          UI                                          */
    /* ==================================================================================== */

    public void show() {
        stage = new Stage();

        /* ---------- CABECERA ---------- */
        lblHeader = new Label();
        lblHeader.getStyleClass().add("chat-header");

        /* ---------- CUERPO RECIBIDO ---------- */
        Label lblBody = new Label(mensaje.getContent());
        lblBody.setWrapText(true);
        lblBody.getStyleClass().add("chat-message-body");
        ScrollPane scroll = new ScrollPane(lblBody);
        scroll.setFitToWidth(true);

        /* ---------- REDACTOR ---------- */
        txtReply = new TextArea();
        txtReply.setPrefRowCount(4);
        txtReply.getStyleClass().add("chat-textarea");

        btnBold = new Button();
        btnBold.setOnAction(e -> txtReply.appendText(" **texto en negrita** "));

        HBox toolbar = new HBox(10, btnBold);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox compose = new VBox(5, toolbar, txtReply);
        compose.setPadding(new Insets(10));

        /* ---------- BOTONERA ---------- */
        btnEnviar = new Button();
        btnCerrar = new Button();
        btnEnviar.setOnAction(e -> sendReply(txtReply.getText().trim()));
        btnCerrar.setOnAction(e -> stage.close());

        HBox bar = new HBox(10, btnEnviar, btnCerrar);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(10));

        /* ---------- ROOT ---------- */
        BorderPane root = new BorderPane(scroll);
        root.setTop(lblHeader);
        root.setBottom(new VBox(compose, bar));
        root.setPadding(new Insets(10));
        root.getStyleClass().add("chat-root");

        /* ---------- SCENE ---------- */
        Scene scene = new Scene(root, 800, 600);
        ThemeManager tm = ThemeManager.getInstance();
        scene.getStylesheets().setAll(tm.getCss());
        tm.themeProperty().addListener((o, oldT, newT) -> scene.getStylesheets().setAll(tm.getCss()));

        /* ---------- Idioma dinámico ---------- */
        LocaleManager.localeProperty().addListener((o, oldL, newL) -> refreshTexts());
        refreshTexts();          // primera vez

        stage.setScene(scene);
        stage.show();
    }

    /* ==================================================================================== */
    /*                                  REFRESCO DE TEXTOS                                  */
    /* ==================================================================================== */

    private void refreshTexts() {
        ResourceBundle b = LocaleManager.bundle();

        stage.setTitle(MessageFormat.format(b.getString("chat.window.title"), mensaje.getSender()));

        lblHeader.setText(b.getString("chat.header.from") + " " + mensaje.getSender() + "  |  " + b.getString("chat.header.subject") + " " + mensaje.getAsunto() + "  |  " + b.getString("chat.header.date") + " " + LocalDateTime.now());

        txtReply.setPromptText(b.getString("chat.prompt.reply"));

        btnBold.setText(b.getString("chat.button.bold"));
        btnEnviar.setText(b.getString("chat.button.send"));
        btnCerrar.setText(b.getString("chat.button.close"));
    }

    /* ==================================================================================== */
    /*                                    ENVÍO RESPUESTA                                   */
    /* ==================================================================================== */

    private void sendReply(String plainText) {
        ResourceBundle b = LocaleManager.bundle();

        if (plainText.isEmpty()) {
            pop.mostrarAlertaError(b.getString("common.error"), b.getString("chat.alert.error.emptyMessage"));
            return;
        }
        String destinatario = mensaje.getSender();

        try {
            /* clave pública destinatario */
            HttpRequest pkReq = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/" + destinatario + "/publicKey")).GET().build();
            String pubB64 = http.send(pkReq, HttpResponse.BodyHandlers.ofString()).body();
            PublicKey destPk = RSAUtils.publicKeyFromBase64(pubB64);

            /* cifrado híbrido */
            HybridCrypto.HybridPayload p = HybridCrypto.encrypt(plainText, destPk);

            /* DTO */
            MensajeDTO dto = new MensajeDTO();
            dto.setRemitente(currentUser);
            dto.setDestinatario(destinatario);
            dto.setAsunto(mensaje.getAsunto());
            dto.setCipherTextBase64(p.cipherB64());
            dto.setEncKeyBase64(p.encKeyB64());
            dto.setIvBase64(p.ivB64());

            String json = mapper.writeValueAsString(dto);

            /* POST /messages/send */
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
