package com.sigcr.dao;

import com.sigcr.models.Notificacion;
import com.sigcr.models.Notificacion.TipoNotificacion;
import com.sigcr.models.Notificacion.RolDestinatario;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object completo para la gestión de notificaciones del sistema (CU-04).
 * Maneja la persistencia y consultas de notificaciones con filtros por rol,
 * tipo, estado de lectura y fechas.
 */
public class NotificacionDAO {
    private Connection conn;

    public NotificacionDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Crea una nueva notificación en la base de datos
     * @param notificacion Notificación a crear
     * @throws SQLException si ocurre error en la operación
     */
    public void crearNotificacion(Notificacion notificacion) throws SQLException {
        String sql = "INSERT INTO notificacion (paciente_id, mensaje, fecha_hora, leida, tipo, destinatario_rol) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, notificacion.getPacienteId());
            stmt.setString(2, notificacion.getMensaje());
            stmt.setTimestamp(3, Timestamp.valueOf(notificacion.getFechaHora()));
            stmt.setBoolean(4, notificacion.isLeida());
            stmt.setString(5, notificacion.getTipo().name());
            stmt.setString(6, notificacion.getDestinatarioRol().name());
            
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                notificacion.setId(rs.getInt(1));
            }
        }
    }

    /**
     * Obtiene todas las notificaciones de un paciente específico
     * @param pacienteId ID del paciente
     * @return Lista de notificaciones del paciente
     * @throws SQLException si ocurre error en la consulta
     */
    public List<Notificacion> obtenerNotificacionesPorPaciente(int pacienteId) throws SQLException {
        String sql = "SELECT * FROM notificacion WHERE paciente_id = ? ORDER BY fecha_hora DESC";
        return ejecutarConsultaNotificaciones(sql, pacienteId);
    }

    /**
     * Obtiene notificaciones dirigidas a un rol específico
     * @param rol Rol destinatario
     * @param incluirTodas Si incluir notificaciones dirigidas a "TODOS"
     * @return Lista de notificaciones para el rol
     * @throws SQLException si ocurre error en la consulta
     */
    public List<Notificacion> obtenerNotificacionesPorRol(RolDestinatario rol, boolean incluirTodas) throws SQLException {
        String sql;
        if (incluirTodas) {
            sql = "SELECT * FROM notificacion WHERE destinatario_rol = ? OR destinatario_rol = 'TODOS' ORDER BY fecha_hora DESC";
        } else {
            sql = "SELECT * FROM notificacion WHERE destinatario_rol = ? ORDER BY fecha_hora DESC";
        }
        return ejecutarConsultaNotificaciones(sql, rol.name());
    }

    /**
     * Obtiene notificaciones no leídas para un rol específico
     * @param rol Rol destinatario
     * @param incluirTodas Si incluir notificaciones dirigidas a "TODOS"
     * @return Lista de notificaciones no leídas
     * @throws SQLException si ocurre error en la consulta
     */
    public List<Notificacion> obtenerNotificacionesNoLeidas(RolDestinatario rol, boolean incluirTodas) throws SQLException {
        String sql;
        if (incluirTodas) {
            sql = "SELECT * FROM notificacion WHERE (destinatario_rol = ? OR destinatario_rol = 'TODOS') AND leida = FALSE ORDER BY fecha_hora DESC";
        } else {
            sql = "SELECT * FROM notificacion WHERE destinatario_rol = ? AND leida = FALSE ORDER BY fecha_hora DESC";
        }
        return ejecutarConsultaNotificaciones(sql, rol.name());
    }

    /**
     * Obtiene notificaciones por tipo
     * @param tipo Tipo de notificación
     * @return Lista de notificaciones del tipo especificado
     * @throws SQLException si ocurre error en la consulta
     */
    public List<Notificacion> obtenerNotificacionesPorTipo(TipoNotificacion tipo) throws SQLException {
        String sql = "SELECT * FROM notificacion WHERE tipo = ? ORDER BY fecha_hora DESC";
        return ejecutarConsultaNotificaciones(sql, tipo.name());
    }

    /**
     * Obtiene notificaciones en un rango de fechas
     * @param desde Fecha inicio
     * @param hasta Fecha fin
     * @return Lista de notificaciones en el rango
     * @throws SQLException si ocurre error en la consulta
     */
    public List<Notificacion> obtenerNotificacionesPorFecha(LocalDateTime desde, LocalDateTime hasta) throws SQLException {
        String sql = "SELECT * FROM notificacion WHERE fecha_hora BETWEEN ? AND ? ORDER BY fecha_hora DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(desde));
            stmt.setTimestamp(2, Timestamp.valueOf(hasta));
            return ejecutarConsultaNotificaciones(stmt);
        }
    }

    /**
     * Marca una notificación como leída
     * @param notificacionId ID de la notificación
     * @throws SQLException si ocurre error en la operación
     */
    public void marcarComoLeida(int notificacionId) throws SQLException {
        String sql = "UPDATE notificacion SET leida = TRUE WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, notificacionId);
            stmt.executeUpdate();
        }
    }

    /**
     * Marca todas las notificaciones de un rol como leídas
     * @param rol Rol destinatario
     * @param incluirTodas Si incluir notificaciones dirigidas a "TODOS"
     * @throws SQLException si ocurre error en la operación
     */
    public void marcarTodasComoLeidas(RolDestinatario rol, boolean incluirTodas) throws SQLException {
        String sql;
        if (incluirTodas) {
            sql = "UPDATE notificacion SET leida = TRUE WHERE destinatario_rol = ? OR destinatario_rol = 'TODOS'";
        } else {
            sql = "UPDATE notificacion SET leida = TRUE WHERE destinatario_rol = ?";
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rol.name());
            stmt.executeUpdate();
        }
    }

    /**
     * Cuenta las notificaciones no leídas para un rol
     * @param rol Rol destinatario
     * @param incluirTodas Si incluir notificaciones dirigidas a "TODOS"
     * @return Número de notificaciones no leídas
     * @throws SQLException si ocurre error en la consulta
     */
    public int contarNotificacionesNoLeidas(RolDestinatario rol, boolean incluirTodas) throws SQLException {
        String sql;
        if (incluirTodas) {
            sql = "SELECT COUNT(*) FROM notificacion WHERE (destinatario_rol = ? OR destinatario_rol = 'TODOS') AND leida = FALSE";
        } else {
            sql = "SELECT COUNT(*) FROM notificacion WHERE destinatario_rol = ? AND leida = FALSE";
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rol.name());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Elimina notificaciones anteriores a una fecha específica
     * @param fechaLimite Fecha límite (se eliminarán las anteriores)
     * @return Número de notificaciones eliminadas
     * @throws SQLException si ocurre error en la operación
     */
    public int limpiarNotificacionesAntiguas(LocalDateTime fechaLimite) throws SQLException {
        String sql = "DELETE FROM notificacion WHERE fecha_hora < ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(fechaLimite));
            return stmt.executeUpdate();
        }
    }

    /**
     * Obtiene una notificación por ID
     * @param id ID de la notificación
     * @return Notificación o null si no existe
     * @throws SQLException si ocurre error en la consulta
     */
    public Notificacion obtenerNotificacionPorId(int id) throws SQLException {
        String sql = "SELECT * FROM notificacion WHERE id = ?";
        List<Notificacion> resultado = ejecutarConsultaNotificaciones(sql, id);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    /**
     * Método auxiliar para ejecutar consultas de notificaciones
     */
    private List<Notificacion> ejecutarConsultaNotificaciones(String sql, Object... parametros) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                stmt.setObject(i + 1, parametros[i]);
            }
            return ejecutarConsultaNotificaciones(stmt);
        }
    }

    /**
     * Método auxiliar para ejecutar consultas de notificaciones con PreparedStatement
     */
    private List<Notificacion> ejecutarConsultaNotificaciones(PreparedStatement stmt) throws SQLException {
        List<Notificacion> notificaciones = new ArrayList<>();
        ResultSet rs = stmt.executeQuery();
        
        while (rs.next()) {
            Notificacion notificacion = new Notificacion();
            notificacion.setId(rs.getInt("id"));
            notificacion.setPacienteId(rs.getObject("paciente_id", Integer.class));
            notificacion.setMensaje(rs.getString("mensaje"));
            notificacion.setFechaHora(rs.getTimestamp("fecha_hora").toLocalDateTime());
            notificacion.setLeida(rs.getBoolean("leida"));
            
            try {
                notificacion.setTipo(TipoNotificacion.valueOf(rs.getString("tipo")));
            } catch (IllegalArgumentException e) {
                notificacion.setTipo(TipoNotificacion.GENERAL);
            }
            
            try {
                notificacion.setDestinatarioRol(RolDestinatario.valueOf(rs.getString("destinatario_rol")));
            } catch (IllegalArgumentException e) {
                notificacion.setDestinatarioRol(RolDestinatario.TODOS);
            }
            
            notificaciones.add(notificacion);
        }
        
        return notificaciones;
    }
}