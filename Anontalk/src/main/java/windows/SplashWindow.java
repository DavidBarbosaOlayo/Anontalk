// src/main/java/windows/SplashWindow.java
package windows;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.function.Consumer;

public class SplashWindow {

    private Stage splashStage;

    /**
     * Muestra la ventana de splash y arranca Spring Boot en background.
     * Cuando Spring termina, invoca onFinished con el contexto.
     */
    public void showSplash(Consumer<ConfigurableApplicationContext> onFinished) {
        splashStage = new Stage(StageStyle.UNDECORATED);
        ProgressIndicator pi = new ProgressIndicator();
        Label lbl = new Label("Inicializando Anontalk...");
        VBox root = new VBox(10, pi, lbl);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: white; " + "-fx-padding:20; " + "-fx-border-color: gray; " + "-fx-border-width:1;");
        splashStage.setScene(new Scene(root, 300, 150));
        splashStage.show();

        // Task para arrancar Spring Boot sin bloquear la UI
        Task<ConfigurableApplicationContext> springInit = new Task<>() {
            @Override
            protected ConfigurableApplicationContext call() {
                // Arranca Spring Boot (lee @SpringBootApplication de LoginWindow)
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

    /**
     * Para cerrar el splash desde fuera si hiciera falta
     */
    public void close() {
        if (splashStage != null) {
            splashStage.close();
        }
    }
}