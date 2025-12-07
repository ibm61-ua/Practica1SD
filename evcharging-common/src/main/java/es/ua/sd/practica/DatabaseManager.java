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
    

    private final String url;
    private final String user;
    private final String pass;
    private static final int POSTGRES_PORT = 5432; 

    public DatabaseManager(String ip, String dbName, String user, String pass) {

        this.url = String.format("jdbc:postgresql://%s:%d/%s", ip, POSTGRES_PORT, dbName);
        this.user = user;
        this.pass = pass;
    }
    
    public Connection connect() throws SQLException {
    	Connection conn = DriverManager.getConnection(this.url, this.user, this.pass);
        
        if (!conn.getAutoCommit()) {
            conn.setAutoCommit(true);
        }
        return conn;
    }

    public void createTable() {
 
        String sql = " CREATE TABLE IF NOT EXISTS CPS ("
                   + " CPID SERIAL PRIMARY KEY,"
                   + " nombre VARCHAR(255) NOT NULL,"
                   + " ciudad VARCHAR(255) NOT NULL,"
                   + " precio NUMERIC(10, 2),"
                   + " estado VARCHAR(50),"
                   + " UNIQUE(nombre)"
                   + ")";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabla CPS verificada/creada.");
        } catch (SQLException e) {
            System.err.println("Error al crear la tabla en PostgreSQL.");
            e.printStackTrace();
        }
    }
    
    public boolean InsertCP(String nombre, String ciudad, double precio, String estado) {
        String sql = "INSERT INTO CPS(nombre, ciudad, precio, estado) VALUES(?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, ciudad);
            pstmt.setDouble(3, precio);
            pstmt.setString(4, estado);
            pstmt.executeUpdate();
            System.out.println("CP insertado: " + nombre);
            return true;
        } catch (SQLException e) {
            System.err.println("Error al insertar CP.");
            e.printStackTrace();
            return false;
        }
    }
    
    public void UpdateCPState(String nombre, String estado) {
        String sql = "UPDATE CPS SET estado=? WHERE nombre=?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, estado);
            pstmt.setString(2, nombre);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al actualizar el estado del CP.");
            e.printStackTrace();
        }
    }
    
    public boolean DeleteCP(String nombre) {
        String sql = "DELETE FROM CPS WHERE nombre=?";

        try (Connection conn = connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.executeUpdate();
            System.out.println("CP " + nombre + " eliminado.");
            return true;
        } catch (SQLException e) {
            System.err.println("Error al eliminar CP.");
            e.printStackTrace();
            return false;
        }
    }
    
    public List<String> GetAllCPS() {
        List<String> estaciones = new ArrayList<>();
        String sql = "SELECT * FROM CPS";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String linea = rs.getInt("cpid") + "|" +
                               rs.getString("nombre") + "|" +
                               rs.getString("ciudad") + "|" +
                               rs.getDouble("precio") + "|" +
                               rs.getString("estado");
                estaciones.add(linea);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener todos los CPs.");
            e.printStackTrace();
        }
        return estaciones;
    }

}