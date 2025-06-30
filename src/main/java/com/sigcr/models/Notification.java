package com.sigcr.models;

import java.time.LocalDateTime;

/**
 * Clase abstracta base para las notificaciones del sistema SIGCR.
 * Define la estructura comun y comportamiento base que deben implementar
 * todas las notificaciones del sistema.
 */
public abstract class Notification {
    protected int id;
    protected Integer pacienteId;  // Puede ser null para notificaciones generales
    protected String mensaje;
    protected LocalDateTime fechaHora;
    protected boolean leida;
    protected TipoNotificacion tipo;
    protected RolDestinatario destinatarioRol;

    /**
     * Enumeracion de tipos de notificacion
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
     * Enumeracion de roles destinatarios
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
    public Notification(Integer pacienteId, String mensaje, TipoNotificacion tipo, RolDestinatario destinatarioRol) {
        this.pacienteId = pacienteId;
        this.mensaje = mensaje;
        this.fechaHora = LocalDateTime.now();
        this.leida = false;
        this.tipo = tipo;
        this.destinatarioRol = destinatarioRol;
    }

    /**
     * Constructor para notificaciones generales sin paciente especifico
     */
    public Notification(String mensaje, TipoNotificacion tipo, RolDestinatario destinatarioRol) {
        this(null, mensaje, tipo, destinatarioRol);
    }

    /**
     * Constructor simplificado para compatibilidad con codigo existente
     */
    public Notification(int pacienteId, String mensaje) {
        this(pacienteId, mensaje, TipoNotificacion.GENERAL, RolDestinatario.TODOS);
    }

    /**
     * Constructor vacio para uso de DAO
     */
    public Notification() {
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
     * Marca la notificacion como leida
     */
    public void marcarComoLeida() {
        this.leida = true;
    }

    /**
     * Obtiene una descripcion del tipo de notificacion
     */
    public String getDescripcionTipo() {
        switch (tipo) {
            case PACIENTE_CREADO: return "Paciente Creado";
            case PACIENTE_ACTUALIZADO: return "Paciente Actualizado";
            case PACIENTE_BAJA: return "Paciente Dado de Baja";
            case CRONOGRAMA_CAMBIO: return "Cambio en Cronograma";
            case PLAN_CREADO: return "Plan de Tratamiento Creado";
            case PLAN_ACTUALIZADO: return "Plan de Tratamiento Actualizado";
            case GENERAL: default: return "Notificacion General";
        }
    }

    /**
     * Obtiene el icono asociado al tipo de notificacion (para la UI)
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
        return String.format("Notificacion[%d]: %s [%s] - %s", 
            id, getDescripcionTipo(), destinatarioRol, mensaje);
    }
} 