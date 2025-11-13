package es.ua.sd.practica;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
	private static final String URL = "jdbc:sqlite:evcharging.db";
	
	public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void createTable() {
        String sql = " CREATE TABLE IF NOT EXISTS CPS (CPID INTEGER PRIMARY KEY, nombre TEXT NOT NULL, ciudad TEXT NOT NULL, precio INTEGER, estado TEXT)";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void InsertCP(String nombre, String ciudad, double d, String estado) {
        String sql = "INSERT INTO CPS(nombre, ciudad, precio, estado) VALUES(?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, ciudad);
            pstmt.setDouble(3, d);
            pstmt.setString(4, estado);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void UpdateCPState(String nombre, String estado) {
        String sql = "UPDATE CPS SET estado=? WHERE nombre=?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, estado);
            pstmt.setString(2, nombre);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void DeleteCP(int id) {
        String sql = "DELETE FROM CPS WHERE CPID=?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static List<String> GetAllCPS() {
        List<String> estaciones = new ArrayList<>();
        String sql = "SELECT * FROM CPS";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String linea = rs.getInt("CPID") + "|" +
                               rs.getString("nombre") + "|" +
                               rs.getString("ciudad") + "|" +
                               rs.getDouble("precio") + "|" +
                               rs.getString("estado");
                estaciones.add(linea);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return estaciones;
    }
}
