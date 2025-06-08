package com.sigcr.dao;

import com.sigcr.models.Sesion;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la gestión de sesiones terapéuticas.
 * Maneja la persistencia de sesiones y consultas especializadas 
 * para cronogramas y reportes del sistema SIGCR.
 */
public class SesionDAO {
    private Connection conn;

    public SesionDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Crea una nueva sesión terapéutica en la base de datos
     * @param sesion Objeto sesión a crear
     * @throws SQLException si ocurre error en la operación
     */
    public void crearSesion(Sesion sesion) throws SQLException {
        String sql = "INSERT INTO sesion (paciente_id, terapeuta, tipo_terapia, fecha_hora, duracion) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sesion.getPacienteId());
            stmt.setString(2, sesion.getTerapeuta());
            stmt.setString(3, sesion.getTipoTerapia());
            stmt.setTimestamp(4, Timestamp.valueOf(sesion.getFechaHora()));
            stmt.setInt(5, sesion.getDuracion());
            stmt.executeUpdate();
        }
    }

    /**
     * Obtiene sesiones de un terapeuta específico para una fecha dada
     * Utilizado para consultar agenda diaria (CU-06)
     * @param terapeuta Nombre del profesional
     * @param fecha Fecha de la agenda (solo ese día)
     * @return Lista de sesiones del terapeuta en esa fecha
     * @throws SQLException si ocurre error en la consulta
     */
    public List<Sesion> obtenerSesionesPorTerapeutaYFecha(String terapeuta, LocalDate fecha) throws SQLException {
        List<Sesion> sesiones = new ArrayList<>();
        String sql = "SELECT * FROM sesion WHERE terapeuta = ? AND DATE(fecha_hora) = ? ORDER BY fecha_hora";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, terapeuta);
            stmt.setDate(2, java.sql.Date.valueOf(fecha));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Sesion sesion = new Sesion(
                        rs.getInt("paciente_id"),
                        rs.getString("terapeuta"),
                        rs.getString("tipo_terapia"),
                        rs.getTimestamp("fecha_hora").toLocalDateTime(),
                        rs.getInt("duracion"));
                sesion.setId(rs.getInt("id"));
                sesiones.add(sesion);
            }
        }
        return sesiones;
    }

    /**
     * Obtiene sesiones de un paciente específico en un rango de fechas
     * Utilizado para generar reportes de evolución (CU-05)
     * @param pacienteId ID del paciente
     * @param desde Fecha inicial del rango
     * @param hasta Fecha final del rango
     * @return Lista de sesiones del paciente en el rango especificado
     * @throws SQLException si ocurre error en la consulta
     */
    public List<Sesion> obtenerSesionesPorPacienteYRango(int pacienteId, LocalDate desde, LocalDate hasta) throws SQLException {
        List<Sesion> sesiones = new ArrayList<>();
        String sql = "SELECT * FROM sesion WHERE paciente_id = ? AND DATE(fecha_hora) BETWEEN ? AND ? ORDER BY fecha_hora";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pacienteId);
            stmt.setDate(2, java.sql.Date.valueOf(desde));
            stmt.setDate(3, java.sql.Date.valueOf(hasta));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Sesion sesion = new Sesion(
                        rs.getInt("paciente_id"),
                        rs.getString("terapeuta"),
                        rs.getString("tipo_terapia"),
                        rs.getTimestamp("fecha_hora").toLocalDateTime(),
                        rs.getInt("duracion"));
                sesion.setId(rs.getInt("id"));
                sesiones.add(sesion);
            }
        }
        return sesiones;
    }

    /**
     * Obtiene todas las sesiones de un paciente específico
     * @param pacienteId ID del paciente
     * @return Lista de todas las sesiones del paciente
     * @throws SQLException si ocurre error en la consulta
     */
    public List<Sesion> obtenerSesionesPorPaciente(int pacienteId) throws SQLException {
        List<Sesion> sesiones = new ArrayList<>();
        String sql = "SELECT * FROM sesion WHERE paciente_id = ? ORDER BY fecha_hora DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pacienteId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Sesion sesion = new Sesion(
                        rs.getInt("paciente_id"),
                        rs.getString("terapeuta"),
                        rs.getString("tipo_terapia"),
                        rs.getTimestamp("fecha_hora").toLocalDateTime(),
                        rs.getInt("duracion"));
                sesion.setId(rs.getInt("id"));
                sesiones.add(sesion);
            }
        }
        return sesiones;
    }

    /**
     * Elimina las sesiones de un paciente en un rango de fechas específico
     * Utilizado para reemplazar cronogramas semanales (CU-02)
     * @param pacienteId ID del paciente
     * @param fechaDesde Fecha inicial del rango
     * @param fechaHasta Fecha final del rango
     * @throws SQLException si ocurre error en la operación
     */
    public void eliminarSesionesPorPacienteYRango(int pacienteId, LocalDate fechaDesde, LocalDate fechaHasta) throws SQLException {
        String sql = "DELETE FROM sesion WHERE paciente_id = ? AND DATE(fecha_hora) BETWEEN ? AND ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pacienteId);
            stmt.setDate(2, java.sql.Date.valueOf(fechaDesde));
            stmt.setDate(3, java.sql.Date.valueOf(fechaHasta));
            stmt.executeUpdate();
        }
    }

    /**
     * Actualiza una sesión existente
     * @param sesion Sesión con datos actualizados
     * @throws SQLException si ocurre error en la operación
     */
    public void actualizarSesion(Sesion sesion) throws SQLException {
        String sql = "UPDATE sesion SET terapeuta=?, tipo_terapia=?, fecha_hora=?, duracion=?, observaciones=?, estado=? WHERE id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sesion.getTerapeuta());
            stmt.setString(2, sesion.getTipoTerapia());
            stmt.setTimestamp(3, Timestamp.valueOf(sesion.getFechaHora()));
            stmt.setInt(4, sesion.getDuracion());
            stmt.setString(5, ""); // observaciones por defecto
            stmt.setString(6, "Programada"); // estado por defecto
            stmt.setInt(7, sesion.getId());
            stmt.executeUpdate();
        }
    }

    /**
     * Obtiene todas las sesiones en un rango de fechas específico
     * Utilizado para reportes estadísticos generales (CU-05)
     * @param desde Fecha inicial del rango
     * @param hasta Fecha final del rango
     * @return Lista de todas las sesiones en el rango especificado
     * @throws SQLException si ocurre error en la consulta
     */
    public List<Sesion> obtenerSesionesPorRango(LocalDate desde, LocalDate hasta) throws SQLException {
        List<Sesion> sesiones = new ArrayList<>();
        String sql = "SELECT * FROM sesion WHERE DATE(fecha_hora) BETWEEN ? AND ? ORDER BY fecha_hora";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(desde));
            stmt.setDate(2, java.sql.Date.valueOf(hasta));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Sesion sesion = new Sesion(
                        rs.getInt("paciente_id"),
                        rs.getString("terapeuta"),
                        rs.getString("tipo_terapia"),
                        rs.getTimestamp("fecha_hora").toLocalDateTime(),
                        rs.getInt("duracion"));
                sesion.setId(rs.getInt("id"));
                sesiones.add(sesion);
            }
        }
        return sesiones;
    }
}   