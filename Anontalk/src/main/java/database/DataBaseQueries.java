package database;

import security.PasswordsUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DataBaseQueries {

    public void crearTablaUsuarios(Connection connection) {
        String sql = "CREATE TABLE IF NOT EXISTS usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL," +
                "salt TEXT NOT NULL)";

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
    public boolean insertarUsuario(String username, String password, Connection connection) {
        String sql = "INSERT INTO usuarios (username, password, salt) VALUES (?,?,?)";

        try {
            if (connection != null) {
                // Generar salt aleatorio
                String salt = java.util.UUID.randomUUID().toString();

                // Hashear la contraseña con PBKDF2
                String hashedPassword = PasswordsUtils.hashPassword(password, salt);

                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, hashedPassword);
                preparedStatement.setString(3, salt);
                preparedStatement.executeUpdate();
                System.out.println("Usuario registrado con éxito (hash + salt).");
                return true;
            } else {
                System.out.println("Conexión no establecida. No se pudo registrar el usuario.");
            }
        } catch (SQLException e) {
            System.out.println("Error al registrar el usuario: " + e.getMessage());
        }
        return false;
    }

    public boolean validarUsuario(String username, String password, Connection connection) {
        String sql = "SELECT password, salt FROM usuarios WHERE username = ?";
        try {
            if (connection != null) {
                PreparedStatement pstmt = connection.prepareStatement(sql);
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    String storedSalt = rs.getString("salt");
                    // Verificar
                    return PasswordsUtils.verifyPassword(password, storedSalt, storedHash);
                } else {
                    // No existe el usuario
                    return false;
                }
            } else {
                System.out.println("Conexión no establecida. No se pudo validar el usuario.");
            }
        } catch (SQLException e) {
            System.out.println("Error al validar el usuario: " + e.getMessage());
        }
        return false;
    }

}

