// src/main/java/windows/LoginWindow.java
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
import security.AESUtils;
import security.KeyManager;
import security.RSAUtils;

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
        "managers.users"
})
@EnableJpaRepositories(basePackages = {"managers.mensajes", "managers.users"})
@EntityScan(basePackages = {"managers.mensajes", "managers.users"})
public class LoginWindow extends Application {

    /* -------- estado -------- */
    private static ConfigurableApplicationContext springCtx;
    private final PopUpInfo pop = new PopUpInfo();
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private Stage primaryStage;        // ← referencia para cerrar la ventana

    public static void main(String[] args) {
        springCtx = SpringApplication.run(LoginWindow.class, args);
        launch(args);
    }

    /* =========================================================== */

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;                 // ← guardamos la referencia
        stage.setTitle("Login - Anontalk");
        stage.setOnCloseRequest(e -> {
            springCtx.close();
            Platform.exit();
            System.exit(0);
        });

        /* ---------- UI ---------- */
        TextField txtUser = new TextField();
        PasswordField txtPwd = new PasswordField();

        Button btnLogin = new Button("Iniciar Sesión");
        Button btnReg   = new Button("Registrarse");

        btnLogin.setOnAction(e -> login(txtUser.getText().trim(), txtPwd.getText()));
        btnReg.setOnAction(e -> register(txtUser.getText().trim(), txtPwd.getText()));

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        grid.addRow(0, new Label("Usuario:"), txtUser);
        grid.addRow(1, new Label("Contraseña:"), txtPwd);
        grid.addRow(2, btnLogin, btnReg);

        Scene scene = new Scene(grid, 400, 250);
        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    /* =========================================================== */
    /*  Registro  */
    /* =========================================================== */

    private void register(String user, String pwd) {
        if (user.isBlank() || pwd.isBlank()) {
            pop.mostrarAlertaError("Error", "Completa todos los campos.");
            return;
        }
        try {
            JsonNode body = mapper.createObjectNode().put("username", user).put("password", pwd);
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
                            pop.mostrarAlertaError("Error", "Ya existe ese usuario.");
                        else
                            pop.mostrarAlertaError("Error", "Registro fallido.");
                    }));
        } catch (Exception ignored) { }
    }

    /* =========================================================== */
    /*  Login  */
    /* =========================================================== */

    private void login(String user, String pwd) {
        if (user.isBlank() || pwd.isBlank()) {
            pop.mostrarAlertaError("Error", "Completa todos los campos.");
            return;
        }
        try {
            JsonNode body = mapper.createObjectNode().put("username", user).put("password", pwd);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/users/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(resp -> {
                        if (resp.statusCode() == 200) {
                            try {
                                JsonNode root = mapper.readTree(resp.body());
                                String salt    = root.get("salt").asText();
                                String pubB64  = root.get("publicKeyBase64").asText();
                                String privEnc = root.get("privateKeyEncryptedBase64").asText();

                                /* --- derivar AES y descifrar privateKey --- */
                                SecretKey aesKey = deriveAesKeyFromPassword(pwd, salt);
                                String privB64 = AESUtils.decrypt(privEnc, aesKey);   // AES-GCM helper
                                PrivateKey privKey = RSAUtils.privateKeyFromBase64(privB64);
                                PublicKey  pubKey  = RSAUtils.publicKeyFromBase64(pubB64);

                                KeyManager.setPrivateKey(privKey);
                                KeyManager.setPublicKey(pubKey);

                                /* --- abrir bandeja y cerrar login --- */
                                Platform.runLater(() -> {
                                    pop.mostrarAlertaInformativa("Éxito", "Bienvenido, " + user);
                                    primaryStage.close();                       // ← cerramos la ventana
                                    try { new MainInboxWindow(user).start(new Stage()); }
                                    catch (Exception ignored) { }
                                });

                            } catch (Exception ex) {
                                Platform.runLater(() ->
                                        pop.mostrarAlertaError("Error", "No se pudo procesar la respuesta del servidor."));
                            }
                        } else {
                            Platform.runLater(() ->
                                    pop.mostrarAlertaError("Error", "Usuario o contraseña incorrectos."));
                        }
                    });
        } catch (Exception ignored) { }
    }

    /* =========================================================== */
    /*  PBKDF2 helper  */
    /* =========================================================== */

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
