package windows;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class ThemeManager {
    private static final ThemeManager INSTANCE = new ThemeManager();

    // "light" o "dark"
    private final ReadOnlyStringWrapper theme = new ReadOnlyStringWrapper("light");

    private ThemeManager() {}

    public static ThemeManager getInstance() {
        return INSTANCE;
    }

    public ReadOnlyStringProperty themeProperty() {
        return theme.getReadOnlyProperty();
    }

    public String getTheme() {
        return theme.get();
    }

    public void setTheme(String newTheme) {
        theme.set(newTheme);
    }

    /** Devuelve la URL de la hoja de estilos actual */
    public String getCss() {
        String css = theme.get().equals("dark")
                ? "/temas/tema2.css"   // dark
                : "/temas/temas.css";  // light
        return getClass().getResource(css).toExternalForm();
    }

    public void setTheme(String newTheme, String username) {
        theme.set(newTheme);
        guardarTemaEnBackend(username, newTheme);
    }

    private void guardarTemaEnBackend(String username, String theme) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(
                    Map.of("username", username, "theme", theme)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/users/change-theme"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
