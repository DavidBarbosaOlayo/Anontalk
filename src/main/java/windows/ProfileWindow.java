package windows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import managers.PopUpInfo;
import org.springframework.context.ConfigurableApplicationContext;
import utils.LocaleManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class ProfileWindow extends Stage {

    /* ----------------- constantes UI ----------------- */
    private static final double WIDTH = 800;
    private static final double HEIGHT = 700;
    private static final double BTN_WIDTH = 250;
    private static final double FIELD_WIDTH = 200;

    /* ----------------- servicios ----------------- */
    private final PopUpInfo pop = new PopUpInfo();
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    /* ----------------- inyección ----------------- */
    private final String currentUser;
    private final Stage inboxStage;
    private final ConfigurableApplicationContext springCtx;

    /* ----------------- nodos que cambian con idioma ----------------- */
    private Label lblTitle;
    private Button hdrPwdBtn, hdrEmailBtn, btnDelete;
    private Label lblPwdLabel, lblEmailLabel;
    private PasswordField txtOldPwd, txtNewPwd, txtConfPwd;
    private TextField txtNewEmail, txtConfEmail;
    private Button btnSavePwd, btnSaveEmail;

    private VBox pwdContent, emailContent;          // secciones colapsables

    /* ===================================================================================== */

    public ProfileWindow(String currentUser, Stage inboxStage, ConfigurableApplicationContext springCtx) {
        this.currentUser = currentUser;
        this.inboxStage = inboxStage;
        this.springCtx = springCtx;

        /* -------- construir interfaz -------- */
        Scene sc = new Scene(buildUI(), WIDTH, HEIGHT);
        ThemeManager tm = ThemeManager.getInstance();
        sc.getStylesheets().setAll(tm.getCss());
        tm.themeProperty().addListener((o, oldT, n) -> sc.getStylesheets().setAll(tm.getCss()));
        setScene(sc);

        /* -------- listener de idioma -------- */
        LocaleManager.localeProperty().addListener((o, oldL, newL) -> refreshTexts());
        refreshTexts();                 // pinta por 1ª vez
    }

    /* ===================================================================================== */
    /*                                      UI builder                                       */
    /* ===================================================================================== */

    private ScrollPane buildUI() {
        /* ---------- título ventana | label de cabecera ---------- */
        lblTitle = new Label();
        lblTitle.getStyleClass().add("profile-title");
        lblTitle.setAlignment(Pos.CENTER);
        lblTitle.setPadding(new Insets(30, 0, 0, 0));

        /* ---------- secciones ---------- */
        pwdContent = buildPwdContent();
        emailContent = buildEmailContent();

        hdrPwdBtn = buildSectionHeader(pwdContent);
        hdrEmailBtn = buildSectionHeader(emailContent);

        VBox rootSections = new VBox(30, buildSection(hdrPwdBtn, pwdContent), buildSection(hdrEmailBtn, emailContent));
        rootSections.setAlignment(Pos.TOP_CENTER);

        /* ---------- botón borrar cuenta ---------- */
        btnDelete = new Button();
        btnDelete.setPrefWidth(BTN_WIDTH);
        btnDelete.getStyleClass().add("delete-button");
        btnDelete.setOnAction(e -> showDeleteDialog());

        VBox deleteBox = new VBox(btnDelete);
        deleteBox.setAlignment(Pos.CENTER);
        deleteBox.setPadding(new Insets(20, 0, 0, 0));

        /* ---------- layout principal ---------- */
        VBox content = new VBox(40, lblTitle, rootSections, deleteBox);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(20));

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        return sp;
    }

    /* ===================================================================================== */
    /*                                refresco de textos                                     */
    /* ===================================================================================== */

    private void refreshTexts() {
        ResourceBundle b = bundle();

        /* título de la ventana + cabecera */
        setTitle(MessageFormat.format(b.getString("profile.window.title"), currentUser));
        lblTitle.setText(b.getString("profile.title"));

        /* headers de secciones */
        hdrPwdBtn.setText(b.getString("profile.section.changePassword"));
        hdrEmailBtn.setText(b.getString("profile.section.changeEmail"));

        /* botón borrar cuenta */
        btnDelete.setText(b.getString("profile.button.deleteAccount"));

        /* -------- sección contraseña -------- */
        lblPwdLabel.setText(b.getString("profile.section.changePassword"));
        txtOldPwd.setPromptText(b.getString("profile.field.currentPassword"));
        txtNewPwd.setPromptText(b.getString("profile.field.newPassword"));
        txtConfPwd.setPromptText(b.getString("profile.field.confirmPassword"));
        btnSavePwd.setText(b.getString("profile.button.save"));

        /* -------- sección email -------- */
        lblEmailLabel.setText(b.getString("profile.section.changeEmail"));
        txtNewEmail.setPromptText(b.getString("profile.field.newEmail"));
        txtConfEmail.setPromptText(b.getString("profile.field.confirmEmail"));
        btnSaveEmail.setText(b.getString("profile.button.save"));
    }

    /* ===================================================================================== */
    /*                              construcción de secciones                                */
    /* ===================================================================================== */

    private Button buildSectionHeader(Node content) {
        Button header = new Button();
        header.setPrefWidth(BTN_WIDTH);
        header.getStyleClass().add("section-header");

        /* animar expandir/colapsar */
        content.setVisible(false);
        content.setScaleY(0);
        content.setOpacity(0);
        content.managedProperty().bind(content.visibleProperty());

        header.setOnAction(e -> {
            if (content.isVisible()) {
                Timeline t = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(content.scaleYProperty(), 0), new KeyValue(content.opacityProperty(), 0)));
                t.setOnFinished(ev -> content.setVisible(false));
                t.play();
            } else {
                content.setVisible(true);
                Timeline t = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(content.scaleYProperty(), 0), new KeyValue(content.opacityProperty(), 0)), new KeyFrame(Duration.millis(300), new KeyValue(content.scaleYProperty(), 1), new KeyValue(content.opacityProperty(), 1)));
                t.play();
            }
        });
        return header;
    }

    private VBox buildSection(Button header, Node content) {
        VBox box = new VBox(10, header, content);
        box.setAlignment(Pos.TOP_CENTER);
        return box;
    }

    /* ---------- contenido cambio contraseña ---------- */
    private VBox buildPwdContent() {
        lblPwdLabel = new Label();
        lblPwdLabel.getStyleClass().add("section-label");

        txtOldPwd = new PasswordField();
        txtOldPwd.setPrefWidth(FIELD_WIDTH);
        txtOldPwd.setMaxWidth(FIELD_WIDTH);
        txtNewPwd = new PasswordField();
        txtNewPwd.setPrefWidth(FIELD_WIDTH);
        txtNewPwd.setMaxWidth(FIELD_WIDTH);
        txtConfPwd = new PasswordField();
        txtConfPwd.setPrefWidth(FIELD_WIDTH);
        txtConfPwd.setMaxWidth(FIELD_WIDTH);

        btnSavePwd = new Button();
        btnSavePwd.setPrefWidth(BTN_WIDTH);
        btnSavePwd.getStyleClass().add("save-button");
        btnSavePwd.setOnAction(e -> handlePwdSave());

        VBox v = new VBox(12, lblPwdLabel, txtOldPwd, txtNewPwd, txtConfPwd, btnSavePwd);
        v.setPadding(new Insets(10, 0, 0, 0));
        v.setAlignment(Pos.CENTER);
        return v;
    }

    /* ---------- contenido cambio email ---------- */
    private VBox buildEmailContent() {
        lblEmailLabel = new Label();
        lblEmailLabel.getStyleClass().add("section-label");

        txtNewEmail = new TextField();
        txtNewEmail.setPrefWidth(FIELD_WIDTH);
        txtNewEmail.setMaxWidth(FIELD_WIDTH);
        txtConfEmail = new TextField();
        txtConfEmail.setPrefWidth(FIELD_WIDTH);
        txtConfEmail.setMaxWidth(FIELD_WIDTH);

        btnSaveEmail = new Button();
        btnSaveEmail.setPrefWidth(BTN_WIDTH);
        btnSaveEmail.getStyleClass().add("save-button");
        btnSaveEmail.setOnAction(e -> handleEmailSave());

        VBox v = new VBox(12, lblEmailLabel, txtNewEmail, txtConfEmail, btnSaveEmail);
        v.setPadding(new Insets(10, 0, 0, 0));
        v.setAlignment(Pos.CENTER);
        return v;
    }

    /* ===================================================================================== */
    /*                               lógica: borrar cuenta                                   */
    /* ===================================================================================== */

    private void showDeleteDialog() {
        ResourceBundle b = bundle();

        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle(b.getString("profile.dialog.deleteAccount.title"));
        dlg.setHeaderText(b.getString("profile.dialog.deleteAccount.header"));
        dlg.setContentText(b.getString("profile.dialog.deleteAccount.content"));

        dlg.showAndWait().ifPresent(input -> {
            String[] parts = b.getString("profile.dialog.deleteAccount.header").split("\n");
            String expected = parts.length > 1 ? parts[1].replace("\"", "").trim() : "";
            if (input.trim().equals(expected)) {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/" + currentUser)).DELETE().build();
                http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(resp -> Platform.runLater(() -> {
                    if (resp.statusCode() == 200 || resp.statusCode() == 204) {
                        pop.mostrarAlertaInformativa(b.getString("common.success"), b.getString("profile.alert.info.accountDeleted"));
                        close();
                        inboxStage.close();
                        try {
                            new LoginWindow(springCtx).start(new Stage());
                        } catch (Exception ex) {
                            pop.mostrarAlertaError(b.getString("common.error"), b.getString("profile.alert.error.openProfile"));
                        }
                    } else {
                        pop.mostrarAlertaError(b.getString("common.error"), MessageFormat.format(b.getString("profile.alert.error.accountNotDeleted"), resp.statusCode()));
                    }
                }));
            } else {
                pop.mostrarAlertaError(b.getString("common.error"), b.getString("profile.alert.error.confirmation"));
            }
        });
    }

    /* ===================================================================================== */
    /*                               lógica: guardar cambios                                 */
    /* ===================================================================================== */

    private void handlePwdSave() {
        ResourceBundle b = bundle();

        String oldPwd = txtOldPwd.getText().trim();
        String newPwd = txtNewPwd.getText().trim();
        String conf = txtConfPwd.getText().trim();

        if (oldPwd.isEmpty() || newPwd.isEmpty() || conf.isEmpty()) {
            pop.mostrarAlertaError(b.getString("common.error"), b.getString("profile.alert.error.fillFields"));
            return;
        }
        if (!newPwd.equals(conf)) {
            pop.mostrarAlertaError(b.getString("common.error"), b.getString("profile.alert.error.passwordMismatch"));
            return;
        }
        try {
            ObjectNode body = mapper.createObjectNode().put("username", currentUser).put("oldPassword", oldPwd).put("newPassword", newPwd);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/change-password")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            http.sendAsync(req, HttpResponse.BodyHandlers.discarding()).thenAccept(resp -> Platform.runLater(() -> {
                if (resp.statusCode() == 200) {
                    pop.mostrarAlertaInformativa(b.getString("common.success"), b.getString("profile.alert.info.passwordChanged"));
                } else {
                    pop.mostrarAlertaError(b.getString("common.error"), MessageFormat.format(b.getString("profile.alert.error.passwordChangeFailed"), resp.statusCode()));
                }
            }));
        } catch (Exception ex) {
            pop.mostrarAlertaError(b.getString("common.error"), b.getString("profile.alert.error.connection"));
        }
    }

    private void handleEmailSave() {
        ResourceBundle b = bundle();

        String newE = txtNewEmail.getText().trim();
        String confE = txtConfEmail.getText().trim();

        if (newE.isEmpty() || confE.isEmpty()) {
            pop.mostrarAlertaError(b.getString("common.error"), b.getString("profile.alert.error.fillEmailFields"));
            return;
        }
        if (!newE.equals(confE)) {
            pop.mostrarAlertaError(b.getString("common.error"), b.getString("profile.alert.error.emailMismatch"));
            return;
        }
        try {
            ObjectNode body = mapper.createObjectNode().put("username", currentUser).put("newEmail", newE);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/change-email")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            http.sendAsync(req, HttpResponse.BodyHandlers.discarding()).thenAccept(resp -> Platform.runLater(() -> {
                if (resp.statusCode() == 200) {
                    pop.mostrarAlertaInformativa(b.getString("common.success"), b.getString("profile.alert.info.emailChanged"));
                } else {
                    pop.mostrarAlertaError(b.getString("common.error"), MessageFormat.format(b.getString("profile.alert.error.emailChangeFailed"), resp.statusCode()));
                }
            }));
        } catch (Exception ex) {
            pop.mostrarAlertaError(b.getString("common.error"), b.getString("profile.alert.error.connection"));
        }
    }

    /* ===================================================================================== */
    private static ResourceBundle bundle() {
        return LocaleManager.bundle();
    }
}
