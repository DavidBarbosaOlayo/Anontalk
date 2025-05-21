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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Pair;
import managers.PopUpInfo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import security.encryption.AESUtils;
import security.encryption.KeyManager;
import security.encryption.RSAUtils;

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

@SpringBootApplication(scanBasePackages = {
        "windows",
        "managers.mensajes",
        "managers.users",
        "security.passwords"
})
@EnableJpaRepositories(basePackages = {
        "managers.mensajes",
        "managers.users",
        "security.passwords"
})
@EntityScan(basePackages = {
        "managers.mensajes",
        "managers.users",
        "security.passwords"
})
public class LoginWindow extends Application {

    private final ConfigurableApplicationContext springCtx;
    private final PopUpInfo pop = new PopUpInfo();
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private Stage primaryStage;

    /** Recibe el contexto de Spring arrancado en el splash */
    public LoginWindow(ConfigurableApplicationContext ctx) {
        this.springCtx = ctx;
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Login - Anontalk");
        stage.setOnCloseRequest(e -> {
            springCtx.close();
            Platform.exit();
            System.exit(0);
        });

        // --- Construcción de la UI (igual que antes) ---
        Label lblTitle = new Label("ANONTALK");
        lblTitle.getStyleClass().add("welcome-label");
        GridPane.setHalignment(lblTitle, HPos.CENTER);

        TextField txtUser = new TextField();
        txtUser.setPromptText("Usuario");
        PasswordField txtPwd = new PasswordField();
        txtPwd.setPromptText("Contraseña");
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Email");

        Button btnLogin = new Button("Iniciar Sesión");
        Button btnReg   = new Button("Registrarse");
        Hyperlink lnkForgot = new Hyperlink("¿Has olvidado tu contraseña?");

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
        grid.add(new Label("Usuario:"), 0, 1);
        grid.add(txtUser, 1, 1);
        grid.add(new Label("Contraseña:"), 0, 2);
        grid.add(txtPwd, 1, 2);
        grid.add(new Label("Email:"), 0, 3);
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
        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private void forgotPasswordDialog() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Recuperar contraseña");
        dlg.setHeaderText("Introduce el correo asociado a tu cuenta:");
        dlg.setContentText("Email:");

        dlg.showAndWait().ifPresent(email -> {
            if (email.isBlank()) {
                pop.mostrarAlertaError("Error", "El email no puede estar vacío.");
                return;
            }
            try {
                JsonNode body = mapper.createObjectNode().put("email", email.trim());
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/forgot-password")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();

                http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(resp -> Platform.runLater(() -> {
                    switch (resp.statusCode()) {
                        case 200 ->
                                pop.mostrarAlertaInformativa("Email enviado", "Revisa tu bandeja en " + email.trim() + " para recuperar tu contraseña.");
                        case 404 ->
                                pop.mostrarAlertaError("Email no registrado", "No existe cuenta asociada a " + email.trim());
                        default ->
                                pop.mostrarAlertaError("Error", "No se pudo solicitar la recuperación. Intenta más tarde.");
                    }
                }));
            } catch (Exception ex) {
                pop.mostrarAlertaError("Error", "Falló la solicitud de recuperación.");
            }
        });
    }


    private void register(String user, String pwd, String email) {
        if (user.isBlank() || pwd.isBlank() || email.isBlank()) {
            pop.mostrarAlertaError("Error", "Completa todos los campos.");
            return;
        }
        try {
            JsonNode body = mapper.createObjectNode().put("username", user).put("password", pwd).put("email", email);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/register")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();

            http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(resp -> Platform.runLater(() -> {
                if (resp.statusCode() == 200) pop.mostrarAlertaInformativa("Éxito", "Usuario registrado.");
                else if (resp.statusCode() == 409) pop.mostrarAlertaError("Error", "Usuario o email ya registrado.");
                else pop.mostrarAlertaError("Error", "Registro fallido.");
            }));
        } catch (Exception ignored) {
        }
    }

    private void login(String user, String pwd) {
        if (user.isBlank() || pwd.isBlank()) {
            pop.mostrarAlertaError("Error", "Completa todos los campos.");
            return;
        }
        try {
            JsonNode body = mapper.createObjectNode().put("username", user).put("password", pwd);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/login")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();

            http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(resp -> {
                if (resp.statusCode() == 200) {
                    handleLoginSuccess(resp.body(), user, pwd);
                } else {
                    Platform.runLater(() -> pop.mostrarAlertaError("Error", "Usuario o contraseña incorrectos."));
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

            SecretKey aesKey = deriveAesKeyFromPassword(pwd, salt);
            String privB64 = AESUtils.decrypt(privEnc, aesKey);
            PrivateKey privKey = RSAUtils.privateKeyFromBase64(privB64);
            PublicKey pubKey = RSAUtils.publicKeyFromBase64(pubB64);
            KeyManager.setPrivateKey(privKey);
            KeyManager.setPublicKey(pubKey);

            if (requireChange) {
                Platform.runLater(() -> showForceChangeDialog(user, pwd));
            } else {
                Platform.runLater(() -> {
                    pop.mostrarAlertaInformativa("Éxito", "Bienvenido, " + user);
                    primaryStage.close();
                    try {
                        // ← Aquí pasamos springCtx al constructor de MainInboxWindow
                        new MainInboxWindow(user, springCtx).start(new Stage());
                    } catch (Exception ignore) {
                    }
                });
            }
        } catch (Exception ex) {
            Platform.runLater(() -> pop.mostrarAlertaError("Error", "No se pudo procesar la respuesta del servidor."));
        }
    }


    /**
     * Diálogo modal que pide nueva contraseña y la envía al endpoint change-password
     */
    private void showForceChangeDialog(String username, String token) {
        Dialog<Pair<String, String>> dlg = new Dialog<>();
        dlg.setTitle("Cambiar contraseña temporal");
        dlg.setHeaderText("Has iniciado con un token. Por seguridad, elige una nueva contraseña.");

        // campos
        PasswordField txtNew = new PasswordField();
        txtNew.setPromptText("Nueva contraseña");
        PasswordField txtConfirm = new PasswordField();
        txtConfirm.setPromptText("Repetir contraseña");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Nueva:"), 0, 0);
        grid.add(txtNew, 1, 0);
        grid.add(new Label("Repetir:"), 0, 1);
        grid.add(txtConfirm, 1, 1);
        dlg.getDialogPane().setContent(grid);

        ButtonType btnSave = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);

        // validación inline
        Node saveButton = dlg.getDialogPane().lookupButton(btnSave);
        saveButton.setDisable(true);
        txtNew.textProperty().addListener((observable, oldValue, newValue) -> saveButton.setDisable(newValue.isBlank() || !newValue.equals(txtConfirm.getText())));

        txtConfirm.textProperty().addListener((observable, oldValue, newValue) -> saveButton.setDisable(newValue.isBlank() || !newValue.equals(txtNew.getText())));

        dlg.setResultConverter(bt -> {
            if (bt == btnSave) return new Pair<>(txtNew.getText(), txtConfirm.getText());
            return null;
        });

        dlg.showAndWait().ifPresent(pair -> {
            String newPwd = pair.getKey();
            // Llamada async a change-password
            ObjectNode body = mapper.createObjectNode().put("username", username).put("oldPassword", token).put("newPassword", newPwd);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/change-password")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            http.sendAsync(req, HttpResponse.BodyHandlers.discarding()).thenAccept(resp -> Platform.runLater(() -> {
                if (resp.statusCode() == 200) {
                    pop.mostrarAlertaInformativa("Éxito", "Contraseña actualizada.");
                    // abrir bandeja
                    primaryStage.close();
                    try {
                        new MainInboxWindow(username, springCtx).start(new Stage());
                    } catch (Exception ignore) {
                    }
                } else {
                    pop.mostrarAlertaError("Error", "No se pudo cambiar la contraseña.");
                }
            }));
        });
    }

    private SecretKey deriveAesKeyFromPassword(String pwd, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pwd.toCharArray(), salt.getBytes(), 65_536, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] bytes = skf.generateSecret(spec).getEncoded();
            return AESUtils.getKeyFromBytes(bytes);
        } catch (InvalidKeySpecException | java.security.NoSuchAlgorithmException ex) {
            throw new RuntimeException("PBKDF2 failure", ex);
        }
    }
}
