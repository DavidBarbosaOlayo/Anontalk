// src/main/java/windows/SplashWindow.java
package windows;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Objects;
import java.util.function.Consumer;

public class SplashWindow {

    private Stage splashStage;

    /**
     * Muestra la ventana de splash y arranca Spring Boot en background.
     * Cuando Spring termina, invoca onFinished con el contexto.
     */
    public void showSplash(Consumer<ConfigurableApplicationContext> onFinished) {
        splashStage = new Stage(StageStyle.UNDECORATED);

        /* ---------- Logo ---------- */
        Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/logo.png")));
        ImageView logoView = new ImageView(logo);
        logoView.setFitWidth(100);         // mismo ancho que antes
        logoView.setPreserveRatio(true);

        /* ---------- Animación circular ---------- */
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(90, 90);            // ⬆ tamaño ligeramente mayor

        /* ---------- Texto ---------- */
        Label lbl = new Label("Inicializando Anontalk...");
        lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #333;");   // ⬆ font-size

        /* ---------- Layout ---------- */
        VBox root = new VBox(14, logoView, pi, lbl);
        root.setAlignment(Pos.CENTER);
        root.setStyle("""
                -fx-background-color: white;
                -fx-padding: 28;
                -fx-border-color: gray;
                -fx-border-width: 1;
                """);

        /* ---------- Escena y ventana ---------- */
        splashStage.setScene(new Scene(root, 360, 240));   // ⬆ ventana un poco mayor
        splashStage.show();

        /* ---------- Arranque de Spring en segundo plano ---------- */
        Task<ConfigurableApplicationContext> springInit = new Task<>() {
            @Override
            protected ConfigurableApplicationContext call() {
                return org.springframework.boot.SpringApplication.run(LoginWindow.class);
            }
        };

        springInit.setOnSucceeded(evt -> {
            ConfigurableApplicationContext ctx = springInit.getValue();
            splashStage.close();
            onFinished.accept(ctx);
        });

        springInit.setOnFailed(evt -> {
            splashStage.close();
            Platform.exit();
        });

        new Thread(springInit, "spring-init").start();
    }
}
