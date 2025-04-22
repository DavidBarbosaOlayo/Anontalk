package windows;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import managers.PopUpInfo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@SpringBootApplication(
        scanBasePackages = {
                "windows",                // tu UI JavaFX
                "managers.mensajes",      // mensajes: entidad, repo, service
                "managers.users"          // usuarios: User, UserRepo, UserService
        }
)
@EnableJpaRepositories(basePackages = {
        "managers.mensajes",      // JPA repositorio de mensajes
        "managers.users"          // JPA repositorio de usuarios
})
@EntityScan(basePackages = {
        "managers.mensajes",      // entidades de mensajes
        "managers.users"          // entidades de usuarios
})
public class LoginWindow extends Application {
    private static ConfigurableApplicationContext springContext;
    private final PopUpInfo popUp = new PopUpInfo();
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        // 1) Arrancamos Spring Boot (crea tablas en PostgreSQL/Render)
        springContext = SpringApplication.run(LoginWindow.class, args);
        // 2) Luego lanzamos JavaFX
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Login - Anontalk");

        // Al cerrar la ventana: detenemos Spring y salimos
        primaryStage.setOnCloseRequest(e -> {
            springContext.close();
            Platform.exit();
            System.exit(0);
        });

        Label lblUser = new Label("Usuario:");
        TextField txtUser = new TextField();
        Label lblPwd = new Label("Contraseña:");
        PasswordField txtPwd = new PasswordField();

        Button btnLogin = new Button("Iniciar Sesión");
        Button btnReg   = new Button("Registrarse");

        btnLogin.setOnAction(e -> {
            if (txtUser.getText().isBlank() || txtPwd.getText().isBlank()) {
                popUp.mostrarAlertaError("Error", "Completa todos los campos");
                return;
            }
            ObjectNode body = mapper.createObjectNode()
                    .put("username", txtUser.getText().trim())
                    .put("password", txtPwd.getText());
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/users/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            client.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(resp -> {
                        if (resp.statusCode() == 200) {
                            Platform.runLater(() -> {
                                popUp.mostrarAlertaInformativa("Éxito", "Bienvenido, " + txtUser.getText());
                                primaryStage.close();
                                try {
                                    new MainInboxWindow(txtUser.getText().trim()).start(new Stage());
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            });
                        } else {
                            Platform.runLater(() ->
                                    popUp.mostrarAlertaError("Error", "Usuario o contraseña incorrectos")
                            );
                        }
                    });
        });

        btnReg.setOnAction(e -> {
            if (txtUser.getText().isBlank() || txtPwd.getText().isBlank()) {
                popUp.mostrarAlertaError("Error", "Completa todos los campos");
                return;
            }
            ObjectNode body = mapper.createObjectNode()
                    .put("username", txtUser.getText().trim())
                    .put("password", txtPwd.getText());
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/users/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            client.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(resp -> {
                        if (resp.statusCode() == 200) {
                            Platform.runLater(() ->
                                    popUp.mostrarAlertaInformativa("Éxito", "Usuario registrado")
                            );
                        } else {
                            Platform.runLater(() ->
                                    popUp.mostrarAlertaError("Error", "El usuario ya existe")
                            );
                        }
                    });
        });

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
        grid.add(btnReg,   1, 2);

        Scene sc = new Scene(grid, 400, 250);
        sc.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        primaryStage.setScene(sc);
        primaryStage.show();
    }
}
