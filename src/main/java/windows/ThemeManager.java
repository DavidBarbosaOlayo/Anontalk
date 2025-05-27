package windows;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

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
}
