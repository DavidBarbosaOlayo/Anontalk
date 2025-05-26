package utils;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Locale;
import java.util.ResourceBundle;

public final class LocaleManager {

    private static final String BUNDLE_BASE = "i18n/messages";
    private static final ObjectProperty<Locale> locale = new SimpleObjectProperty<>(Locale.getDefault());

    private LocaleManager() {
    }

    /* -------- API pública -------- */
    public static ObjectProperty<Locale> localeProperty() {
        return locale;
    }

    public static Locale getLocale() {
        return locale.get();
    }


    public static void setLocale(Locale newLocale) {
        locale.set(newLocale);
    }

    public static ResourceBundle bundle() {
        return ResourceBundle.getBundle(BUNDLE_BASE, getLocale());
    }
}
