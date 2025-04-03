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
    // Singleton: solo se crea una instancia por nodo.
    private static TCPController instance = null;
    private ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final int port;
    private volatile boolean running = false;

    // Clave AES compartida para cifrado/descifrado.
    private SecretKey aesKey;

    // Lista de listeners para notificar la recepción de mensajes.
    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();

    public interface MessageListener {
        void onMessageReceived(String sender, String message);
    }

    // Constructor privado.
    private TCPController(int port) {
        this.port = port;
        this.threadPool = Executors.newCachedThreadPool();
        // Cargar la clave AES de un archivo compartido (asegúrate que ambas máquinas usen el mismo archivo).
        this.aesKey = AESUtils.loadKey();
    }

    // Obtención de la instancia (singleton).
    public static TCPController getInstance(int port) {
        if (instance == null) {
            synchronized (TCPController.class) {
                if (instance == null) {
                    instance = new TCPController(port);
                }
            }
        }
        return instance;
    }

    // Inicia el servidor si no está ya en ejecución.
    public void startServer() {
        if (running) {
            System.out.println("Servidor ya iniciado.");
            return;
        }
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"));
            running = true;
            System.out.println("Servidor TCP escuchando en el puerto: " + port);
            // Hilo para aceptar conexiones de forma continua.
            threadPool.execute(() -> {
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("Conexión entrante de: " + clientSocket.getInetAddress());
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

    // Maneja una conexión entrante: recibe el mensaje cifrado y lo descifra.
    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String sender = clientSocket.getInetAddress().toString();
            String encryptedMessage;
            while ((encryptedMessage = in.readLine()) != null) {
                try {
                    String decryptedMessage = AESUtils.decrypt(encryptedMessage, aesKey);
                    System.out.println("Mensaje recibido (descifrado): " + decryptedMessage);
                    for (MessageListener listener : listeners) {
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

    // Detiene el servidor y libera recursos.
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

    // Registra un listener para recibir notificaciones cuando llegue un mensaje.
    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }

    // Envía un mensaje cifrado a otro usuario.
    public void sendMessage(String host, int port, String message) {
        try {
            // Cifrar el mensaje.
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
