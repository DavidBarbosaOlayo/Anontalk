package windows;

import connections.TCPConnection;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import managers.PopUpMessages;

public class MainInboxWindow extends Application {
    private String currentUser;
    private final PopUpMessages pum = new PopUpMessages();

    public MainInboxWindow(String currentUser) {
        this.currentUser = currentUser;
    }

    private TCPConnection tcpManager = new TCPConnection();

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Bandeja de Entrada  -  Anontalk");

        tcpManager.startServer(1212); // Iniciar el servidor en el puerto 12345

        Label etBienvenida = new Label("Bienvenido, " + currentUser);
        etBienvenida.getStyleClass().add("label2");

        TableView<String> tablaMensajes = new TableView<>();
        tablaMensajes.setPlaceholder(new Label("No hay mensajes en la bandeja."));

        Button btnNuevoMensaje = new Button("Nuevo Mensaje");
        Button btnCerrarSesion = new Button("Cerrar Sesión");

        btnNuevoMensaje.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Enviar Mensaje");
            dialog.setHeaderText("Introduce la IP, el puerto y el mensaje (formato: IP:puerto:mensaje)");
            dialog.setContentText("Formato: ");
            dialog.showAndWait().ifPresent(input -> {
                try {
                    String[] parts = input.split(":");
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    String message = parts[2];
                    tcpManager.sendMessage(host, port, message);
                } catch (Exception ex) {
                    pum.mostrarAlertaError("Error", "Formato incorrecto. Usa: IP:puerto:mensaje");
                }
            });
        });

        btnCerrarSesion.setOnAction(e -> {
            tcpManager.stopServer(); // Detener el servidor
            stage.close();
            pum.mostrarAlertaInformativa("Cerrar Sesión", "Has cerrado sesión correctamente.");
            new LoginWindow().start(new Stage());
        });

        // Layout
        HBox topBar = new HBox(etBienvenida);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10));
        etBienvenida.getStyleClass().add("top-bar-label");

        HBox bottomBar = new HBox(10, btnNuevoMensaje, btnCerrarSesion);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setPadding(new Insets(10));

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(topBar);
        mainLayout.setCenter(tablaMensajes);
        mainLayout.setBottom(bottomBar);
        mainLayout.setPadding(new Insets(10));

        Scene scene = new Scene(mainLayout, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

}
