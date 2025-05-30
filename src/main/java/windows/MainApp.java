package windows;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainApp extends Application {

    private ConfigurableApplicationContext springCtx;
    private SplashWindow splash;

    @Override
    public void start(Stage primaryStage) {
        splash = new SplashWindow();
        // Lanza el splash y arranca Spring; al terminar, muestra LoginWindow
        splash.showSplash(ctx -> {
            this.springCtx = ctx;
            try {
                // Inicia la ventana de login usando el mismo Stage
                LoginWindow login = new LoginWindow(ctx);
                login.start(primaryStage);
            } catch (Exception e) {
                e.printStackTrace();
                Platform.exit();
            }
        });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        Background.POOL.shutdownNow();
        // Al cerrar la app, cerramos también el contexto de Spring
        if (springCtx != null) {
            springCtx.close();
        }
    }

    public static class Background {
        public static final ExecutorService POOL = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }


    public static void main(String[] args) {
        launch(args);
    }
}
