package com.sigcr.repositories;

import com.sigcr.models.User;
import java.sql.*;

/**
 * Repositorio para acceso a datos de usuarios.
 * Maneja la autenticacion y gestion de usuarios del sistema SIGCR (CU-03).
 */
public class UserRepository {

    private final String url = "jdbc:mysql://localhost:3306/sigcr_db";
    private final String userDB = "root";
    private final String passwordDB = "password";

    /**
     * Busca un usuario por su nombre de usuario
     * @param username Nombre de usuario a buscar
     * @return Usuario encontrado o null si no existe
     */
    public User findByUsername(String username) {
        try (Connection conn = DriverManager.getConnection(url, userDB, passwordDB)) {
            String query = "SELECT * FROM usuario WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"), // Campo en BD es 'password'
                        rs.getString("rol"));
            }
        } catch (SQLException e) {
            System.err.println("Error de base de datos: " + e.getMessage());
        }
        return null;
    }

    /**
     * Busca un usuario por su ID
     * @param id ID del usuario
     * @return Usuario encontrado o null si no existe
     */
    public User findById(int id) {
        try (Connection conn = DriverManager.getConnection(url, userDB, passwordDB)) {
            String query = "SELECT * FROM usuario WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("rol"));
            }
        } catch (SQLException e) {
            System.err.println("Error de base de datos: " + e.getMessage());
        }
        return null;
    }

    /**
     * Valida credenciales de usuario sin hashear (para testing)
     * En produccion deberia usar contraseñas hasheadas
     * @param username Nombre de usuario
     * @param password Contraseña en texto plano
     * @return Usuario si las credenciales son validas, null en caso contrario
     */
    public User validateCredentials(String username, String password) {
        try (Connection conn = DriverManager.getConnection(url, userDB, passwordDB)) {
            String query = "SELECT * FROM usuario WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("rol"));
            }
        } catch (SQLException e) {
            System.err.println("Error de base de datos: " + e.getMessage());
        }
        return null;
    }

    /**
     * Obtiene una conexion a la base de datos
     * @return Conexion activa
     * @throws SQLException si hay error de conexion
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, userDB, passwordDB);
    }
}