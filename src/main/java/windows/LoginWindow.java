package windows;

import database.DataBaseQueries;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import database.DataBase;
import managers.PopUpMessages;

import java.io.IOException;

public class LoginWindow extends Application {
    private final PopUpMessages popUpMessages = new PopUpMessages();
    private final DataBaseQueries dataBaseQueries = new DataBaseQueries();
    private final DataBase db = new DataBase(dataBaseQueries);

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/windows/LoginWindow.fxml"));

        GridPane root = loader.load();

        // Crear la escena con el GridPane
        Scene scene = new Scene(root, 400, 250);

        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());

        primaryStage.setTitle("Login - Anontalk");
        primaryStage.setScene(scene);

               LoginWindowController controller = loader.getController();
        primaryStage.setOnCloseRequest(event -> {
            controller.cerrarVentana();
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
