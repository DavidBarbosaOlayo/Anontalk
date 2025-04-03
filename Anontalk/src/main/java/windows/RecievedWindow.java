package windows;

import connections.TCPController;
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
import managers.MessageStore;
import managers.Mensaje;
import managers.PopUpMessages;

public class RecievedWindow {
    private final PopUpMessages pum = new PopUpMessages();
    private final TableView<Mensaje> tablaMensajes = new TableView<>();
    private final BorderPane mainLayout = new BorderPane();
    private Stage primaryStage;

    private static final int DEFAULT_PORT = 1212;
    private final TCPController tcpController = TCPController.getInstance(DEFAULT_PORT);

    /**
     * Ventana que muestra “Mensajes Enviados”.
     */
    public void show(String currentUser, Stage stage) throws Exception {
        this.primaryStage = stage;
        stage.setTitle("Mensajes Enviados - Anontalk");

        configurarTablaMensajes();

        // Si quisieras ver también mensajes entrantes, podrías:
        // tcpController.addMessageListener(this::handleIncomingMessage);

        Label etBienvenida = new Label("Bienvenido, " + currentUser);
        etBienvenida.getStyleClass().add("label2");

        MenuButton menuMensajes = new MenuButton("Mensajes enviados");
        menuMensajes.getStyleClass().add("mnsjsenviados");

        // Para volver a la bandeja de entrada
        MenuItem bandejaDeEntrada = new MenuItem("Bandeja de entrada");
        bandejaDeEntrada.setOnAction(actionEvent -> {
            MainInboxWindow mainInboxWindow = new MainInboxWindow(currentUser);
            try {
                mainInboxWindow.start(stage);
            } catch (Exception e) {
                pum.mostrarAlertaError("Error", "No se pudo abrir la Bandeja de Entrada.");
                e.printStackTrace();
            }
        });
        menuMensajes.getItems().add(bandejaDeEntrada);

        // Botón para enviar mensaje
        Button btnNuevoMensaje = new Button("Nuevo Mensaje");
        btnNuevoMensaje.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Enviar Mensaje");
            dialog.setHeaderText("Introduce la IP y el mensaje (formato: IP:mensaje)");
            dialog.setContentText("Formato: ");
            dialog.showAndWait().ifPresent(input -> {
                try {
                    String[] parts = input.split(":", 2);
                    String host = parts[0];
                    String mensaje = parts[1];

                    tcpController.sendMessage(host, DEFAULT_PORT, mensaje);
                    MessageStore.sentMessages.add(new Mensaje("Tú", mensaje));

                } catch (Exception ex) {
                    pum.mostrarAlertaError("Error", "Formato incorrecto. Usa: IP:mensaje");
                }
            });
        });

        // Botón para cerrar sesión
        Button btnCerrarSesion = new Button("Cerrar Sesión");
        btnCerrarSesion.setOnAction(e -> {
            tcpController.stopServer();
            stage.close();
            pum.mostrarAlertaInformativa("Cerrar Sesión", "Has cerrado sesión correctamente.");
            new LoginWindow().start(new Stage());
        });

        VBox topBar = new VBox(10);
        topBar.setPadding(new Insets(10));

        HBox topLeftBar = new HBox(etBienvenida);
        topLeftBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getChildren().add(topLeftBar);

        HBox centerBar = new HBox(menuMensajes);
        centerBar.setAlignment(Pos.CENTER);
        topBar.getChildren().add(centerBar);

        HBox bottomBar = new HBox(10, btnNuevoMensaje, btnCerrarSesion);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setPadding(new Insets(10));

        mainLayout.setTop(topBar);
        mainLayout.setCenter(tablaMensajes);
        mainLayout.setBottom(bottomBar);
        mainLayout.setPadding(new Insets(10));

        Scene scene = new Scene(mainLayout, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    // Si quisieras manejar mensajes entrantes aquí también:
    private void handleIncomingMessage(String sender, String message) {
        Platform.runLater(() -> {
            // Podrías guardar en inbox si te interesa
        });
    }

    /**
     * Tabla que muestra la lista de "sentMessages", con un Label clicable
     * en vez de un botón para mantener el estilo original.
     */
    private void configurarTablaMensajes() {
        tablaMensajes.getColumns().clear();

        TableColumn<Mensaje, String> colSender = new TableColumn<>("Contacto");
        colSender.setCellValueFactory(new PropertyValueFactory<>("sender"));
        colSender.setPrefWidth(150);
        colSender.setResizable(false);

        TableColumn<Mensaje, String> colContent = new TableColumn<>("Mensajes");
        colContent.setPrefWidth(550);
        colContent.setResizable(false);

        colContent.setCellFactory(param -> new TableCell<Mensaje, String>() {
            private final Label lblEnviado = new Label("Mensaje Enviado, pulsa para ver.");

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Mensaje mensaje = getTableRow().getItem();

                lblEnviado.getStyleClass().add("lblmensajeEnviado");
                lblEnviado.setOnMouseClicked(e -> {
                    // Abre la ventana de chat
                    ChatWindow chatWindow = new ChatWindow(mensaje, tcpController, DEFAULT_PORT, primaryStage);
                    chatWindow.show();
                    primaryStage.close();
                });
                setGraphic(lblEnviado);
                setText(null);
            }
        });

        tablaMensajes.getColumns().addAll(colSender, colContent);

        // Mostrar la lista de enviados
        tablaMensajes.setItems(MessageStore.sentMessages);
        tablaMensajes.setPlaceholder(new Label("No hay mensajes enviados aún."));
    }
}
