package com.sigcr.models;

import java.time.LocalDateTime;

/**
 * Modelo completo para las notificaciones del sistema SIGCR (CU-04).
 * Representa las notificaciones que se generan por eventos del sistema
 * y se envían a diferentes roles de usuarios según el contexto.
 */
public class Notificacion {
    private int id;
    private Integer pacienteId;  // Puede ser null para notificaciones generales
    private String mensaje;
    private LocalDateTime fechaHora;
    private boolean leida;
    private TipoNotificacion tipo;
    private RolDestinatario destinatarioRol;

    /**
     * Enumeración de tipos de notificación
     */
    public enum TipoNotificacion {
        PACIENTE_CREADO,
        PACIENTE_ACTUALIZADO,
        PACIENTE_BAJA,
        CRONOGRAMA_CAMBIO,
        PLAN_CREADO,
        PLAN_ACTUALIZADO,
        GENERAL
    }

    /**
     * Enumeración de roles destinatarios
     */
    public enum RolDestinatario {
        ADMIN,
        MEDICO,
        TERAPEUTA,
        ENFERMERIA,
        TODOS
    }

    /**
     * Constructor completo
     */
    public Notificacion(Integer pacienteId, String mensaje, TipoNotificacion tipo, RolDestinatario destinatarioRol) {
        this.pacienteId = pacienteId;
        this.mensaje = mensaje;
        this.fechaHora = LocalDateTime.now();
        this.leida = false;
        this.tipo = tipo;
        this.destinatarioRol = destinatarioRol;
    }

    /**
     * Constructor para notificaciones generales sin paciente específico
     */
    public Notificacion(String mensaje, TipoNotificacion tipo, RolDestinatario destinatarioRol) {
        this(null, mensaje, tipo, destinatarioRol);
    }

    /**
     * Constructor simplificado para compatibilidad con código existente
     */
    public Notificacion(int pacienteId, String mensaje) {
        this(pacienteId, mensaje, TipoNotificacion.GENERAL, RolDestinatario.TODOS);
    }

    /**
     * Constructor vacío para uso de DAO
     */
    public Notificacion() {
        this.fechaHora = LocalDateTime.now();
        this.leida = false;
        this.tipo = TipoNotificacion.GENERAL;
        this.destinatarioRol = RolDestinatario.TODOS;
    }

    // Getters y setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Integer pacienteId) {
        this.pacienteId = pacienteId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoNotificacion tipo) {
        this.tipo = tipo;
    }

    public RolDestinatario getDestinatarioRol() {
        return destinatarioRol;
    }

    public void setDestinatarioRol(RolDestinatario destinatarioRol) {
        this.destinatarioRol = destinatarioRol;
    }

    /**
     * Marca la notificación como leída
     */
    public void marcarComoLeida() {
        this.leida = true;
    }

    /**
     * Obtiene una descripción del tipo de notificación
     */
    public String getDescripcionTipo() {
        switch (tipo) {
            case PACIENTE_CREADO: return "Paciente Creado";
            case PACIENTE_ACTUALIZADO: return "Paciente Actualizado";
            case PACIENTE_BAJA: return "Paciente Dado de Baja";
            case CRONOGRAMA_CAMBIO: return "Cambio en Cronograma";
            case PLAN_CREADO: return "Plan de Tratamiento Creado";
            case PLAN_ACTUALIZADO: return "Plan de Tratamiento Actualizado";
            case GENERAL: default: return "Notificación General";
        }
    }

    /**
     * Obtiene el ícono asociado al tipo de notificación (para la UI)
     */
    public String getIconoTipo() {
        switch (tipo) {
            case PACIENTE_CREADO: return "👤";
            case PACIENTE_ACTUALIZADO: return "✏️";
            case PACIENTE_BAJA: return "❌";
            case CRONOGRAMA_CAMBIO: return "📅";
            case PLAN_CREADO: return "📋";
            case PLAN_ACTUALIZADO: return "🔄";
            case GENERAL: default: return "ℹ️";
        }
    }

    @Override
    public String toString() {
        return String.format("Notificación[%d]: %s [%s] - %s", 
            id, getDescripcionTipo(), destinatarioRol, mensaje);
    }
}