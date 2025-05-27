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

        // 1. Carga el logo desde resources (src/main/resources/logo.png)
        Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/logo.png")));
        ImageView logoView = new ImageView(logo);
        logoView.setFitWidth(100);        // ancho deseado
        logoView.setPreserveRatio(true);  // mantiene la proporción original

        // 2. Elementos de UI
        ProgressIndicator pi = new ProgressIndicator();
        Label lbl = new Label("Inicializando Anontalk...");

        // 3. Construye el layout con el logo
        VBox root = new VBox(10, logoView, pi, lbl);
        root.setAlignment(Pos.CENTER);
        root.setStyle(
                "-fx-background-color: white; " +
                        "-fx-padding: 20; " +
                        "-fx-border-color: gray; " +
                        "-fx-border-width: 1;"
        );

        // 4. Ajusta el tamaño de la ventana para que quepa el logo extra
        splashStage.setScene(new Scene(root, 300, 200));
        splashStage.show();

        // 5. Task para arrancar Spring Boot sin bloquear la UI
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
}
