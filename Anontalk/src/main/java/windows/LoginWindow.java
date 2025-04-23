package windows;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import managers.PopUpInfo;
import security.AESUtils;
import security.KeyManager;
import security.RSAUtils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

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
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


/**
 * Ventana de login/registro.
 * Al hacer login, descarga tu clave pública y privada (AES+RSA),
 * descifra la clave privada y la guarda en KeyManager.
 */
@SpringBootApplication(scanBasePackages = {"windows",               // UI JavaFX
        "managers.mensajes",     // mensajes: entidad, repo, service
        "managers.users"         // usuarios: User, UserRepo, UserService
})
@EnableJpaRepositories(basePackages = {"managers.mensajes", "managers.users"})
@EntityScan(basePackages = {"managers.mensajes", "managers.users"})
public class LoginWindow extends Application {
    private static ConfigurableApplicationContext springContext;
    private final PopUpInfo popUp = new PopUpInfo();
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        // 1) Arrancar Spring Boot
        springContext = SpringApplication.run(LoginWindow.class, args);
        // 2) Luego JavaFX
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Login - Anontalk");
        primaryStage.setOnCloseRequest(e -> {
            // Al cerrar: para Spring y sale
            springContext.close();
            Platform.exit();
            System.exit(0);
        });

        // --- Controles UI ---
        Label lblUser = new Label("Usuario:");
        TextField txtUser = new TextField();
        Label lblPwd = new Label("Contraseña:");
        PasswordField txtPwd = new PasswordField();

        Button btnLogin = new Button("Iniciar Sesión");
        Button btnReg = new Button("Registrarse");

        // --- Acción LOGIN ---
        btnLogin.setOnAction(e -> {
            String user = txtUser.getText().trim();
            String pwd = txtPwd.getText();
            if (user.isBlank() || pwd.isBlank()) {
                popUp.mostrarAlertaError("Error", "Completa todos los campos");
                return;
            }
            // Construir body JSON
            ObjectNode body = mapper.createObjectNode().put("username", user).put("password", pwd);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/login")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();

            client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(resp -> {
                if (resp.statusCode() == 200) {
                    try {
                        // Parsear respuesta: salt + claves en Base64
                        JsonNode root = mapper.readTree(resp.body());
                        String salt = root.get("salt").asText();
                        String pubB64 = root.get("publicKeyBase64").asText();
                        String privEnc = root.get("privateKeyEncryptedBase64").asText();

                        // 1) Derivar clave AES de pwd+salt (PBKDF2)
                        SecretKey aesKey = deriveAesKeyFromPassword(pwd, salt);

                        // 2) Descifrar la privateKey RSA
                        System.out.println("privEnc raw: →'" + privEnc + "'← (len=" + privEnc.length() + ")");
                        byte[] dec = Base64.getDecoder().decode(privEnc);
                        System.out.println("decoded bytes: len=" + dec.length + ", dec.length%16="+(dec.length%16));

                        String privB64 = AESUtils.decrypt(privEnc, aesKey);
                        PrivateKey privKey = RSAUtils.privateKeyFromBase64(privB64);

                        // 3) Reconstruir la publicKey
                        PublicKey pubKey = RSAUtils.publicKeyFromBase64(pubB64);

                        // 4) Guardar en KeyManager (singleton)
                        KeyManager.setPrivateKey(privKey);
                        KeyManager.setPublicKey(pubKey);

                        // 5) Abrir bandeja
                        Platform.runLater(() -> {
                            popUp.mostrarAlertaInformativa("Éxito", "Bienvenido, " + user);
                            primaryStage.close();
                            try {
                                new MainInboxWindow(user).start(new Stage());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Platform.runLater(() -> popUp.mostrarAlertaError("Error", "No se pudo procesar la respuesta de login"));
                    }
                } else {
                    Platform.runLater(() -> popUp.mostrarAlertaError("Error", "Usuario o contraseña incorrectos"));
                }
            });
        });

        // --- Acción REGISTER ---
        btnReg.setOnAction(e -> {
            String user = txtUser.getText().trim();
            String pwd = txtPwd.getText();
            if (user.isBlank() || pwd.isBlank()) {
                popUp.mostrarAlertaError("Error", "Completa todos los campos");
                return;
            }
            ObjectNode body = mapper.createObjectNode().put("username", user).put("password", pwd);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/users/register")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();

            client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(resp -> {
                int status = resp.statusCode();
                String rb = resp.body();
                Platform.runLater(() -> {
                    if (status == 200) {
                        popUp.mostrarAlertaInformativa("Éxito", "Usuario registrado correctamente");
                    } else if (status == 409) {
                        popUp.mostrarAlertaError("Error", "Ya existe un usuario con ese nombre");
                    } else if (status == 400) {
                        try {
                            JsonNode err = mapper.readTree(rb);
                            String msg = err.path("message").asText("Error en los datos de registro");
                            popUp.mostrarAlertaError("Error", msg);
                        } catch (Exception ex) {
                            popUp.mostrarAlertaError("Error", "Datos inválidos");
                        }
                    } else {
                        popUp.mostrarAlertaError("Error", "Ha ocurrido un error inesperado");
                    }
                });
            });
        });

        // --- Layout ---
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(lblUser, 0, 0);
        grid.add(txtUser, 1, 0);
        grid.add(lblPwd, 0, 1);
        grid.add(txtPwd, 1, 1);
        grid.add(btnLogin, 0, 2);
        grid.add(btnReg, 1, 2);

        Scene scene = new Scene(grid, 400, 250);
        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * PBKDF2: deriva una clave AES de la contraseña y el salt
     */
    private SecretKey deriveAesKeyFromPassword(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = skf.generateSecret(spec).getEncoded();
            return AESUtils.getKeyFromBytes(keyBytes);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new RuntimeException("Error al derivar clave AES", ex);
        }
    }
}
