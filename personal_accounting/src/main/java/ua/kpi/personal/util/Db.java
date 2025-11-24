package ua.kpi.personal.util;

import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class Db {
    private static String url;
    private static String user;
    private static String password;

    static {
        try (InputStream is = Db.class.getResourceAsStream("/config.properties")) {
            Properties p = new Properties();
            p.load(is);
            url = p.getProperty("db.url");
            user = p.getProperty("db.user");
            password = p.getProperty("db.password");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void init() {
        
        try (Connection conn = getConnection()) {
            System.out.println("Connected to database successfully!");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }


    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
    
    // Додано: close() для коректного завершення з'єднання
    public static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // Логування помилки закриття, але не кидаємо виняток
                System.err.println("Помилка при закритті з'єднання: " + e.getMessage());
            }
        }
    }

    public static void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
                System.err.println("Транзакція відкочена успішно.");
            } catch (SQLException e) {
                System.err.println("Помилка при відкаті транзакції: " + e.getMessage());
            }
        }
    }
}