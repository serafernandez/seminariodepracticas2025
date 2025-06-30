package com.sigcr.models;

import java.time.LocalDateTime;

/**
 * Modelo completo para las notificaciones del sistema SIGCR (CU-04).
 * Representa las notificaciones que se generan por eventos del sistema
 * y se envian a diferentes roles de usuarios segun el contexto.
 * 
 * Esta clase extiende la clase abstracta Notification y proporciona
 * la implementacion concreta para las notificaciones del sistema.
 */
public class Notificacion extends Notification {

    /**
     * Constructor completo
     */
    public Notificacion(Integer pacienteId, String mensaje, Notification.TipoNotificacion tipo, Notification.RolDestinatario destinatarioRol) {
        super(pacienteId, mensaje, tipo, destinatarioRol);
    }

    /**
     * Constructor para notificaciones generales sin paciente especifico
     */
    public Notificacion(String mensaje, Notification.TipoNotificacion tipo, Notification.RolDestinatario destinatarioRol) {
        super(mensaje, tipo, destinatarioRol);
    }

    /**
     * Constructor simplificado para compatibilidad con codigo existente
     */
    public Notificacion(int pacienteId, String mensaje) {
        super(pacienteId, mensaje);
    }

    /**
     * Constructor vacio para uso de DAO
     */
    public Notificacion() {
        super();
    }
}