package windows;

import database.DataBase;
import database.DataBaseQueries;
import managers.PopUpMessages;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controlador para la vista LoginWindow.fxml
 */
public class LoginWindowController {

    // Controles enlazados con el FXML
    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnLogin;

    @FXML
    private Button btnRegister;

    // Objetos de lógica (iguales que en tu código original)
    private final PopUpMessages popUpMessages = new PopUpMessages();
    private final DataBaseQueries dataBaseQueries = new DataBaseQueries();
    private final DataBase db = new DataBase(dataBaseQueries);

    /**
     * Se llama automáticamente después de que el FXML haya cargado,
     * y antes de que la ventana sea visible. Sirve para inicializar componentes.
     */
    @FXML
    public void initialize() {
        // Conexión a la Base de Datos
        db.conectarDataBase();
        dataBaseQueries.crearTablaUsuarios(db.getConnection());

        // Asignar acciones a los botones
        btnLogin.setOnAction(event -> handleLogin());
        btnRegister.setOnAction(event -> handleRegister());
    }

    /**
     * Maneja la lógica de inicio de sesión.
     */
    private void handleLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            popUpMessages.mostrarAlertaError("Error", "Por favor, completa todos los campos.");
        } else if (dataBaseQueries.validarUsuario(username, password, db.getConnection())) {
            popUpMessages.mostrarAlertaInformativa("Éxito", "Inicio de sesión exitoso. ¡Bienvenido, " + username + "!");
            cerrarVentana();
            try {
                new MainInboxWindow(username).start(new Stage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            popUpMessages.mostrarAlertaError("Error", "Usuario o contraseña incorrectos.");
        }
    }

    private void handleRegister() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            popUpMessages.mostrarAlertaError("Error", "Por favor, completa todos los campos.");
        } else if (dataBaseQueries.insertarUsuario(username, password, db.getConnection())) {
            popUpMessages.mostrarAlertaInformativa("Éxito", "Usuario registrado correctamente.");
        } else {
            popUpMessages.mostrarAlertaError("Error", "No se pudo registrar el usuario. Es posible que ya exista.");
        }
    }


    public void cerrarVentana() {
        Stage stage = (Stage) btnLogin.getScene().getWindow();
        stage.close();
        db.desconectarDataBase();
    }
}
