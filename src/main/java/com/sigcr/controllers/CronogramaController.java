package com.sigcr.controllers;

import com.sigcr.dao.PlanTratamientoDAO;
import com.sigcr.dao.SesionDAO;
import com.sigcr.models.PlanTratamiento;
import com.sigcr.models.Sesion;
import com.sigcr.models.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador que maneja la planificacion de cronogramas terapeuticos (CU-02).
 * Coordina la asignacion de sesiones, validacion de horas requeridas y
 * generacion de notificaciones para profesionales afectados.
 */
public class CronogramaController {
    
    private PlanTratamientoDAO planTratamientoDAO;
    private SesionDAO sesionDAO;
    private NotificacionController notificacionController;
    private User usuarioActual;

    // Tipos de terapia disponibles en el sistema
    public static final List<String> TIPOS_TERAPIA = Arrays.asList(
        "Fisioterapia", "Terapia Ocupacional", "Psicologia", 
        "Fonoaudiologia", "Terapia Respiratoria", "Hidroterapia"
    );

    // Terapeutas disponibles (en un sistema real vendria de la BD)
    public static final List<String> TERAPEUTAS = Arrays.asList(
        "luz.terapeuta", "ana.fisio", "carlos.psico", 
        "maria.fono", "jose.respiratorio", "sofia.hidro"
    );

    /**
     * Constructor del controlador de cronogramas
     * @param conn Conexion a la base de datos
     * @param usuarioActual Usuario que esta realizando las operaciones
     */
    public CronogramaController(Connection conn, User usuarioActual) {
        this.planTratamientoDAO = new PlanTratamientoDAO(conn);
        this.sesionDAO = new SesionDAO(conn);
        this.notificacionController = new NotificacionController(conn, usuarioActual);
        this.usuarioActual = usuarioActual;
    }

    /**
     * Crea un nuevo plan de tratamiento para un paciente
     * @param plan Plan de tratamiento a crear
     * @return true si se creo exitosamente
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no tiene permisos
     */
    public boolean crearPlanTratamiento(PlanTratamiento plan) throws SQLException, SecurityException {
        // Verificar permisos (solo MEDICO puede crear planes)
        if (!usuarioActual.getRole().equals("MEDICO")) {
            throw new SecurityException("Solo los medicos pueden crear planes de tratamiento");
        }

        // Validar el plan
        if (!plan.isValido()) {
            throw new IllegalArgumentException("El plan de tratamiento no es valido");
        }

        // Crear el plan
        planTratamientoDAO.crearPlanTratamiento(plan);

        // Generar notificacion
        generarNotificacionPlanCreado(plan);

        return true;
    }

    /**
     * Planifica las sesiones de una semana especifica para un paciente
     * @param pacienteId ID del paciente
     * @param fechaInicioDeSemana Fecha del lunes de la semana a planificar
     * @param sesionesPropuestas Lista de sesiones propuestas
     * @return Resultado de la planificacion con alertas si las hay
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no tiene permisos
     */
    public ResultadoPlanificacion planificarSemana(int pacienteId, LocalDate fechaInicioDeSemana, 
                                                 List<Sesion> sesionesPropuestas) throws SQLException, SecurityException {
        
        // Verificar permisos (solo MEDICO puede planificar)
        if (!usuarioActual.getRole().equals("MEDICO")) {
            throw new SecurityException("Solo los medicos pueden planificar cronogramas");
        }

        // Obtener plan de tratamiento activo
        PlanTratamiento plan = planTratamientoDAO.obtenerPlanActivoPorPaciente(pacienteId);
        if (plan == null) {
            throw new IllegalArgumentException("No existe un plan de tratamiento activo para el paciente");
        }

        // Validar que las sesiones esten en la semana correcta
        validarSesionesEnSemana(sesionesPropuestas, fechaInicioDeSemana);

        // Calcular cumplimiento de horas
        Map<String, Integer> horasAsignadas = calcularHorasAsignadasPorTipo(sesionesPropuestas);
        Map<String, Integer> horasRequeridas = plan.getHorasSemanalesPorTipo();
        
        ResultadoPlanificacion resultado = new ResultadoPlanificacion();
        resultado.setHorasRequeridas(horasRequeridas);
        resultado.setHorasAsignadas(horasAsignadas);
        resultado.setSesiones(sesionesPropuestas);

        // Verificar cumplimiento y generar alertas
        List<String> alertas = verificarCumplimientoHoras(horasRequeridas, horasAsignadas);
        resultado.setAlertas(alertas);

        // Si no hay alertas criticas, guardar las sesiones
        if (alertas.stream().noneMatch(alerta -> alerta.contains("CRiTICO"))) {
            guardarSesiones(sesionesPropuestas, pacienteId, fechaInicioDeSemana);
            resultado.setGuardadoExitoso(true);
            
            // Generar notificaciones a terapeutas
            generarNotificacionesCronograma(plan, sesionesPropuestas, fechaInicioDeSemana);
        } else {
            resultado.setGuardadoExitoso(false);
        }

        return resultado;
    }

    /**
     * Obtiene las sesiones programadas para una semana especifica de un paciente
     * @param pacienteId ID del paciente
     * @param fechaInicioDeSemana Fecha del lunes de la semana
     * @return Lista de sesiones de la semana
     * @throws SQLException si ocurre error en base de datos
     */
    public List<Sesion> obtenerSesionesSemana(int pacienteId, LocalDate fechaInicioDeSemana) throws SQLException {
        LocalDate fechaFin = fechaInicioDeSemana.plusDays(6);
        return sesionDAO.obtenerSesionesPorPacienteYRango(pacienteId, fechaInicioDeSemana, fechaFin);
    }

    /**
     * Obtiene el plan de tratamiento activo de un paciente
     * @param pacienteId ID del paciente
     * @return Plan activo o null si no existe
     * @throws SQLException si ocurre error en base de datos
     */
    public PlanTratamiento obtenerPlanActivo(int pacienteId) throws SQLException {
        return planTratamientoDAO.obtenerPlanActivoPorPaciente(pacienteId);
    }

    /**
     * Obtiene todos los planes de tratamiento activos
     * @return Lista de planes activos
     * @throws SQLException si ocurre error en base de datos
     */
    public List<PlanTratamiento> obtenerPlanesActivos() throws SQLException {
        return planTratamientoDAO.obtenerPlanesActivos();
    }

    /**
     * Actualiza un plan de tratamiento existente
     * @param plan Plan con datos actualizados
     * @return true si se actualizo exitosamente
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no tiene permisos
     */
    public boolean actualizarPlanTratamiento(PlanTratamiento plan) throws SQLException, SecurityException {
        if (!usuarioActual.getRole().equals("MEDICO")) {
            throw new SecurityException("Solo los medicos pueden actualizar planes de tratamiento");
        }

        if (!plan.isValido()) {
            throw new IllegalArgumentException("El plan de tratamiento no es valido");
        }

        planTratamientoDAO.actualizarPlanTratamiento(plan);
        generarNotificacionPlanActualizado(plan);
        
        return true;
    }

    /**
     * Valida que todas las sesiones esten dentro de la semana especificada
     */
    private void validarSesionesEnSemana(List<Sesion> sesiones, LocalDate fechaInicioDeSemana) {
        LocalDate fechaFin = fechaInicioDeSemana.plusDays(6);
        
        for (Sesion sesion : sesiones) {
            LocalDate fechaSesion = sesion.getFechaHora().toLocalDate();
            if (fechaSesion.isBefore(fechaInicioDeSemana) || fechaSesion.isAfter(fechaFin)) {
                throw new IllegalArgumentException("La sesion del " + fechaSesion + 
                    " no esta dentro de la semana del " + fechaInicioDeSemana);
            }
        }
    }

    /**
     * Calcula las horas asignadas por tipo de terapia
     */
    private Map<String, Integer> calcularHorasAsignadasPorTipo(List<Sesion> sesiones) {
        Map<String, Integer> horasAsignadas = new HashMap<>();
        
        for (Sesion sesion : sesiones) {
            String tipo = sesion.getTipoTerapia();
            int duracionHoras = sesion.getDuracion() / 60; // Convertir minutos a horas
            horasAsignadas.merge(tipo, duracionHoras, Integer::sum);
        }
        
        return horasAsignadas;
    }

    /**
     * Verifica el cumplimiento de horas y genera alertas
     */
    private List<String> verificarCumplimientoHoras(Map<String, Integer> horasRequeridas, 
                                                   Map<String, Integer> horasAsignadas) {
        List<String> alertas = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : horasRequeridas.entrySet()) {
            String tipo = entry.getKey();
            int requeridas = entry.getValue();
            int asignadas = horasAsignadas.getOrDefault(tipo, 0);
            
            if (asignadas < requeridas) {
                if (asignadas == 0) {
                    alertas.add("CRiTICO: No se asignaron sesiones de " + tipo + 
                               " (requeridas: " + requeridas + " horas)");
                } else {
                    alertas.add("ADVERTENCIA: " + tipo + " tiene " + asignadas + 
                               " horas asignadas de " + requeridas + " requeridas");
                }
            } else if (asignadas > requeridas) {
                alertas.add("INFORMACION: " + tipo + " tiene " + asignadas + 
                           " horas asignadas, " + (asignadas - requeridas) + " mas de las requeridas");
            }
        }
        
        return alertas;
    }

    /**
     * Guarda las sesiones en la base de datos
     */
    private void guardarSesiones(List<Sesion> sesiones, int pacienteId, LocalDate fechaInicioDeSemana) throws SQLException {
        // Primero eliminar sesiones existentes de la semana
        LocalDate fechaFin = fechaInicioDeSemana.plusDays(6);
        sesionDAO.eliminarSesionesPorPacienteYRango(pacienteId, fechaInicioDeSemana, fechaFin);
        
        // Crear nuevas sesiones
        for (Sesion sesion : sesiones) {
            sesionDAO.crearSesion(sesion);
        }
    }

    /**
     * Genera notificacion cuando se crea un plan
     */
    private void generarNotificacionPlanCreado(PlanTratamiento plan) {
        try {
            notificacionController.notificarPlanCreado(
                plan.getPacienteId(), 
                plan.getNombrePaciente(), 
                plan.getTotalHorasSemanales()
            );
        } catch (SQLException e) {
            System.err.println("Error al generar notificacion de plan creado: " + e.getMessage());
        }
    }

    /**
     * Genera notificaciones cuando se actualiza un plan
     */
    private void generarNotificacionPlanActualizado(PlanTratamiento plan) {
        try {
            String cambios = "Plan de tratamiento actualizado. Revisar nuevas asignaciones de cronograma.";
            notificacionController.notificarPacienteActualizado(
                plan.getPacienteId(), 
                plan.getNombrePaciente(), 
                cambios
            );
        } catch (SQLException e) {
            System.err.println("Error al generar notificacion de plan actualizado: " + e.getMessage());
        }
    }

    /**
     * Genera notificaciones a terapeutas cuando se modifica un cronograma
     */
    private void generarNotificacionesCronograma(PlanTratamiento plan, List<Sesion> sesiones, LocalDate fechaSemana) {
        try {
            // Obtener lista de terapeutas afectados
            List<String> terapeutasAfectados = sesiones.stream()
                .map(Sesion::getTerapeuta)
                .distinct()
                .collect(Collectors.toList());
            
            notificacionController.notificarCambioCronograma(
                plan.getPacienteId(), 
                plan.getNombrePaciente(), 
                terapeutasAfectados
            );
        } catch (SQLException e) {
            System.err.println("Error al generar notificaciones de cronograma: " + e.getMessage());
        }
    }

    /**
     * Clase interna para encapsular el resultado de una planificacion
     */
    public static class ResultadoPlanificacion {
        private Map<String, Integer> horasRequeridas;
        private Map<String, Integer> horasAsignadas;
        private List<Sesion> sesiones;
        private List<String> alertas;
        private boolean guardadoExitoso;

        // Getters y setters
        public Map<String, Integer> getHorasRequeridas() { return horasRequeridas; }
        public void setHorasRequeridas(Map<String, Integer> horasRequeridas) { this.horasRequeridas = horasRequeridas; }
        
        public Map<String, Integer> getHorasAsignadas() { return horasAsignadas; }
        public void setHorasAsignadas(Map<String, Integer> horasAsignadas) { this.horasAsignadas = horasAsignadas; }
        
        public List<Sesion> getSesiones() { return sesiones; }
        public void setSesiones(List<Sesion> sesiones) { this.sesiones = sesiones; }
        
        public List<String> getAlertas() { return alertas; }
        public void setAlertas(List<String> alertas) { this.alertas = alertas; }
        
        public boolean isGuardadoExitoso() { return guardadoExitoso; }
        public void setGuardadoExitoso(boolean guardadoExitoso) { this.guardadoExitoso = guardadoExitoso; }
        
        public boolean tieneAlertas() { return alertas != null && !alertas.isEmpty(); }
        public boolean tieneAlertasCriticas() { 
            return alertas != null && alertas.stream().anyMatch(a -> a.contains("CRiTICO"));
        }
    }
} 