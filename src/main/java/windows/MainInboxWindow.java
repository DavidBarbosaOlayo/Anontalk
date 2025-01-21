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

        // Layout superior e inferior
        HBox topBar = new HBox(etBienvenida);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10));

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
        TableColumn<Mensaje, String> colSender = new TableColumn<>("Remitente");
        colSender.setCellValueFactory(new PropertyValueFactory<>("sender"));
        colSender.setResizable(false);
        colSender.setPrefWidth(150);
        colSender.setReorderable(false);

        // Columna dinámica de notificación/mensaje
        TableColumn<Mensaje, String> colContent = new TableColumn<>("Mensajes");
        colContent.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = new Button("Abrir");

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                Mensaje mensaje = (Mensaje) getTableRow().getItem();

                btnAbrir.setOnAction(e -> mostrarVistaChat(mensaje));
                setGraphic(btnAbrir); // Mostrar el botón
                setText(null); // No mostrar texto
            }
        });
        colContent.setResizable(false);
        colContent.setPrefWidth(550);
        colContent.setReorderable(false);

        tablaMensajes.getColumns().addAll(colSender, colContent);
        tablaMensajes.setPlaceholder(new Label("No hay mensajes en la bandeja."));
    }

    // Mostrar la vista de chat
    private void mostrarVistaChat(Mensaje mensaje) {
        // Etiquetas para mostrar el mensaje
        Label lblRemitente = new Label("De: " + mensaje.getSender());
        lblRemitente.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label lblMensaje = new Label(mensaje.getContent());
        lblMensaje.setWrapText(true);
        lblMensaje.setPadding(new Insets(10));
        lblMensaje.setStyle("-fx-border-color: lightgray; -fx-border-radius: 5; -fx-padding: 10px;");

        // Campo de texto para la respuesta
        TextArea txtRespuesta = new TextArea();
        txtRespuesta.setPromptText("Escribe tu respuesta aquí...");
        txtRespuesta.setPrefRowCount(4);

        // Botones para enviar y volver
        Button btnEnviar = new Button("Enviar");
        btnEnviar.setOnAction(e -> {
            String respuesta = txtRespuesta.getText().trim();
            if (!respuesta.isEmpty()) {
                tcpManager.sendMessage(mensaje.getSender(), DEFAULT_PORT, respuesta);
                pum.mostrarAlertaInformativa("Mensaje enviado", "Tu respuesta ha sido enviada.");
                mostrarVistaBandeja(); // Volver a la bandeja de entrada
            } else {
                pum.mostrarAlertaError("Error", "No puedes enviar un mensaje vacío.");
            }
        });

        Button btnVolver = new Button("Volver");
        btnVolver.setOnAction(e -> mostrarVistaBandeja());

        HBox botones = new HBox(10, btnEnviar, btnVolver);
        botones.setAlignment(Pos.CENTER_RIGHT);
        botones.setPadding(new Insets(10));

        // Layout del chat
        VBox chatLayout = new VBox(10, lblRemitente, lblMensaje, txtRespuesta, botones);
        chatLayout.setPadding(new Insets(10));
        chatLayout.setAlignment(Pos.TOP_LEFT);

        mainLayout.setCenter(chatLayout); // Cambiar el contenido del centro a la vista de chat
    }

    // Volver a la bandeja de entrada
    private void mostrarVistaBandeja() {
        mainLayout.setCenter(tablaMensajes); // Restaurar la tabla de mensajes
    }
}
