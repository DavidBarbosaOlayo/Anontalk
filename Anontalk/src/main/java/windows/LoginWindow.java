package windows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
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

    private static ConfigurableApplicationContext springCtx;
    private final PopUpInfo pop = new PopUpInfo();
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private Stage primaryStage;

    public static void main(String[] args) {
        springCtx = SpringApplication.run(LoginWindow.class, args);
        launch(args);
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

        // Campos
        TextField txtUser  = new TextField();
        txtUser.setPromptText("Usuario");
        PasswordField txtPwd = new PasswordField();
        txtPwd.setPromptText("Contraseña");
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Email");

        Button btnLogin = new Button("Iniciar Sesión");
        Button btnReg   = new Button("Registrarse");
        Hyperlink lnkForgot = new Hyperlink("¿Has olvidado tu contraseña?");

        // Acciones
        btnLogin.setOnAction(e -> login(txtUser.getText().trim(), txtPwd.getText().trim()));
        btnReg  .setOnAction(e -> register(
                txtUser.getText().trim(),
                txtPwd.getText().trim(),
                txtEmail.getText().trim()
        ));
        lnkForgot.setOnAction(e -> forgotPasswordDialog());

        // Layout
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.addRow(0, new Label("Usuario:"), txtUser);
        grid.addRow(1, new Label("Contraseña:"), txtPwd);
        grid.addRow(2, new Label("Email:"), txtEmail);
        grid.addRow(3, btnLogin, btnReg);
        grid.add(lnkForgot, 1, 4);
        GridPane.setMargin(lnkForgot, new Insets(10, 0, 0, 0));

        Scene scene = new Scene(grid, 450, 350);
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
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/users/forgot-password"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build();

                http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(resp -> Platform.runLater(() -> {
                            switch (resp.statusCode()) {
                                case 200 -> pop.mostrarAlertaInformativa(
                                        "Email enviado",
                                        "Revisa tu bandeja en " + email.trim() +
                                                " para recuperar tu contraseña."
                                );
                                case 404 -> pop.mostrarAlertaError(
                                        "Email no registrado",
                                        "No existe cuenta asociada a " + email.trim()
                                );
                                default -> pop.mostrarAlertaError(
                                        "Error",
                                        "No se pudo solicitar la recuperación. Intenta más tarde."
                                );
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
                        if (resp.statusCode() == 200)
                            pop.mostrarAlertaInformativa("Éxito", "Usuario registrado.");
                        else if (resp.statusCode() == 409)
                            pop.mostrarAlertaError("Error", "Usuario o email ya registrado.");
                        else
                            pop.mostrarAlertaError("Error", "Registro fallido.");
                    }));
        } catch (Exception ignored) { }
    }

    private void login(String user, String pwd) {
        if (user.isBlank() || pwd.isBlank()) {
            pop.mostrarAlertaError("Error", "Completa todos los campos.");
            return;
        }
        try {
            JsonNode body = mapper.createObjectNode()
                    .put("username", user)
                    .put("password", pwd);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/users/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(resp -> {
                        if (resp.statusCode() == 200) {
                            handleLoginSuccess(resp.body(), user, pwd);
                        } else {
                            Platform.runLater(() ->
                                    pop.mostrarAlertaError("Error", "Usuario o contraseña incorrectos."));
                        }
                    });
        } catch (Exception ignored) { }
    }

    private void handleLoginSuccess(String respBody, String user, String pwd) {
        try {
            JsonNode root = mapper.readTree(respBody);
            String salt    = root.get("salt").asText();
            String pubB64  = root.get("publicKeyBase64").asText();
            String privEnc = root.get("privateKeyEncryptedBase64").asText();

            SecretKey aesKey = deriveAesKeyFromPassword(pwd, salt);
            String privB64 = AESUtils.decrypt(privEnc, aesKey);
            PrivateKey privKey = RSAUtils.privateKeyFromBase64(privB64);
            PublicKey  pubKey  = RSAUtils.publicKeyFromBase64(pubB64);

            KeyManager.setPrivateKey(privKey);
            KeyManager.setPublicKey(pubKey);

            Platform.runLater(() -> {
                pop.mostrarAlertaInformativa("Éxito", "Bienvenido, " + user);
                primaryStage.close();
                try { new MainInboxWindow(user).start(new Stage()); }
                catch (Exception ignore) { }
            });
        } catch (Exception ex) {
            Platform.runLater(() ->
                    pop.mostrarAlertaError("Error", "No se pudo procesar la respuesta del servidor."));
        }
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
