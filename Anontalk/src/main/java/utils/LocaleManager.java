package utils;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Locale;

public class LocaleManager {
    private static final ObjectProperty<Locale> localeProperty =
            new SimpleObjectProperty<>(Locale.getDefault());

    /** Propiedad observable que puedes suscribir en cualquier ventana */
    public static ObjectProperty<Locale> localeProperty() {
        return localeProperty;
    }

    /** Llama a este método para cambiar el idioma en toda la app */
    public static void setLocale(Locale locale) {
        localeProperty.set(locale);
    }

    /** Para obtener el locale actual */
    public static Locale getLocale() {
        return localeProperty.get();
    }
}
