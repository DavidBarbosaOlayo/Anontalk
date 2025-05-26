package managers.mensajes;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MessageStore {
    // Lista para los mensajes recibidos
    public static ObservableList<Mensaje> inboxMessages = FXCollections.observableArrayList();

    // Lista para los mensajes enviados
    public static ObservableList<Mensaje> sentMessages = FXCollections.observableArrayList();
}
