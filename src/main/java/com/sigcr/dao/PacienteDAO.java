package com.sigcr.dao;

import com.sigcr.models.Paciente;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la gestión de pacientes en base de datos.
 * Implementa operaciones CRUD completas y consultas especializadas
 * necesarias para el mantenimiento integral de pacientes (CU-01).
 */
public class PacienteDAO {
    private Connection conn;

    public PacienteDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Crea un nuevo paciente en la base de datos
     * @param paciente Objeto paciente a crear
     * @throws SQLException si ocurre error en la operación
     */
    public void createPaciente(Paciente paciente) throws SQLException {
        String sql = "INSERT INTO paciente (nombre, documento, fecha_nacimiento, diagnostico, habitacion, estado) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, paciente.getNombre());
            stmt.setString(2, paciente.getDocumento());
            stmt.setDate(3, paciente.getFechaNacimiento() != null ? Date.valueOf(paciente.getFechaNacimiento()) : null);
            stmt.setString(4, paciente.getDiagnostico());
            stmt.setString(5, paciente.getHabitacion());
            stmt.setString(6, paciente.getEstado());
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                paciente.setId(rs.getInt(1));
            }
        }
    }

    /**
     * Obtiene un paciente por su ID
     * @param id ID del paciente
     * @return Objeto Paciente o null si no existe
     * @throws SQLException si ocurre error en la consulta
     */
    public Paciente getPaciente(int id) throws SQLException {
        String sql = "SELECT * FROM paciente WHERE id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Paciente(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("documento"),
                        rs.getDate("fecha_nacimiento") != null ? rs.getDate("fecha_nacimiento").toLocalDate() : null,
                        rs.getString("diagnostico"),
                        rs.getString("habitacion"),
                        rs.getString("estado"));
            }
        }
        return null;
    }

    /**
     * Actualiza los datos de un paciente existente
     * @param paciente Objeto paciente con datos actualizados
     * @throws SQLException si ocurre error en la operación
     */
    public void updatePaciente(Paciente paciente) throws SQLException {
        String sql = "UPDATE paciente SET nombre=?, documento=?, fecha_nacimiento=?, diagnostico=?, habitacion=?, estado=? WHERE id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, paciente.getNombre());
            stmt.setString(2, paciente.getDocumento());
            stmt.setDate(3, paciente.getFechaNacimiento() != null ? Date.valueOf(paciente.getFechaNacimiento()) : null);
            stmt.setString(4, paciente.getDiagnostico());
            stmt.setString(5, paciente.getHabitacion());
            stmt.setString(6, paciente.getEstado());
            stmt.setInt(7, paciente.getId());
            stmt.executeUpdate();
        }
    }

    /**
     * Elimina físicamente un paciente de la base de datos
     * @param id ID del paciente a eliminar
     * @throws SQLException si ocurre error en la operación
     */
    public void deletePaciente(int id) throws SQLException {
        String sql = "DELETE FROM paciente WHERE id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Da de baja lógica a un paciente (cambia estado a 'Baja')
     * @param id ID del paciente
     * @throws SQLException si ocurre error en la operación
     */
    public void darDeBajaPaciente(int id) throws SQLException {
        String sql = "UPDATE paciente SET estado='Baja' WHERE id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Obtiene todos los pacientes del sistema
     * @return Lista de todos los pacientes
     * @throws SQLException si ocurre error en la consulta
     */
    public List<Paciente> getAllPacientes() throws SQLException {
        List<Paciente> pacientes = new ArrayList<>();
        String sql = "SELECT * FROM paciente ORDER BY nombre";
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                pacientes.add(new Paciente(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("documento"),
                        rs.getDate("fecha_nacimiento") != null ? rs.getDate("fecha_nacimiento").toLocalDate() : null,
                        rs.getString("diagnostico"),
                        rs.getString("habitacion"),
                        rs.getString("estado")));
            }
        }
        return pacientes;
    }

    /**
     * Busca pacientes por nombre (búsqueda parcial)
     * @param nombre Nombre o parte del nombre a buscar
     * @return Lista de pacientes que coinciden con la búsqueda
     * @throws SQLException si ocurre error en la consulta
     */
    public List<Paciente> buscarPacientesPorNombre(String nombre) throws SQLException {
        List<Paciente> pacientes = new ArrayList<>();
        String sql = "SELECT * FROM paciente WHERE nombre LIKE ? ORDER BY nombre";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + nombre + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                pacientes.add(new Paciente(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("documento"),
                        rs.getDate("fecha_nacimiento") != null ? rs.getDate("fecha_nacimiento").toLocalDate() : null,
                        rs.getString("diagnostico"),
                        rs.getString("habitacion"),
                        rs.getString("estado")));
            }
        }
        return pacientes;
    }

    /**
     * Busca un paciente por su documento
     * @param documento Documento del paciente
     * @return Paciente encontrado o null
     * @throws SQLException si ocurre error en la consulta
     */
    public Paciente buscarPacientePorDocumento(String documento) throws SQLException {
        String sql = "SELECT * FROM paciente WHERE documento = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, documento);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Paciente(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("documento"),
                        rs.getDate("fecha_nacimiento") != null ? rs.getDate("fecha_nacimiento").toLocalDate() : null,
                        rs.getString("diagnostico"),
                        rs.getString("habitacion"),
                        rs.getString("estado"));
            }
        }
        return null;
    }

    /**
     * Obtiene pacientes filtrados por estado
     * @param estado Estado a filtrar ('Activo', 'Alta', 'Baja')
     * @return Lista de pacientes con el estado especificado
     * @throws SQLException si ocurre error en la consulta
     */
    public List<Paciente> getPacientesPorEstado(String estado) throws SQLException {
        List<Paciente> pacientes = new ArrayList<>();
        String sql = "SELECT * FROM paciente WHERE estado = ? ORDER BY nombre";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estado);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                pacientes.add(new Paciente(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("documento"),
                        rs.getDate("fecha_nacimiento") != null ? rs.getDate("fecha_nacimiento").toLocalDate() : null,
                        rs.getString("diagnostico"),
                        rs.getString("habitacion"),
                        rs.getString("estado")));
            }
        }
        return pacientes;
    }
}