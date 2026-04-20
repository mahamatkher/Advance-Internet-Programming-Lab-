import java.sql.*;

/**
 * King Faisal University – Chad
 * Academic Information System v2.0
 * Database connection utility.
 */
public class DBConnection {
    private static final String URL  = "jdbc:mysql://localhost:3306/kfudb"
            + "?useSSL=false&serverTimezone=UTC&autoReconnect=true&characterEncoding=UTF-8";
    private static final String USER = "root";
    private static final String PASS = "Tchad235@"; // change before deployment

    static {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found on classpath", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    /** Silently close a connection (null-safe). */
    public static void close(Connection con) {
        if (con != null) try { con.close(); } catch (SQLException ignored) {}
    }

    /** Return a safe error message – never null. */
    public static String errMsg(SQLException e) {
        String msg = e.getMessage();
        return (msg != null ? msg : "Unknown database error").replace("\"", "'");
    }
}
