package connections;

import security.AESUtils;
import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPController {
    // Singleton
    private static TCPController instance = null;

    private ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final int port;
    private volatile boolean running;

    // Clave AES para cifrar/descifrar
    private SecretKey aesKey;

    // Lista de listeners para notificar a todas las vistas que estén interesadas
    private final List<TCPConnection.MessageListener> listeners = new CopyOnWriteArrayList<>();

    // Constructor privado para singleton
    private TCPController(int port) {
        this.port = port;
        // Pool de hilos para manejar las conexiones entrantes
        this.threadPool = Executors.newCachedThreadPool();
    }

    // Método estático para obtener la instancia. Carga la clave AES de disco.
    public static TCPController getInstance(int port) {
        if (instance == null) {
            synchronized (TCPController.class) {
                if (instance == null) {
                    instance = new TCPController(port);
                    // Cargar la clave AES (asegúrate de que exista aes.key)
                    // Si no existe, se puede crear en el arranque de la app con AESUtils.createAndStoreKeyIfNotExist()
                    instance.aesKey = AESUtils.loadKey();
                }
            }
        }
        return instance;
    }

    // Iniciar el servidor TCP
    public void startServer() {
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"));
            running = true;
            System.out.println("Servidor TCP escuchando en el puerto: " + port);

            // Hilo encargado de aceptar conexiones de forma continua
            threadPool.execute(() -> {
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("Conexión entrante de: " + clientSocket.getInetAddress());
                        // Cada conexión se maneja en su propio hilo del pool
                        threadPool.execute(() -> handleClient(clientSocket));
                    } catch (IOException e) {
                        if (running) {
                            System.out.println("Error al aceptar la conexión: " + e.getMessage());
                        }
                    }
                }
            });
        } catch (IOException e) {
            System.out.println("Error al iniciar el servidor: " + e.getMessage());
        }
    }

    // Manejar una conexión de cliente
    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String sender = clientSocket.getInetAddress().toString();
            String encryptedMessage;
            while ((encryptedMessage = in.readLine()) != null) {
                try {
                    // Descifrar el contenido antes de notificar a los listeners
                    String decryptedMessage = AESUtils.decrypt(encryptedMessage, aesKey);
                    System.out.println("Mensaje recibido (descifrado): " + decryptedMessage);

                    // Notificar a todos los listeners registrados con el mensaje en claro
                    for (TCPConnection.MessageListener listener : listeners) {
                        listener.onMessageReceived(sender, decryptedMessage);
                    }
                } catch (Exception e) {
                    System.out.println("Error al descifrar el mensaje entrante: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Error al manejar cliente: " + e.getMessage());
        }
    }

    // Detener el servidor y liberar recursos
    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            System.out.println("Servidor detenido.");
        } catch (IOException e) {
            System.out.println("Error al detener el servidor: " + e.getMessage());
        }
        threadPool.shutdown();
    }

    public void addMessageListener(TCPConnection.MessageListener listener) {
        listeners.add(listener);
    }

    public void removeMessageListener(TCPConnection.MessageListener listener) {
        listeners.remove(listener);
    }

    // Enviar un mensaje a otro usuario (cifrado con AES)
    public void sendMessage(String host, int port, String message) {
        try {
            // Cifrar antes de enviar
            String encryptedMessage = AESUtils.encrypt(message, aesKey);

            try (Socket socket = new Socket(host, port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                out.println(encryptedMessage);
                System.out.println("Mensaje enviado (cifrado) a " + host + ":" + port + " -> " + message);
            }
        } catch (Exception e) {
            System.out.println("Error al enviar el mensaje cifrado: " + e.getMessage());
        }
    }
}
