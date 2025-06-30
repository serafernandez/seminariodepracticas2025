package com.sigcr.models;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo que representa el plan de tratamiento de un paciente.
 * Define las horas semanales requeridas por cada tipo de terapia,
 * estableciendo la base para la planificacion de cronogramas (CU-02).
 */
public class PlanTratamiento {
    private int id;
    private int pacienteId;
    private String nombrePaciente; // Para facilitar visualizacion
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estado; // 'Activo', 'Completado', 'Suspendido'
    private String observaciones;
    
    // Mapa de tipo de terapia -> horas semanales requeridas
    private Map<String, Integer> horasSemanalesPorTipo;
    
    // Constructor completo
    public PlanTratamiento(int id, int pacienteId, String nombrePaciente, 
                          LocalDate fechaInicio, LocalDate fechaFin, String estado, String observaciones) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.nombrePaciente = nombrePaciente;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
        this.observaciones = observaciones;
        this.horasSemanalesPorTipo = new HashMap<>();
    }
    
    // Constructor para creacion (sin ID)
    public PlanTratamiento(int pacienteId, String nombrePaciente, 
                          LocalDate fechaInicio, LocalDate fechaFin, String observaciones) {
        this(-1, pacienteId, nombrePaciente, fechaInicio, fechaFin, "Activo", observaciones);
    }
    
    // Constructor minimo
    public PlanTratamiento(int pacienteId, String nombrePaciente) {
        this(pacienteId, nombrePaciente, LocalDate.now(), LocalDate.now().plusMonths(3), "");
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(int pacienteId) {
        this.pacienteId = pacienteId;
    }

    public String getNombrePaciente() {
        return nombrePaciente;
    }

    public void setNombrePaciente(String nombrePaciente) {
        this.nombrePaciente = nombrePaciente;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Map<String, Integer> getHorasSemanalesPorTipo() {
        return horasSemanalesPorTipo;
    }

    public void setHorasSemanalesPorTipo(Map<String, Integer> horasSemanalesPorTipo) {
        this.horasSemanalesPorTipo = horasSemanalesPorTipo;
    }

    /**
     * Establece las horas semanales para un tipo especifico de terapia
     * @param tipoTerapia Tipo de terapia (ej: "Fisioterapia", "Terapia Ocupacional")
     * @param horasSemanales Numero de horas semanales requeridas
     */
    public void setHorasSemanales(String tipoTerapia, int horasSemanales) {
        if (horasSemanales >= 0) {
            horasSemanalesPorTipo.put(tipoTerapia, horasSemanales);
        }
    }

    /**
     * Obtiene las horas semanales requeridas para un tipo de terapia
     * @param tipoTerapia Tipo de terapia
     * @return Horas semanales requeridas (0 si no esta definido)
     */
    public int getHorasSemanales(String tipoTerapia) {
        return horasSemanalesPorTipo.getOrDefault(tipoTerapia, 0);
    }

    /**
     * Elimina un tipo de terapia del plan
     * @param tipoTerapia Tipo de terapia a eliminar
     */
    public void removerTipoTerapia(String tipoTerapia) {
        horasSemanalesPorTipo.remove(tipoTerapia);
    }

    /**
     * Calcula el total de horas semanales del plan
     * @return Total de horas semanales
     */
    public int getTotalHorasSemanales() {
        return horasSemanalesPorTipo.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Verifica si el plan esta activo y vigente
     * @return true si esta activo y en fechas validas
     */
    public boolean isActivo() {
        LocalDate hoy = LocalDate.now();
        return "Activo".equals(estado) && 
               (fechaInicio == null || !hoy.isBefore(fechaInicio)) &&
               (fechaFin == null || !hoy.isAfter(fechaFin));
    }

    /**
     * Obtiene los tipos de terapia definidos en el plan
     * @return Set con los tipos de terapia
     */
    public java.util.Set<String> getTiposTerapia() {
        return horasSemanalesPorTipo.keySet();
    }

    /**
     * Valida que el plan tenga al menos un tipo de terapia definido
     * @return true si es valido
     */
    public boolean isValido() {
        return !horasSemanalesPorTipo.isEmpty() && 
               getTotalHorasSemanales() > 0 &&
               fechaInicio != null && 
               fechaFin != null &&
               !fechaFin.isBefore(fechaInicio);
    }

    @Override
    public String toString() {
        return String.format("Plan de %s - %d horas/semana (%s)", 
                           nombrePaciente, getTotalHorasSemanales(), estado);
    }
} 