package database;

import javax.xml.crypto.Data;
import java.io.File;
import java.sql.*;

public class DataBase {
    private Connection connection;
    private DataBaseQueries dataBaseQueries;

    public DataBase(DataBaseQueries dataBaseQueries) {
        this.dataBaseQueries = dataBaseQueries;
    }

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

    public Connection getConnection() {
        return connection;
    }
}
