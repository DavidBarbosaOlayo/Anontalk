package windows;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import managers.PopUpInfo;

public class ProfileWindow extends Stage {

    private final PopUpInfo pop = new PopUpInfo();
    private static final double WIDTH = 800;
    private static final double HEIGHT = 650;
    private static final double BTN_WIDTH = 250;  // ancho fijo para todos los botones
    private static final double FIELD_WIDTH = 200;  // ancho fijo para los campos

    public ProfileWindow(String currentUser) {
        setTitle("Perfil de " + currentUser);

        // Título principal, centrado con padding top extra
        Label lblTitle = new Label("Perfil Usuario");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        lblTitle.setAlignment(Pos.CENTER);
        lblTitle.setPadding(new Insets(30, 0, 0, 0));

        // Secciones con separación
        VBox rootSections = new VBox(30, buildSection("Cambiar contraseña", buildPwdContent()), buildSection("Cambiar email", buildEmailContent()));
        rootSections.setAlignment(Pos.TOP_CENTER);

        // Botón de eliminar cuenta (siempre visible), rojo brillante, centrado
        Button btnDel = new Button("Eliminar cuenta");
        btnDel.setPrefWidth(BTN_WIDTH);
        btnDel.setAlignment(Pos.CENTER);
        btnDel.setStyle("-fx-background-color: #d32f2f;" + "-fx-text-fill: white;" + "-fx-font-weight: bold;");
        btnDel.setOnAction(e -> pop.mostrarAlertaInformativa("Eliminar cuenta", "Funcionalidad en desarrollo."));
        VBox deleteBox = new VBox(btnDel);
        deleteBox.setAlignment(Pos.CENTER);
        deleteBox.setPadding(new Insets(20, 0, 0, 0));

        // Contenedor principal con scroll
        VBox content = new VBox(40, lblTitle, rootSections, deleteBox);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Scene scene = new Scene(scrollPane, WIDTH, HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/temas.css").toExternalForm());
        setScene(scene);
    }

    /**
     * Crea una sección con botón-header centrado y contenido colapsable animado
     */
    private VBox buildSection(String headerText, Node content) {
        Button header = new Button(headerText);
        header.setPrefWidth(BTN_WIDTH);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // Contenido inicialmente oculto y colapsado
        content.setVisible(false);
        content.setScaleY(0);
        content.setOpacity(0);
        content.managedProperty().bind(content.visibleProperty());

        header.setOnAction(e -> {
            if (content.isVisible()) {
                Timeline collapse = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(content.scaleYProperty(), 0), new KeyValue(content.opacityProperty(), 0)));
                collapse.setOnFinished(ev -> content.setVisible(false));
                collapse.play();
            } else {
                content.setVisible(true);
                Timeline expand = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(content.scaleYProperty(), 0), new KeyValue(content.opacityProperty(), 0)), new KeyFrame(Duration.millis(300), new KeyValue(content.scaleYProperty(), 1), new KeyValue(content.opacityProperty(), 1)));
                expand.play();
            }
        });

        VBox box = new VBox(10, header, content);
        box.setAlignment(Pos.TOP_CENTER);
        return box;
    }

    private VBox buildPwdContent() {
        Label lbl = new Label("Cambiar contraseña");
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        PasswordField txt1 = new PasswordField();
        txt1.setPromptText("Introduzca nueva contraseña");
        txt1.setPrefWidth(FIELD_WIDTH);
        txt1.setMaxWidth(FIELD_WIDTH);

        PasswordField txt2 = new PasswordField();
        txt2.setPromptText("Confirme contraseña");
        txt2.setPrefWidth(FIELD_WIDTH);
        txt2.setMaxWidth(FIELD_WIDTH);

        Button btnSave = new Button("Guardar");
        btnSave.setPrefWidth(BTN_WIDTH);
        btnSave.setAlignment(Pos.CENTER);
        btnSave.setStyle("-fx-background-color: transparent;" + "-fx-border-width: 0;" + "-fx-text-fill: #102C54;" + "-fx-font-weight: bold;");
        btnSave.setOnAction(e -> pop.mostrarAlertaInformativa("Cambiar contraseña", "Funcionalidad en desarrollo."));

        VBox v = new VBox(12, lbl, txt1, txt2, btnSave);
        v.setPadding(new Insets(10, 0, 0, 0));
        v.setAlignment(Pos.CENTER);
        return v;
    }

    private VBox buildEmailContent() {
        Label lbl = new Label("Cambiar email");
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        TextField txt1 = new TextField();
        txt1.setPromptText("Introduzca nuevo correo");
        txt1.setPrefWidth(FIELD_WIDTH);
        txt1.setMaxWidth(FIELD_WIDTH);

        TextField txt2 = new TextField();
        txt2.setPromptText("Confirme correo");
        txt2.setPrefWidth(FIELD_WIDTH);
        txt2.setMaxWidth(FIELD_WIDTH);

        Button btnSave = new Button("Guardar");
        btnSave.setPrefWidth(BTN_WIDTH);
        btnSave.setAlignment(Pos.CENTER);
        btnSave.setStyle("-fx-background-color: transparent;" + "-fx-border-width: 0;" + "-fx-text-fill: #102C54;" + "-fx-font-weight: bold;");
        btnSave.setOnAction(e -> pop.mostrarAlertaInformativa("Cambiar email", "Funcionalidad en desarrollo."));

        VBox v = new VBox(12, lbl, txt1, txt2, btnSave);
        v.setPadding(new Insets(10, 0, 0, 0));
        v.setAlignment(Pos.CENTER);
        return v;
    }
}
