package windows;

import database.DataBaseQueries;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import database.DataBase;
import managers.PopUpMessages;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import security.AESUtils;

import javax.crypto.SecretKey;

@SpringBootApplication
public class LoginWindow extends Application {
    private final PopUpMessages popUpMessages = new PopUpMessages();
    private final DataBaseQueries dataBaseQueries = new DataBaseQueries();
    private final DataBase db = new DataBase(dataBaseQueries);

    @Override
    public void start(Stage primaryStage) {
        // Al inicio de tu programa (ej. en main)...
        AESUtils.createAndStoreKeyIfNotExist();
        SecretKey appAESKey = AESUtils.loadKey();

        primaryStage.setTitle("Login - Anontalk");
        db.conectarDataBase();
        dataBaseQueries.crearTablaUsuarios(db.getConnection());

        Label lblUsername = new Label("Usuario:");
        TextField txtUsername = new TextField();

        Label lblPassword = new Label("Contraseña:");
        PasswordField txtPassword = new PasswordField();

        Button btnLogin = new Button("Iniciar Sesión");
        Button btnRegister = new Button("Registrarse");

        btnLogin.setOnAction(e -> {
            String username = txtUsername.getText();
            String password = txtPassword.getText();
            if (username.isEmpty() || password.isEmpty()) {
                popUpMessages.mostrarAlertaError("Error", "Por favor, completa todos los campos.");
            } else if (dataBaseQueries.validarUsuario(username, password, db.getConnection())) {
                popUpMessages.mostrarAlertaInformativa("Éxito", "Inicio de sesión exitoso. ¡Bienvenido, " + username + "!");
                primaryStage.close(); // Cerrar la ventana de login
                try {
                    new MainInboxWindow(username).start(new Stage()); // Abrir la bandeja de entrada
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                popUpMessages.mostrarAlertaError("Error", "Usuario o contraseña incorrectos.");
            }
        });


        btnRegister.setOnAction(e -> {
            String username = txtUsername.getText();
            String password = txtPassword.getText();
            if (username.isEmpty() || password.isEmpty()) {
                popUpMessages.mostrarAlertaError("Error", "Por favor, completa todos los campos.");
            } else if (dataBaseQueries.insertarUsuario(username, password, db.getConnection())) {
                popUpMessages.mostrarAlertaInformativa("Éxito", "Usuario registrado correctamente.");
            } else {
                popUpMessages.mostrarAlertaError("Error", "No se pudo registrar el usuario. Es posible que ya exista.");
            }
        });

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 20, 20));

        grid.add(lblUsername, 0, 0);
        grid.add(txtUsername, 1, 0);
        grid.add(lblPassword, 0, 1);
        grid.add(txtPassword, 1, 1);
        grid.add(btnLogin, 0, 2);
        grid.add(btnRegister, 1, 2);

        Scene scene = new Scene(grid, 400, 250);
        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> db.desconectarDataBase());
        primaryStage.show();
    }

    public static void main(String[] args) {
        new Thread(() -> SpringApplication.run(LoginWindow.class, args)).start();
        launch(args);
    }
}
