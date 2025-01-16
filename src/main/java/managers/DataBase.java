package managers;

import java.io.File;
import java.sql.*;

public class DataBase {
    private Connection connection;

    public void conectarDataBase() {
        try {
            String userHome = System.getProperty("user.home");
            String dbPath = userHome + File.separator + "UsersDataBase.db";

            String url = "jdbc:sqlite:" + dbPath;
            System.out.println("Conectando a la base de datos en:  " + dbPath);
            connection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void desconectarDataBase() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Conexión cerrada.");
            } catch (SQLException e) {
                System.out.println("Error al cerrar la conexión.");
            }
        } else {
            System.out.println("Sin conexiones activas para cerrar.");
        }
    }

    public void initTablaUsuarios() {
        String sql = "CREATE TABLE IF NOT EXISTS usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL)";

        try {
            if (connection != null) {
                connection.createStatement().execute(sql);
                System.out.println("Tabla 'usuarios' creada/validada correctamente.");
            } else {
                System.out.println("Conexión no establecida.");
            }
        } catch (SQLException e) {
            System.out.println("Error al crear/validar tabla 'usuarios': " + e.getMessage());
        }
    }

    public boolean insertarUsuario(String username, String password) {
        String sql = "INSERT INTO usuarios (username, password) VALUES (?,?)";

        try {
            if (connection != null){
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, password);
                preparedStatement.executeUpdate();
                System.out.println("Usuario registrado con éxito.");
                return true;
            } else {
                System.out.println("Conexión no establecida. No se pudo registrar el usuario.");
            }
        } catch (SQLException e) {
            System.out.println("Error al registrar el usuario: " + e.getMessage());
        }
        return false;
    }

    public boolean validarUsuario(String username, String password) {
        String sql = "SELECT * FROM usuarios WHERE username = ? AND password = ?";
        try {
            if (connection != null) {
                PreparedStatement pstmt = connection.prepareStatement(sql);
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                ResultSet rs = pstmt.executeQuery();
                return rs.next();
            } else {
                System.out.println("Conexión no establecida. No se pudo validar el usuario.");
            }
        } catch (SQLException e) {
            System.out.println("Error al validar el usuario: " + e.getMessage());
        }
        return false;
    }
}
