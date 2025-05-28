// FILE: src/main/java/utils/LocaleManager.java
package utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.util.ResourceBundle;

public class LocaleManager {
    private static final String BUNDLE_BASE = "i18n/messages";
    private static final ObjectProperty<Locale> locale =
            new SimpleObjectProperty<>(Locale.getDefault());

    private static String currentUser;

    public static void initializeForUser(String username) {
        currentUser = username;
        loadSavedLanguage();
    }

    private static void loadSavedLanguage() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/users/" + currentUser + "/language"))
                    .GET()
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String languageTag = response.body();
                setLocaleFromLanguageTag(languageTag);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setLocale(Locale newLocale) {
        locale.set(newLocale);
        saveLanguagePreference(newLocale.toLanguageTag());
    }

    private static void saveLanguagePreference(String languageTag) {
        new Thread(() -> {
            try {
                String json = String.format(
                        "{\"username\":\"%s\",\"language\":\"%s\"}",
                        currentUser,
                        languageTag
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/users/change-language"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void setLocaleFromLanguageTag(String languageTag) {
        Locale newLocale = Locale.forLanguageTag(languageTag);
        locale.set(newLocale);
        Locale.setDefault(newLocale);
    }

    /* -------- API pública -------- */
    public static ObjectProperty<Locale> localeProperty() {
        return locale;
    }

    public static Locale getLocale() {
        return locale.get();
    }

    public static ResourceBundle bundle() {
        return ResourceBundle.getBundle(BUNDLE_BASE, getLocale());
    }
}