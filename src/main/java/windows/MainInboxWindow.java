package windows;

import connections.TCPConnection;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import managers.Mensaje;
import managers.PopUpMessages;

public class MainInboxWindow extends Application {
    private String currentUser;
    private final PopUpMessages pum = new PopUpMessages();
    private final TableView<Mensaje> tablaMensajes = new TableView<>();

    public MainInboxWindow(String currentUser) {
        this.currentUser = currentUser;
    }

    private TCPConnection tcpManager = new TCPConnection();
    private static final int DEFAULT_PORT = 1212; // Definir el puerto por defecto

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Bandeja de Entrada  -  Anontalk");

        // Configurar la tabla de mensajes
        configurarTablaMensajes();

        // Iniciar el servidor y registrar el listener
        tcpManager.setMessageListener(this::handleIncomingMessage);
        tcpManager.startServer(DEFAULT_PORT);

        Label etBienvenida = new Label("Bienvenido, " + currentUser);
        etBienvenida.getStyleClass().add("label2");

        Button btnNuevoMensaje = new Button("Nuevo Mensaje");
        Button btnCerrarSesion = new Button("Cerrar Sesión");

        btnNuevoMensaje.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Enviar Mensaje");
            dialog.setHeaderText("Introduce la IP y el mensaje (formato: IP:mensaje)");
            dialog.setContentText("Formato: ");
            dialog.showAndWait().ifPresent(input -> {
                try {
                    String[] parts = input.split(":");
                    String host = parts[0];
                    String message = parts[1];
                    tcpManager.sendMessage(host, DEFAULT_PORT, message);
                } catch (Exception ex) {
                    pum.mostrarAlertaError("Error", "Formato incorrecto. Usa: IP:mensaje");
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

    // Manejar mensajes entrantes
    private void handleIncomingMessage(String sender, String message) {
        Platform.runLater(() -> {
            tablaMensajes.getItems().add(new Mensaje(sender, message));
        });
    }


    // Método para configurar la tabla de mensajes
    private void configurarTablaMensajes() {
        TableColumn<Mensaje, String> colSender = new TableColumn<>("Remitente");
        colSender.setCellValueFactory(new PropertyValueFactory<>("sender"));

        TableColumn<Mensaje, String> colContent = new TableColumn<>("Mensaje");
        colContent.setCellValueFactory(new PropertyValueFactory<>("content"));

        tablaMensajes.getColumns().addAll(colSender, colContent);
        tablaMensajes.setPlaceholder(new Label("No hay mensajes en la bandeja."));
    }

    // Manejar mensajes entrantes

}
