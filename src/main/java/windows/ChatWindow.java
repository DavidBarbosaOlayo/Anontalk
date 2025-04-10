package windows;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import managers.Mensaje;
import managers.PopUpMessages;
import connections.TCPConnection;

public class ChatWindow {
    private final Mensaje mensaje;
    private final int port;
    private final PopUpMessages pum = new PopUpMessages();
    private final TCPConnection tcpManager;
    private final Stage parentStage; // Referencia al stage principal

    public ChatWindow(Mensaje mensaje, TCPConnection tcpManager, int port, Stage parentStage) {
        this.mensaje = mensaje;
        this.tcpManager = tcpManager;
        this.port = port;
        this.parentStage = parentStage; // Guardar la referencia al Stage principal
    }

    public void show() {
        Stage chatStage = new Stage(); // Nueva ventana de chat
        chatStage.setTitle("Chat con " + mensaje.getSender());

        // Configuración de la ventana (igual que antes)
        Label lblRemitente = new Label("Mensaje de: " + mensaje.getSender());
        lblRemitente.getStyleClass().add("lblremitente");

        Label lblMensaje = new Label(mensaje.getSender() + ": " + mensaje.getContent());
        lblMensaje.setWrapText(true);
        lblMensaje.setPadding(new Insets(10));
        lblMensaje.getStyleClass().add("lblmensaje");

        TextArea txtRespuesta = new TextArea();
        txtRespuesta.setPromptText("Escribe tu respuesta aquí...");
        txtRespuesta.setPrefRowCount(4);
        txtRespuesta.getStyleClass().add("txtrespuesta");

        Button btnEnviar = new Button("Enviar");
        btnEnviar.setOnAction(e -> {
            String respuesta = txtRespuesta.getText().trim();
            if (!respuesta.isEmpty()) {
                tcpManager.sendMessage(mensaje.getSender(), port, respuesta);
                pum.mostrarAlertaInformativa("Mensaje enviado", "Tu respuesta ha sido enviada.");
                chatStage.close(); // Cerrar chat
                parentStage.show(); // Mostrar la bandeja de entrada
            } else {
                pum.mostrarAlertaError("Error", "No puedes enviar un mensaje vacío.");
            }
        });

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setOnAction(e -> {
            chatStage.close(); // Cerrar chat
            parentStage.show(); // Mostrar la bandeja de entrada
        });

        HBox botones = new HBox(10, btnEnviar, btnCerrar);
        botones.setAlignment(Pos.CENTER_RIGHT);
        botones.setPadding(new Insets(10));

        VBox chatLayout = new VBox(10, lblRemitente, lblMensaje, txtRespuesta, botones);
        chatLayout.setPadding(new Insets(10));
        chatLayout.setAlignment(Pos.TOP_LEFT);

        Scene scene = new Scene(chatLayout, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        chatStage.setScene(scene);
        chatStage.show();
    }
}
