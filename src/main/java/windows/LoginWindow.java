package windows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Pair;
import managers.PopUpInfo;
import managers.users.UserService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import security.encryption.AESUtils;
import security.encryption.KeyManager;
import security.encryption.RSAUtils;
import utils.LocaleManager;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

@SpringBootApplication(scanBasePackages = {"windows", "managers.mensajes", "managers.users", "security.passwords"})
@EnableJpaRepositories(basePackages = {"managers.mensajes", "managers.users", "security.passwords"})
@EntityScan(basePackages = {"managers.mensajes", "managers.users", "security.passwords"})
public class LoginWindow extends Application {

    private final ConfigurableApplicationContext springCtx;
    private final PopUpInfo pop = new PopUpInfo();
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private Stage primaryStage;
    private ResourceBundle b;
    private final UserService svc;

    /**
     * Recibe el contexto de Spring arrancado en el splash
     */
    public LoginWindow(ConfigurableApplicationContext ctx) {
        this.springCtx = ctx;
        this.svc = ctx.getBean(UserService.class); // Obtener el servicio del contexto

    }

    public static void main(String[] args) {
        // Para ejecutar standalone si se desea
        SpringApplication.run(LoginWindow.class, args);

    }

    @Override
    public void start(Stage stage) {
        Locale locale = Locale.ENGLISH;
        this.b = ResourceBundle.getBundle("i18n/messages", locale);

        this.primaryStage = stage;
        stage.setTitle(b.getString("login.button.login") + " - " + b.getString("login.appName"));
        stage.setOnCloseRequest(e -> {
            springCtx.close();
            Platform.exit();
            System.exit(0);
        });

        try {
            Image appIcon = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/assets/logo.png")
            ));
            stage.getIcons().add(appIcon);
        } catch (Exception e) {
            System.err.println("Error cargando icono: " + e.getMessage());
        }

        // --- Construcción de la UI ---
        Label lblTitle = new Label(b.getString("login.appName"));
        lblTitle.getStyleClass().add("welcome-label");
        GridPane.setHalignment(lblTitle, HPos.CENTER);

        TextField txtUser = new TextField();
        txtUser.setPromptText(b.getString("login.label.user"));
        PasswordField txtPwd = new PasswordField();
        txtPwd.setPromptText(b.getString("login.label.password"));
        TextField txtEmail = new TextField();
        txtEmail.setPromptText(b.getString("login.label.email"));

        Button btnLogin = new Button(b.getString("login.button.login"));
        Button btnReg = new Button(b.getString("login.button.register"));
        Hyperlink lnkForgot = new Hyperlink(b.getString("login.link.forgot"));

        btnLogin.setOnAction(e -> login(txtUser.getText().trim(), txtPwd.getText().trim()));
        btnReg.setOnAction(e -> register(txtUser.getText().trim(), txtPwd.getText().trim(), txtEmail.getText().trim()));
        lnkForgot.setOnAction(e -> forgotPasswordDialog());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(30));
        grid.setMaxWidth(300);

        grid.add(lblTitle, 0, 0, 2, 1);
        GridPane.setMargin(lblTitle, new Insets(0, 0, 20, 0));

        grid.add(new Label(b.getString("login.label.user") + ":"), 0, 1);
        grid.add(txtUser, 1, 1);

        grid.add(new Label(b.getString("login.label.password") + ":"), 0, 2);
        grid.add(txtPwd, 1, 2);

        grid.add(new Label(b.getString("login.label.email") + ":"), 0, 3);
        grid.add(txtEmail, 1, 3);

        HBox buttonBox = new HBox(10, btnLogin, btnReg);
        buttonBox.setAlignment(Pos.CENTER);
        grid.add(buttonBox, 0, 4, 2, 1);
        GridPane.setMargin(buttonBox, new Insets(30, 0, 0, 0));

        grid.add(lnkForgot, 0, 5, 2, 1);
        GridPane.setHalignment(lnkForgot, HPos.CENTER);
        GridPane.setMargin(lnkForgot, new Insets(15, 0, 0, 0));

        StackPane root = new StackPane(grid);
        Scene scene = new Scene(root, 450, 420);
        scene.getStylesheets().add(getClass().getResource("/temas/temas.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private void forgotPasswordDialog() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle(b.getString("login.dialog.forgot.title"));
        dlg.setHeaderText(b.getString("login.dialog.forgot.header"));
        dlg.setContentText(b.getString("login.dialog.forgot.content"));

        dlg.showAndWait().ifPresent(email -> {
            if (email.isBlank()) {
                pop.mostrarAlertaError(b.getString("common.error"), b.getString("login.alert.error.emptyEmail"));
                return;
            }
            try {
                JsonNode body = mapper.createObjectNode().put("email", email.trim());
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/forgot-password")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();

                http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(resp -> Platform.runLater(() -> {
                    switch (resp.statusCode()) {
                        case 200 ->
                                pop.mostrarAlertaInformativa(b.getString("common.info"), MessageFormat.format(b.getString("login.alert.info.emailSent"), email.trim()));
                        case 404 ->
                                pop.mostrarAlertaError(b.getString("common.error"), MessageFormat.format(b.getString("login.alert.error.emailNotRegistered"), email.trim()));
                        default ->
                                pop.mostrarAlertaError(b.getString("common.error"), b.getString("login.alert.error.requestFailed"));
                    }
                }));
            } catch (Exception ex) {
                pop.mostrarAlertaError(b.getString("common.error"), b.getString("login.alert.error.requestFailedNetwork"));
            }
        });
    }

    private void register(String user, String pwd, String email) {
        if (user.isBlank() || pwd.isBlank() || email.isBlank()) {
            pop.mostrarAlertaError(b.getString("common.error"), b.getString("login.alert.error.fillFields"));
            return;
        }
        try {
            JsonNode body = mapper.createObjectNode()
                    .put("username", user)
                    .put("password", pwd)
                    .put("email", email);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/users/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(resp -> Platform.runLater(() -> {
                        if (resp.statusCode() == 200) {
                            pop.mostrarAlertaInformativa(b.getString("common.success"),
                                    b.getString("login.alert.info.userRegistered"));
                        } else {
                            // Extraer mensaje de error del servidor
                            String errorMessage;
                            try {
                                JsonNode errorJson = mapper.readTree(resp.body());
                                errorMessage = errorJson.get("message").asText();
                            } catch (Exception ex) {
                                errorMessage = b.getString("login.alert.error.registerFailed");
                            }

                            pop.mostrarAlertaError(b.getString("common.error"), errorMessage);
                        }
                    }));
        } catch (Exception ignored) {}
    }

    private void login(String user, String pwd) {
        if (user.isBlank() || pwd.isBlank()) {
            pop.mostrarAlertaError(b.getString("common.error"), b.getString("login.alert.error.fillFields"));
            return;
        }
        try {
            JsonNode body = mapper.createObjectNode().put("username", user).put("password", pwd);

            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/login")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();

            http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(resp -> {
                if (resp.statusCode() == 200) {
                    handleLoginSuccess(resp.body(), user, pwd);
                } else {
                    Platform.runLater(() -> pop.mostrarAlertaError(b.getString("common.error"), b.getString("login.alert.error.invalidCredentials")));
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void handleLoginSuccess(String respBody, String user, String pwd) {
        try {
            JsonNode root = mapper.readTree(respBody);
            String salt = root.get("salt").asText();
            String pubB64 = root.get("publicKeyBase64").asText();
            String privEnc = root.get("privateKeyEncryptedBase64").asText();
            boolean requireChange = root.get("requirePasswordChange").asBoolean();
            String savedTheme = svc.getTheme(user);
            ThemeManager.getInstance().setTheme(savedTheme);

            SecretKey aesKey = deriveAesKeyFromPassword(pwd, salt);
            String privB64 = AESUtils.decrypt(privEnc, aesKey);
            PrivateKey privKey = RSAUtils.privateKeyFromBase64(privB64);
            PublicKey pubKey = RSAUtils.publicKeyFromBase64(pubB64);
            KeyManager.setPrivateKey(privKey);
            KeyManager.setPublicKey(pubKey);

            LocaleManager.initializeForUser(user);


            if (requireChange) {
                Platform.runLater(() -> showForceChangeDialog(user, pwd));
            } else {
                Platform.runLater(() -> {
                    pop.mostrarAlertaInformativa(b.getString("common.success"), MessageFormat.format(b.getString("login.alert.info.welcome"), user));
                    primaryStage.close();
                    try {
                        new MainInboxWindow(user, springCtx).start(new Stage());
                    } catch (Exception ignore) {
                    }
                });
            }
        } catch (Exception ex) {
            Platform.runLater(() -> pop.mostrarAlertaError(b.getString("common.error"), b.getString("login.alert.error.serverResponse")));
        }
    }

    private void showForceChangeDialog(String username, String token) {
        Dialog<Pair<String, String>> dlg = new Dialog<>();
        dlg.setTitle(b.getString("login.dialog.forceChange.title"));
        dlg.setHeaderText(b.getString("login.dialog.forceChange.header"));

        PasswordField txtNew = new PasswordField();
        txtNew.setPromptText(b.getString("login.dialog.forceChange.prompt.new"));
        PasswordField txtConfirm = new PasswordField();
        txtConfirm.setPromptText(b.getString("login.dialog.forceChange.prompt.confirm"));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label(b.getString("login.dialog.forceChange.label.new")), 0, 0);
        grid.add(txtNew, 1, 0);
        grid.add(new Label(b.getString("login.dialog.forceChange.label.confirm")), 0, 1);
        grid.add(txtConfirm, 1, 1);
        dlg.getDialogPane().setContent(grid);

        ButtonType btnSave = new ButtonType(b.getString("login.dialog.forceChange.button.save"), ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);

        Node saveButton = dlg.getDialogPane().lookupButton(btnSave);
        saveButton.setDisable(true);
        txtNew.textProperty().addListener((o, old, ne) -> saveButton.setDisable(ne.isBlank() || !ne.equals(txtConfirm.getText())));
        txtConfirm.textProperty().addListener((o, old, ne) -> saveButton.setDisable(ne.isBlank() || !ne.equals(txtNew.getText())));

        dlg.setResultConverter(bt -> {
            if (bt == btnSave) return new Pair<>(txtNew.getText(), txtConfirm.getText());
            return null;
        });

        dlg.showAndWait().ifPresent(pair -> {
            String newPwd = pair.getKey();
            ObjectNode body = mapper.createObjectNode().put("username", username).put("oldPassword", token).put("newPassword", newPwd);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/change-password")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();

            http.sendAsync(req, HttpResponse.BodyHandlers.discarding()).thenAccept(resp -> Platform.runLater(() -> {
                if (resp.statusCode() == 200) {
                    pop.mostrarAlertaInformativa(b.getString("common.success"), b.getString("login.alert.info.passwordUpdated"));
                    primaryStage.close();
                    try {
                        new MainInboxWindow(username, springCtx).start(new Stage());
                    } catch (Exception ignore) {
                    }
                } else {
                    pop.mostrarAlertaError(b.getString("common.error"), b.getString("login.alert.error.passwordUpdateFailed"));
                }
            }));
        });
    }

    private SecretKey deriveAesKeyFromPassword(String pwd, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pwd.toCharArray(), salt.getBytes(), 65_536, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = skf.generateSecret(spec).getEncoded();
            return AESUtils.getKeyFromBytes(keyBytes);
        } catch (InvalidKeySpecException | java.security.NoSuchAlgorithmException ex) {
            throw new RuntimeException("PBKDF2 failure", ex);
        }
    }
}
