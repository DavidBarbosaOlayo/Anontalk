package windows;

import connections.TCPController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import managers.MessageStore;
import managers.Mensaje;
import managers.PopUpMessages;
import java.time.LocalDateTime;

public class ChatWindow {
    private static final int DEFAULT_PORT = 1212;
    private final Mensaje mensaje;
    private final int port;
    private final PopUpMessages pum = new PopUpMessages();
    // Se asume que se usa siempre la misma instancia de TCPController.
    private final TCPController tcpController;
    private final Stage parentStage;

    public ChatWindow(Mensaje mensaje, TCPController tcpController, int port, Stage parentStage) {
        this.mensaje = mensaje;
        this.tcpController = tcpController;
        this.port = port;
        this.parentStage = parentStage;
    }

    public void show() {
        Stage chatStage = new Stage();
        chatStage.setTitle("Chat con " + mensaje.getSender());

        // Encabezado: muestra remitente y fecha.
        Label lblHeader = new Label("De: " + mensaje.getSender() + "  |  Fecha: " + LocalDateTime.now());
        lblHeader.getStyleClass().add("chat-header");

        // Área de lectura del mensaje.
        Label lblMensaje = new Label(mensaje.getContent());
        lblMensaje.setWrapText(true);
        lblMensaje.getStyleClass().add("chat-message-body");

        ScrollPane scrollMessage = new ScrollPane(lblMensaje);
        scrollMessage.setFitToWidth(true);
        scrollMessage.getStyleClass().add("chat-scrollpane");

        // Área de redacción del mensaje.
        TextArea txtRespuesta = new TextArea();
        txtRespuesta.setPromptText("Redacta tu respuesta...");
        txtRespuesta.setPrefRowCount(4);
        txtRespuesta.getStyleClass().add("chat-textarea");

        // Barra de herramientas (ejemplo: botón para negrita).
        HBox toolbar = new HBox(10);
        Button btnBold = new Button("B");
        btnBold.setOnAction(e -> {
            txtRespuesta.appendText(" **Texto en negrita** ");
        });
        toolbar.getChildren().add(btnBold);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("chat-toolbar");

        VBox composeArea = new VBox(5, toolbar, txtRespuesta);
        composeArea.setPadding(new Insets(10));
        composeArea.getStyleClass().add("chat-compose-area");

        // Botones de acción.
        Button btnEnviar = new Button("Enviar");
        btnEnviar.setOnAction(e -> {
            String respuesta = txtRespuesta.getText().trim();
            if (!respuesta.isEmpty()) {
                // Se envía el mensaje cifrado usando TCPController.
                tcpController.sendMessage(mensaje.getSender(), port, respuesta);
                MessageStore.sentMessages.add(new Mensaje("Tú", respuesta));
                pum.mostrarAlertaInformativa("Mensaje enviado", "Tu respuesta ha sido enviada.");
                chatStage.close();
                parentStage.show();
            } else {
                pum.mostrarAlertaError("Error", "No puedes enviar un mensaje vacío.");
            }
        });

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setOnAction(e -> {
            chatStage.close();
            parentStage.show();
        });

        HBox buttonBar = new HBox(10, btnEnviar, btnCerrar);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10));

        BorderPane chatLayout = new BorderPane();
        chatLayout.setTop(lblHeader);
        chatLayout.setCenter(scrollMessage);
        chatLayout.setBottom(new VBox(composeArea, buttonBar));
        chatLayout.setPadding(new Insets(10));
        chatLayout.getStyleClass().add("chat-root");

        Scene scene = new Scene(chatLayout, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        chatStage.setScene(scene);
        chatStage.show();
    }
}
