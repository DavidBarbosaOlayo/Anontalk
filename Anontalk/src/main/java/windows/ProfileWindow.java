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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ProfileWindow extends Stage {

    private final PopUpInfo pop = new PopUpInfo();
    private static final double WIDTH = 800;
    private static final double HEIGHT = 700;
    private static final double BTN_WIDTH = 250;
    private static final double FIELD_WIDTH = 200;

    private final String currentUser;
    private final Stage inboxStage;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ConfigurableApplicationContext springCtx;

    public ProfileWindow(String currentUser, Stage inboxStage, ConfigurableApplicationContext springCtx) {
        this.currentUser = currentUser;
        this.inboxStage = inboxStage;
        this.springCtx   = springCtx;

        setTitle("Perfil de " + currentUser);

        Label lblTitle = new Label("Perfil Usuario");
        lblTitle.getStyleClass().add("profile-title");
        lblTitle.setAlignment(Pos.CENTER);
        lblTitle.setPadding(new Insets(30, 0, 0, 0));

        VBox rootSections = new VBox(
                30,
                buildSection("Cambiar contraseña", buildPwdContent()),
                buildSection("Cambiar email", buildEmailContent())
        );
        rootSections.setAlignment(Pos.TOP_CENTER);

        Button btnDel = new Button("Eliminar cuenta");
        btnDel.setPrefWidth(BTN_WIDTH);
        btnDel.setAlignment(Pos.CENTER);
        btnDel.getStyleClass().add("delete-button");
        // En ProfileWindow.java, dentro del constructor, reemplaza el bloque de elim. cuenta por:

        btnDel.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog();
            dlg.setTitle("Eliminar cuenta");
            dlg.setHeaderText("Para confirmar, escribe:\n\"Estoy seguro de eliminar la cuenta\"");
            dlg.setContentText("Confirmación:");
            dlg.showAndWait().ifPresent(input -> {
                if ("Estoy seguro de eliminar la cuenta".equals(input.trim())) {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/users/" + currentUser))
                            .DELETE()
                            .build();
                    http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                            .thenAccept(resp -> Platform.runLater(() -> {
                                if (resp.statusCode() == 200 || resp.statusCode() == 204) {
                                    pop.mostrarAlertaInformativa("Cuenta eliminada", "Tu cuenta ha sido eliminada correctamente.");
                                    this.close();
                                    inboxStage.close();
                                    try {
                                        // ← Volvemos al login directamente con el mismo contexto
                                        new LoginWindow(springCtx).start(new Stage());
                                    } catch (Exception ex) {
                                        pop.mostrarAlertaError("Error", "No se pudo abrir la ventana de login.");
                                    }
                                } else {
                                    pop.mostrarAlertaError("Error", "No se pudo eliminar la cuenta (código " + resp.statusCode() + ").");
                                }
                            }));
                } else {
                    pop.mostrarAlertaError("Confirmación incorrecta", "El texto no coincide. Operación cancelada.");
                }
            });
        });

        VBox deleteBox = new VBox(btnDel);
        deleteBox.setAlignment(Pos.CENTER);
        deleteBox.setPadding(new Insets(20, 0, 0, 0));

        VBox content = new VBox(40, lblTitle, rootSections, deleteBox);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Scene scene = new Scene(scrollPane, WIDTH, HEIGHT);

        // Gestión de tema
        ThemeManager tm = ThemeManager.getInstance();
        scene.getStylesheets().setAll(tm.getCss());
        tm.themeProperty().addListener((obs, oldT, newT) -> {
            scene.getStylesheets().setAll(tm.getCss());
        });

        setScene(scene);
    }

    private VBox buildSection(String headerText, Node content) {
        Button header = new Button(headerText);
        header.setPrefWidth(BTN_WIDTH);
        header.setAlignment(Pos.CENTER);
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
        Label lbl = new Label("Cambiar contraseña");
        lbl.getStyleClass().add("section-label");

        PasswordField txtOld = new PasswordField();
        txtOld.setPromptText("Contraseña actual");
        txtOld.setPrefWidth(FIELD_WIDTH);
        txtOld.setMaxWidth(FIELD_WIDTH);

        PasswordField txtNew = new PasswordField();
        txtNew.setPromptText("Nueva contraseña");
        txtNew.setPrefWidth(FIELD_WIDTH);
        txtNew.setMaxWidth(FIELD_WIDTH);

        PasswordField txtConfirm = new PasswordField();
        txtConfirm.setPromptText("Confirma nueva contraseña");
        txtConfirm.setPrefWidth(FIELD_WIDTH);
        txtConfirm.setMaxWidth(FIELD_WIDTH);

        Button btnSave = new Button("Guardar");
        btnSave.setPrefWidth(BTN_WIDTH);
        btnSave.setAlignment(Pos.CENTER);
        btnSave.getStyleClass().add("save-button");
        btnSave.setOnAction(e -> {
            String oldPwd = txtOld.getText().trim();
            String newPwd = txtNew.getText().trim();
            String confPwd = txtConfirm.getText().trim();
            if (oldPwd.isEmpty() || newPwd.isEmpty() || confPwd.isEmpty()) {
                pop.mostrarAlertaError("Error", "Completa todos los campos.");
                return;
            }
            if (!newPwd.equals(confPwd)) {
                pop.mostrarAlertaError("Error", "Las nuevas contraseñas no coinciden.");
                return;
            }
            try {
                ObjectNode body = mapper.createObjectNode().put("username", currentUser).put("oldPassword", oldPwd).put("newPassword", newPwd);
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/change-password")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
                http.sendAsync(req, HttpResponse.BodyHandlers.discarding()).thenAccept(resp -> Platform.runLater(() -> {
                    if (resp.statusCode() == 200) {
                        pop.mostrarAlertaInformativa("Éxito", "Contraseña cambiada correctamente.");
                    } else {
                        pop.mostrarAlertaError("Error", "No se pudo cambiar la contraseña (" + resp.statusCode() + ").");
                    }
                }));
            } catch (Exception ex) {
                pop.mostrarAlertaError("Error", "Error al conectar con el servidor.");
            }
        });

        VBox v = new VBox(12, lbl, txtOld, txtNew, txtConfirm, btnSave);
        v.setPadding(new Insets(10, 0, 0, 0));
        v.setAlignment(Pos.CENTER);
        return v;
    }

    private VBox buildEmailContent() {
        Label lbl = new Label("Cambiar email");
        lbl.getStyleClass().add("section-label");

        TextField txtNew = new TextField();
        txtNew.setPromptText("Nuevo correo");
        txtNew.setPrefWidth(FIELD_WIDTH);
        txtNew.setMaxWidth(FIELD_WIDTH);

        TextField txtConfirm = new TextField();
        txtConfirm.setPromptText("Confirma correo");
        txtConfirm.setPrefWidth(FIELD_WIDTH);
        txtConfirm.setMaxWidth(FIELD_WIDTH);

        Button btnSave = new Button("Guardar");
        btnSave.setPrefWidth(BTN_WIDTH);
        btnSave.setAlignment(Pos.CENTER);
        btnSave.getStyleClass().add("save-button");
        btnSave.setOnAction(e -> {
            String newEmail = txtNew.getText().trim();
            String confEmail = txtConfirm.getText().trim();
            if (newEmail.isEmpty() || confEmail.isEmpty()) {
                pop.mostrarAlertaError("Error", "Completa ambos campos.");
                return;
            }
            if (!newEmail.equals(confEmail)) {
                pop.mostrarAlertaError("Error", "Los correos no coinciden.");
                return;
            }
            try {
                ObjectNode body = mapper.createObjectNode().put("username", currentUser).put("newEmail", newEmail);
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/change-email")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
                http.sendAsync(req, HttpResponse.BodyHandlers.discarding()).thenAccept(resp -> Platform.runLater(() -> {
                    if (resp.statusCode() == 200) {
                        pop.mostrarAlertaInformativa("Éxito", "Email cambiado correctamente.");
                    } else {
                        pop.mostrarAlertaError("Error", "No se pudo cambiar el email (" + resp.statusCode() + ").");
                    }
                }));
            } catch (Exception ex) {
                pop.mostrarAlertaError("Error", "Error al conectar con el servidor.");
            }
        });

        VBox v = new VBox(12, lbl, txtNew, txtConfirm, btnSave);
        v.setPadding(new Insets(10, 0, 0, 0));
        v.setAlignment(Pos.CENTER);
        return v;
    }
}
