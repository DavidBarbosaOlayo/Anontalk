package managers;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class PopUpInfo {

    private Image appIcon;

    public PopUpInfo() {
        try {
            // Cargar el icono de la aplicación
            appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/logo.png")));
        } catch (Exception e) {
            // Si no se puede cargar el icono, appIcon será null
            appIcon = null;
        }
    }

    public void mostrarAlertaInformativa(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);

        if (appIcon != null) {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(appIcon);
        }

        alert.showAndWait();
    }
    public void mostrarAlertaError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);

        if (appIcon != null) {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(appIcon);
        }
        
        alert.showAndWait();
    }
}
