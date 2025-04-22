// MainInboxWindow.java
package windows;

import connections.TCPController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import managers.MessageStore;
import managers.Mensaje;
import managers.PopUpMessages;
import managers.MensajeDTO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class MainInboxWindow extends Application {
    private final String currentUser;
    private final PopUpMessages pum = new PopUpMessages();
    private Stage primaryStage;
    private static final int DEFAULT_PORT = 1212;
    private final TCPController tcpController = TCPController.getInstance(DEFAULT_PORT);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper;

    public MainInboxWindow(String currentUser) {
        this.currentUser = currentUser;
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;

        // 1) Limpiar y recargar siempre desde API
        MessageStore.inboxMessages.clear();
        MessageStore.sentMessages.clear();
        loadMessagesFromApi();

        // 2) Iniciar TCP y listener (persistirá inbound incluso en tiempo real)
        tcpController.startServer();
        tcpController.addMessageListener(this::onTcpMessage);

        // Barra superior con botones...
        Label lblW = new Label("Bienvenido, " + currentUser);
        lblW.getStyleClass().add("label2");
        HBox left = new HBox(lblW); left.setAlignment(Pos.CENTER_LEFT);

        Button btnNew = new Button("Nuevo Mensaje");
        btnNew.setOnAction(e -> showSendDialog());

        Button btnLogout = new Button("Cerrar Sesión");
        btnLogout.setOnAction(e -> {
            tcpController.stopServer();
            primaryStage.close();
            pum.mostrarAlertaInformativa("Cerrar Sesión","Has cerrado sesión.");
            try { new LoginWindow().start(new Stage()); }
            catch (Exception ex){ throw new RuntimeException(ex); }
        });

        HBox right = new HBox(10, btnNew, btnLogout); right.setAlignment(Pos.CENTER_RIGHT);

        BorderPane top = new BorderPane();
        top.setLeft(left); top.setRight(right); top.setPadding(new Insets(10));

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                new Tab("Bandeja de Entrada", createInboxTable()),
                new Tab("Mensajes Enviados", createSentTable())
        );
        tabs.getTabs().forEach(t->t.setClosable(false));

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(tabs);
        root.setPadding(new Insets(10));

        Scene sc = new Scene(root,800,600);
        sc.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        primaryStage.setScene(sc);
        primaryStage.show();
    }

    private TableView<Mensaje> createInboxTable() {
        TableView<Mensaje> table = new TableView<>(MessageStore.inboxMessages);
        TableColumn<Mensaje,String> col1 = new TableColumn<>("Contacto");
        col1.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getSender()));
        col1.setPrefWidth(150);

        TableColumn<Mensaje,String> col2 = new TableColumn<>("Mensaje");
        col2.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty("Pulsa para leer"));
        col2.setPrefWidth(600);
        col2.setCellFactory(tc->new TableCell<>() {
            @Override protected void updateItem(String t, boolean empty){
                super.updateItem(t,empty);
                if(empty||getTableRow().getItem()==null){ setText(null); return; }
                setText(t);
                setOnMouseClicked(e->{
                    Mensaje m=getTableRow().getItem();
                    new ChatWindow(currentUser,m,tcpController,primaryStage).show();
                });
            }
        });

        table.getColumns().addAll(col1,col2);
        table.setPlaceholder(new Label("No hay mensajes."));
        return table;
    }

    private TableView<Mensaje> createSentTable() {
        TableView<Mensaje> table = new TableView<>(MessageStore.sentMessages);
        TableColumn<Mensaje,String> col1 = new TableColumn<>("Destinatario");
        col1.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getSender()));
        col1.setPrefWidth(150);

        TableColumn<Mensaje,String> col2 = new TableColumn<>("Mensaje");
        col2.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty("Pulsa para ver"));
        col2.setPrefWidth(600);
        col2.setCellFactory(tc->new TableCell<>() {
            @Override protected void updateItem(String t, boolean empty){
                super.updateItem(t,empty);
                if(empty||getTableRow().getItem()==null){ setText(null); return; }
                setText(t);
                setOnMouseClicked(e->{
                    Mensaje m=getTableRow().getItem();
                    new ChatWindow(currentUser,m,tcpController,primaryStage).show();
                });
            }
        });

        table.getColumns().addAll(col1,col2);
        table.setPlaceholder(new Label("No hay mensajes enviados."));
        return table;
    }

    private void loadMessagesFromApi() {
        try {
            // INBOX
            HttpRequest r1 = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/messages/inbox/" + currentUser))
                    .GET().build();
            HttpResponse<String> resp1 = httpClient.send(r1, HttpResponse.BodyHandlers.ofString());
            List<MensajeDTO> inbox = mapper.readValue(resp1.body(), new TypeReference<>(){});
            inbox.forEach(d->MessageStore.inboxMessages.add(new Mensaje(d.getRemitente(),d.getMensaje())));

            // SENT
            HttpRequest r2 = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/messages/sent/" + currentUser))
                    .GET().build();
            HttpResponse<String> resp2 = httpClient.send(r2, HttpResponse.BodyHandlers.ofString());
            List<MensajeDTO> sent = mapper.readValue(resp2.body(), new TypeReference<>(){});
            sent.forEach(d->MessageStore.sentMessages.add(new Mensaje(d.getDestinatario(),d.getMensaje())));

        } catch (Exception e) {
            e.printStackTrace();
            pum.mostrarAlertaError("Error de red","No se pudieron cargar los mensajes.");
        }
    }

    private void onTcpMessage(String sender, String message) {
        // 1) Persistir en BD (es incoming)
        persistMensaje(sender, currentUser, message);
        // 2) Refrescar UI
        Platform.runLater(() -> MessageStore.inboxMessages.add(new Mensaje(sender, message)));
    }

    private void showSendDialog() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Enviar Mensaje");
        dlg.setHeaderText("Formato: destinatario:mensaje");
        dlg.setContentText("dest:msg");
        dlg.showAndWait().ifPresent(input->{
            try {
                var p=input.split(":",2);
                String destUser=p[0], msg=p[1];
                // 1) Persistir
                persistMensaje(currentUser, destUser, msg);
                // 2) Enviar TCP (opcional: si conoces IP)
                tcpController.sendMessage(destUser, DEFAULT_PORT, msg);
                // 3) UI
                MessageStore.sentMessages.add(new Mensaje(destUser, msg));
            } catch (Exception ex){
                pum.mostrarAlertaError("Error","Formato incorrecto.");
            }
        });
    }

    private void persistMensaje(String remitente, String destinatario, String texto) {
        try {
            MensajeDTO dto=new MensajeDTO();
            dto.setRemitente(remitente);
            dto.setDestinatario(destinatario);
            dto.setMensaje(texto);
            String j=mapper.writeValueAsString(dto);
            HttpRequest rq=HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/messages/send"))
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(j))
                    .build();
            httpClient.sendAsync(rq, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e){
            e.printStackTrace();
            pum.mostrarAlertaError("Error","No se pudo guardar en BD.");
        }
    }
}
