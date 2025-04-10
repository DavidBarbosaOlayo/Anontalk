package connections;

import windows.MainInboxWindow;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPConnection {
    private ServerSocket serverSocket;
    private boolean running;

    // Interfaz para manejar mensajes entrantes
    public interface MessageListener {
        void onMessageReceived(String sender, String message);
    }

    private MessageListener messageListener;

    // Asignar un listener para manejar mensajes
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    // Iniciar el servidor en un puerto específico
    public void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"));
            running = true;
            System.out.println("Servidor TCP escuchando en el puerto: " + port);

            // Hilo para aceptar conexiones entrantes
            new Thread(() -> {
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("Conexión entrante de: " + clientSocket.getInetAddress());
                        handleClient(clientSocket); // Manejar la conexión
                    } catch (IOException e) {
                        System.out.println("Error al aceptar la conexión: " + e.getMessage());
                    }
                }
            }).start();
        } catch (IOException e) {
            System.out.println("Error al iniciar el servidor: " + e.getMessage());
        }
    }

    // Manejar clientes conectados
    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String sender = clientSocket.getInetAddress().toString();
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Mensaje recibido: " + message);
                // Notificar al listener (ventana principal)
                if (messageListener != null) {
                    messageListener.onMessageReceived(sender, message);
                }
            }
        } catch (IOException e) {
            System.out.println("Error al manejar cliente: " + e.getMessage());
        }
    }

    // Detener el servidor
    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            System.out.println("Servidor detenido.");
        } catch (IOException e) {
            System.out.println("Error al detener el servidor: " + e.getMessage());
        }
    }

    // Enviar un mensaje a otro usuario
    public void sendMessage(String host, int port, String message) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println(message);
            System.out.println("Mensaje enviado a " + host + ":" + port + " -> " + message);
        } catch (IOException e) {
            System.out.println("Error al enviar el mensaje: " + e.getMessage());
        }
    }
}
