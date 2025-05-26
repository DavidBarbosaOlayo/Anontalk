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
import javafx.stage.StageStyle;
import javafx.util.Duration;
import managers.PopUpInfo;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class ProfileWindow extends Stage {

    private static final double WIDTH = 800;
    private static final double HEIGHT = 700;
    private static final double BTN_WIDTH = 250;
    private static final double FIELD_WIDTH = 200;

    private final PopUpInfo pop = new PopUpInfo();
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final String currentUser;
    private final Stage inboxStage;
    private final ConfigurableApplicationContext springCtx;
    private final ResourceBundle b;

    public ProfileWindow(String currentUser, Stage inboxStage, ConfigurableApplicationContext springCtx) {
        this.currentUser = currentUser;
        this.inboxStage = inboxStage;
        this.springCtx = springCtx;
        // Cargamos bundle en inglés
        this.b = ResourceBundle.getBundle("i18n/messages", Locale.ENGLISH);

        // ---------- Ventana ----------
        setTitle(MessageFormat.format(b.getString("profile.window.title"), currentUser));

        // ---------- Título ----------
        Label lblTitle = new Label(b.getString("profile.title"));
        lblTitle.getStyleClass().add("profile-title");
        lblTitle.setAlignment(Pos.CENTER);
        lblTitle.setPadding(new Insets(30, 0, 0, 0));

        // ---------- Secciones ----------
        VBox rootSections = new VBox(30, buildSection(b.getString("profile.section.changePassword"), buildPwdContent()), buildSection(b.getString("profile.section.changeEmail"), buildEmailContent()));
        rootSections.setAlignment(Pos.TOP_CENTER);

        // ---------- Botón Eliminar cuenta ----------
        Button btnDel = new Button(b.getString("profile.button.deleteAccount"));
        btnDel.setPrefWidth(BTN_WIDTH);
        btnDel.getStyleClass().add("delete-button");
        btnDel.setOnAction(e -> showDeleteDialog());

        VBox deleteBox = new VBox(btnDel);
        deleteBox.setAlignment(Pos.CENTER);
        deleteBox.setPadding(new Insets(20, 0, 0, 0));

        // ---------- Layout principal ----------
        VBox content = new VBox(40, lblTitle, rootSections, deleteBox);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Scene scene = new Scene(scrollPane, WIDTH, HEIGHT);
        // Tema
        ThemeManager tm = ThemeManager.getInstance();
        scene.getStylesheets().setAll(tm.getCss());
        tm.themeProperty().addListener((obs, oldT, newT) -> scene.getStylesheets().setAll(tm.getCss()));

        setScene(scene);
    }

    private void showDeleteDialog() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle(b.getString("profile.dialog.deleteAccount.title"));
        dlg.setHeaderText(b.getString("profile.dialog.deleteAccount.header"));
        dlg.setContentText(b.getString("profile.dialog.deleteAccount.content"));

        dlg.showAndWait().ifPresent(input -> {
            // Extraemos la segunda línea del header y quitamos las comillas
            String[] parts = b.getString("profile.dialog.deleteAccount.header").split("\n");
            String expected = parts[1].replace("\"", "").trim();
            if (input.trim().equals(expected)) {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/" + currentUser)).DELETE().build();
                http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(resp -> Platform.runLater(() -> {
                    if (resp.statusCode() == 200 || resp.statusCode() == 204) {
                        pop.mostrarAlertaInformativa(b.getString("common.success"), b.getString("profile.alert.info.accountDeleted"));
                        this.close();
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

    private VBox buildSection(String headerText, Node content) {
        Button header = new Button(headerText);
        header.setPrefWidth(BTN_WIDTH);
        header.getStyleClass().add("section-header");

        content.setVisible(false);
        content.setScaleY(0);
        content.setOpacity(0);
        content.managedProperty().bind(content.visibleProperty());

        header.setOnAction(e -> {
            if (content.isVisible()) {
                Timeline collapse = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(content.scaleYProperty(), 0), new KeyValue(content.opacityProperty(), 0)));
                collapse.setOnFinished(ev -> content.setVisible(false));
                collapse.play();
            } else {
                content.setVisible(true);
                Timeline expand = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(content.scaleYProperty(), 0), new KeyValue(content.opacityProperty(), 0)), new KeyFrame(Duration.millis(300), new KeyValue(content.scaleYProperty(), 1), new KeyValue(content.opacityProperty(), 1)));
                expand.play();
            }
        });

        VBox box = new VBox(10, header, content);
        box.setAlignment(Pos.TOP_CENTER);
        return box;
    }

    private VBox buildPwdContent() {
        Label lbl = new Label(b.getString("profile.section.changePassword"));
        lbl.getStyleClass().add("section-label");

        PasswordField txtOld = new PasswordField();
        txtOld.setPromptText(b.getString("profile.field.currentPassword"));
        txtOld.setPrefWidth(FIELD_WIDTH);
        txtOld.setMaxWidth(FIELD_WIDTH);

        PasswordField txtNew = new PasswordField();
        txtNew.setPromptText(b.getString("profile.field.newPassword"));
        txtNew.setPrefWidth(FIELD_WIDTH);
        txtNew.setMaxWidth(FIELD_WIDTH);

        PasswordField txtConfirm = new PasswordField();
        txtConfirm.setPromptText(b.getString("profile.field.confirmPassword"));
        txtConfirm.setPrefWidth(FIELD_WIDTH);
        txtConfirm.setMaxWidth(FIELD_WIDTH);

        Button btnSave = new Button(b.getString("profile.button.save"));
        btnSave.setPrefWidth(BTN_WIDTH);
        btnSave.getStyleClass().add("save-button");
        btnSave.setOnAction(e -> {
            String oldPwd = txtOld.getText().trim();
            String newPwd = txtNew.getText().trim();
            String confPwd = txtConfirm.getText().trim();
            if (oldPwd.isEmpty() || newPwd.isEmpty() || confPwd.isEmpty()) {
                pop.mostrarAlertaError(b.getString("common.error"), b.getString("profile.alert.error.fillFields"));
                return;
            }
            if (!newPwd.equals(confPwd)) {
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
        });

        VBox v = new VBox(12, lbl, txtOld, txtNew, txtConfirm, btnSave);
        v.setPadding(new Insets(10, 0, 0, 0));
        v.setAlignment(Pos.CENTER);
        return v;
    }

    private VBox buildEmailContent() {
        Label lbl = new Label(b.getString("profile.section.changeEmail"));
        lbl.getStyleClass().add("section-label");

        TextField txtNew = new TextField();
        txtNew.setPromptText(b.getString("profile.field.newEmail"));
        txtNew.setPrefWidth(FIELD_WIDTH);
        txtNew.setMaxWidth(FIELD_WIDTH);

        TextField txtConfirm = new TextField();
        txtConfirm.setPromptText(b.getString("profile.field.confirmEmail"));
        txtConfirm.setPrefWidth(FIELD_WIDTH);
        txtConfirm.setMaxWidth(FIELD_WIDTH);

        Button btnSave = new Button(b.getString("profile.button.save"));
        btnSave.setPrefWidth(BTN_WIDTH);
        btnSave.getStyleClass().add("save-button");
        btnSave.setOnAction(e -> {
            String newEmail = txtNew.getText().trim();
            String confEmail = txtConfirm.getText().trim();
            if (newEmail.isEmpty() || confEmail.isEmpty()) {
                pop.mostrarAlertaError(b.getString("common.error"), b.getString("profile.alert.error.fillEmailFields"));
                return;
            }
            if (!newEmail.equals(confEmail)) {
                pop.mostrarAlertaError(b.getString("common.error"), b.getString("profile.alert.error.emailMismatch"));
                return;
            }
            try {
                ObjectNode body = mapper.createObjectNode().put("username", currentUser).put("newEmail", newEmail);
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
        });

        VBox v = new VBox(12, lbl, txtNew, txtConfirm, btnSave);
        v.setPadding(new Insets(10, 0, 0, 0));
        v.setAlignment(Pos.CENTER);
        return v;
    }
}
