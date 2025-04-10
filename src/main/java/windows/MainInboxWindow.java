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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import managers.Mensaje;
import managers.PopUpMessages;

public class MainInboxWindow extends Application {
    private String currentUser;
    private final PopUpMessages pum = new PopUpMessages();
    private final TableView<Mensaje> tablaMensajes = new TableView<>();
    private final BorderPane mainLayout = new BorderPane(); // Contenedor principal dinámico
    private Stage primaryStage;

    public MainInboxWindow(String currentUser) {
        this.currentUser = currentUser;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    private TCPConnection tcpManager = new TCPConnection();
    private static final int DEFAULT_PORT = 1212; // Definir el puerto por defecto

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("Bandeja de Entrada  -  Anontalk");

        // Configurar la tabla de mensajes
        configurarTablaMensajes();

        // Iniciar el servidor y registrar el listener
        tcpManager.setMessageListener(this::handleIncomingMessage);
        tcpManager.startServer(DEFAULT_PORT);

        Label etBienvenida = new Label("Bienvenido, " + currentUser);
        etBienvenida.getStyleClass().add("label2");

        // Crear el MenuButton para "Bandeja de Entrada"
        MenuButton bandejaEntrada = new MenuButton("Bandeja de entrada");
        bandejaEntrada.getStyleClass().add("bandeja");

        // Crear el MenuItem para "Mensajes enviados"
        MenuItem mensajesEnviados = new MenuItem("Mensajes enviados");
        mensajesEnviados.setOnAction(actionEvent -> mostrarMensajesEnviados());

        // Añadir el MenuItem al MenuButton
        bandejaEntrada.getItems().add(mensajesEnviados);


        mensajesEnviados.setOnAction(actionEvent -> {
            RecievedWindow recievedWindow = new RecievedWindow();
            try {
                recievedWindow.show(currentUser, primaryStage); // Utilitzar el mateix Stage
            } catch (Exception e) {
                pum.mostrarAlertaError("Error", "No s'ha pogut obrir la finestra de Missatges Rebuts.");
                e.printStackTrace();
            }
        });


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

        VBox topBar = new VBox(10);
        topBar.setPadding(new Insets(10));

        HBox topLeftBar = new HBox(etBienvenida);
        topLeftBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getChildren().add(topLeftBar);

        HBox centerBar = new HBox(bandejaEntrada);
        centerBar.setAlignment(Pos.CENTER);
        topBar.getChildren().add(centerBar);

        HBox bottomBar = new HBox(10, btnNuevoMensaje, btnCerrarSesion);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setPadding(new Insets(10));

        // Establecer layout principal
        mainLayout.setTop(topBar);
        mainLayout.setCenter(tablaMensajes);
        mainLayout.setBottom(bottomBar);
        mainLayout.setPadding(new Insets(10));

        Scene scene = new Scene(mainLayout, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    // Mostrar los mensajes enviados en una nueva ventana
    private void mostrarMensajesEnviados() {
        // Aquí puedes implementar la lógica para mostrar los mensajes enviados en una nueva ventana
        System.out.println("Mostrando mensajes enviados...");
    }

    // Manejar mensajes entrantes
    private void handleIncomingMessage(String sender, String message) {
        Platform.runLater(() -> {
            tablaMensajes.getItems().add(new Mensaje(sender, message));
        });
    }

    // Método para configurar la tabla de mensajes
    private void configurarTablaMensajes() {
        tablaMensajes.getColumns().clear();

        // Columna del remitente
        TableColumn<Mensaje, String> colSender = new TableColumn<>("Contacto");
        colSender.setCellValueFactory(new PropertyValueFactory<>("sender"));
        colSender.setResizable(false);
        colSender.setPrefWidth(150);
        colSender.setReorderable(false);

        // Columna dinámica de notificación/mensaje
        TableColumn<Mensaje, String> colContent = new TableColumn<>("Mensajes");
        colContent.setCellFactory(param -> new TableCell<Mensaje, String>() {
            private final Button btnAbrir = new Button("Has recibido un nuevo mensaje, entra para leerlo.");

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                Mensaje mensaje = getTableRow().getItem();
                btnAbrir.getStyleClass().add("btn-abrir");
                btnAbrir.setMaxWidth(Double.MAX_VALUE);
                btnAbrir.setMaxHeight(Double.MAX_VALUE);
                setAlignment(Pos.CENTER);

                btnAbrir.setOnAction(e -> {
                    ChatWindow chatWindow = new ChatWindow(mensaje, tcpManager, DEFAULT_PORT, primaryStage); // Pasar el stage actual
                    chatWindow.show();
                    primaryStage.close(); // Cerrar la bandeja de entrada
                });

                setGraphic(btnAbrir);
                setText(null); // No mostrar texto en la celda
            }
        });
        colContent.setResizable(false);
        colContent.setPrefWidth(550);
        colContent.setReorderable(false);

        tablaMensajes.getColumns().addAll(colSender, colContent);
        tablaMensajes.setPlaceholder(new Label("No hay mensajes en la bandeja."));
    }
}
