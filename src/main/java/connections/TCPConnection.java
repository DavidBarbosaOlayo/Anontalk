package connections;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPConnection {
    private ServerSocket serverSocket;
    private boolean running;

    // Iniciar el servidor en un puerto específico
    public void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
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
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Mensaje recibido: " + message);
                // Responder al cliente
                out.println("Mensaje recibido: " + message);
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
