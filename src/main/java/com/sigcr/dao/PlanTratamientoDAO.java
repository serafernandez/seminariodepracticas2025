package com.sigcr.dao;

import com.sigcr.models.PlanTratamiento;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object para la gestión de planes de tratamiento.
 * Maneja la persistencia de los planes terapéuticos y sus horas semanales
 * requeridas por tipo de terapia para el CU-02.
 */
public class PlanTratamientoDAO {
    private Connection conn;

    public PlanTratamientoDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Crea un nuevo plan de tratamiento en la base de datos
     * @param plan Plan de tratamiento a crear
     * @throws SQLException si ocurre error en la operación
     */
    public void crearPlanTratamiento(PlanTratamiento plan) throws SQLException {
        conn.setAutoCommit(false);
        try {
            // Insertar plan principal
            String sqlPlan = "INSERT INTO plan_tratamiento (paciente_id, fecha_inicio, fecha_fin, estado, observaciones) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sqlPlan, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, plan.getPacienteId());
                stmt.setDate(2, plan.getFechaInicio() != null ? Date.valueOf(plan.getFechaInicio()) : null);
                stmt.setDate(3, plan.getFechaFin() != null ? Date.valueOf(plan.getFechaFin()) : null);
                stmt.setString(4, plan.getEstado());
                stmt.setString(5, plan.getObservaciones());
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    plan.setId(rs.getInt(1));
                }
            }

            // Insertar detalles de horas por tipo de terapia
            insertarDetallesHoras(plan);
            
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Actualiza un plan de tratamiento existente
     * @param plan Plan de tratamiento con datos actualizados
     * @throws SQLException si ocurre error en la operación
     */
    public void actualizarPlanTratamiento(PlanTratamiento plan) throws SQLException {
        conn.setAutoCommit(false);
        try {
            // Actualizar plan principal
            String sqlPlan = "UPDATE plan_tratamiento SET fecha_inicio=?, fecha_fin=?, estado=?, observaciones=? WHERE id=?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlPlan)) {
                stmt.setDate(1, plan.getFechaInicio() != null ? Date.valueOf(plan.getFechaInicio()) : null);
                stmt.setDate(2, plan.getFechaFin() != null ? Date.valueOf(plan.getFechaFin()) : null);
                stmt.setString(3, plan.getEstado());
                stmt.setString(4, plan.getObservaciones());
                stmt.setInt(5, plan.getId());
                stmt.executeUpdate();
            }

            // Eliminar detalles existentes y recrear
            eliminarDetallesHoras(plan.getId());
            insertarDetallesHoras(plan);
            
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Obtiene un plan de tratamiento por ID
     * @param id ID del plan
     * @return Plan de tratamiento o null si no existe
     * @throws SQLException si ocurre error en la consulta
     */
    public PlanTratamiento obtenerPlanTratamiento(int id) throws SQLException {
        String sql = "SELECT pt.*, p.nombre as nombre_paciente FROM plan_tratamiento pt " +
                    "JOIN paciente p ON pt.paciente_id = p.id WHERE pt.id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                PlanTratamiento plan = new PlanTratamiento(
                    rs.getInt("id"),
                    rs.getInt("paciente_id"),
                    rs.getString("nombre_paciente"),
                    rs.getDate("fecha_inicio") != null ? rs.getDate("fecha_inicio").toLocalDate() : null,
                    rs.getDate("fecha_fin") != null ? rs.getDate("fecha_fin").toLocalDate() : null,
                    rs.getString("estado"),
                    rs.getString("observaciones")
                );
                
                // Cargar detalles de horas
                cargarDetallesHoras(plan);
                return plan;
            }
        }
        return null;
    }

    /**
     * Obtiene el plan de tratamiento activo de un paciente
     * @param pacienteId ID del paciente
     * @return Plan activo o null si no existe
     * @throws SQLException si ocurre error en la consulta
     */
    public PlanTratamiento obtenerPlanActivoPorPaciente(int pacienteId) throws SQLException {
        String sql = "SELECT pt.*, p.nombre as nombre_paciente FROM plan_tratamiento pt " +
                    "JOIN paciente p ON pt.paciente_id = p.id " +
                    "WHERE pt.paciente_id = ? AND pt.estado = 'Activo' " +
                    "ORDER BY pt.fecha_inicio DESC LIMIT 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pacienteId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                PlanTratamiento plan = new PlanTratamiento(
                    rs.getInt("id"),
                    rs.getInt("paciente_id"),
                    rs.getString("nombre_paciente"),
                    rs.getDate("fecha_inicio") != null ? rs.getDate("fecha_inicio").toLocalDate() : null,
                    rs.getDate("fecha_fin") != null ? rs.getDate("fecha_fin").toLocalDate() : null,
                    rs.getString("estado"),
                    rs.getString("observaciones")
                );
                
                cargarDetallesHoras(plan);
                return plan;
            }
        }
        return null;
    }

    /**
     * Obtiene todos los planes de tratamiento activos
     * @return Lista de planes activos
     * @throws SQLException si ocurre error en la consulta
     */
    public List<PlanTratamiento> obtenerPlanesActivos() throws SQLException {
        List<PlanTratamiento> planes = new ArrayList<>();
        String sql = "SELECT pt.*, p.nombre as nombre_paciente FROM plan_tratamiento pt " +
                    "JOIN paciente p ON pt.paciente_id = p.id " +
                    "WHERE pt.estado = 'Activo' ORDER BY p.nombre";
        
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                PlanTratamiento plan = new PlanTratamiento(
                    rs.getInt("id"),
                    rs.getInt("paciente_id"),
                    rs.getString("nombre_paciente"),
                    rs.getDate("fecha_inicio") != null ? rs.getDate("fecha_inicio").toLocalDate() : null,
                    rs.getDate("fecha_fin") != null ? rs.getDate("fecha_fin").toLocalDate() : null,
                    rs.getString("estado"),
                    rs.getString("observaciones")
                );
                
                cargarDetallesHoras(plan);
                planes.add(plan);
            }
        }
        return planes;
    }

    /**
     * Obtiene todos los planes de un paciente específico
     * @param pacienteId ID del paciente
     * @return Lista de planes del paciente
     * @throws SQLException si ocurre error en la consulta
     */
    public List<PlanTratamiento> obtenerPlanesPorPaciente(int pacienteId) throws SQLException {
        List<PlanTratamiento> planes = new ArrayList<>();
        String sql = "SELECT pt.*, p.nombre as nombre_paciente FROM plan_tratamiento pt " +
                    "JOIN paciente p ON pt.paciente_id = p.id " +
                    "WHERE pt.paciente_id = ? ORDER BY pt.fecha_inicio DESC";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pacienteId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                PlanTratamiento plan = new PlanTratamiento(
                    rs.getInt("id"),
                    rs.getInt("paciente_id"),
                    rs.getString("nombre_paciente"),
                    rs.getDate("fecha_inicio") != null ? rs.getDate("fecha_inicio").toLocalDate() : null,
                    rs.getDate("fecha_fin") != null ? rs.getDate("fecha_fin").toLocalDate() : null,
                    rs.getString("estado"),
                    rs.getString("observaciones")
                );
                
                cargarDetallesHoras(plan);
                planes.add(plan);
            }
        }
        return planes;
    }

    /**
     * Inserta los detalles de horas por tipo de terapia
     * @param plan Plan de tratamiento
     * @throws SQLException si ocurre error en la operación
     */
    private void insertarDetallesHoras(PlanTratamiento plan) throws SQLException {
        String sql = "INSERT INTO plan_detalle (plan_id, tipo_terapia, horas_semanales) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Map.Entry<String, Integer> entry : plan.getHorasSemanalesPorTipo().entrySet()) {
                stmt.setInt(1, plan.getId());
                stmt.setString(2, entry.getKey());
                stmt.setInt(3, entry.getValue());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    /**
     * Elimina los detalles de horas de un plan
     * @param planId ID del plan
     * @throws SQLException si ocurre error en la operación
     */
    private void eliminarDetallesHoras(int planId) throws SQLException {
        String sql = "DELETE FROM plan_detalle WHERE plan_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, planId);
            stmt.executeUpdate();
        }
    }

    /**
     * Carga los detalles de horas en un plan
     * @param plan Plan de tratamiento
     * @throws SQLException si ocurre error en la consulta
     */
    private void cargarDetallesHoras(PlanTratamiento plan) throws SQLException {
        String sql = "SELECT tipo_terapia, horas_semanales FROM plan_detalle WHERE plan_id = ?";
        Map<String, Integer> horasMap = new HashMap<>();
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, plan.getId());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                horasMap.put(rs.getString("tipo_terapia"), rs.getInt("horas_semanales"));
            }
        }
        
        plan.setHorasSemanalesPorTipo(horasMap);
    }

    /**
     * Elimina un plan de tratamiento
     * @param id ID del plan a eliminar
     * @throws SQLException si ocurre error en la operación
     */
    public void eliminarPlanTratamiento(int id) throws SQLException {
        conn.setAutoCommit(false);
        try {
            eliminarDetallesHoras(id);
            
            String sql = "DELETE FROM plan_tratamiento WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
            
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
} 