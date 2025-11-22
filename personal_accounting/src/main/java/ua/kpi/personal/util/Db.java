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
}
