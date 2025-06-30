package com.sigcr.controllers;

import com.sigcr.dao.NotificacionDAO;
import com.sigcr.dao.PacienteDAO;
import com.sigcr.models.Notificacion;
import com.sigcr.models.Notification.TipoNotificacion;
import com.sigcr.models.Notification.RolDestinatario;
import com.sigcr.models.Paciente;
import com.sigcr.models.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador que maneja toda la logica de negocio para el sistema de notificaciones (CU-04).
 * Coordina la creacion, consulta, marcado como leidas y gestion de notificaciones
 * segun el rol del usuario y el contexto del sistema.
 */
public class NotificacionController {
    
    private NotificacionDAO notificacionDAO;
    private PacienteDAO pacienteDAO;
    private User usuarioActual;

    /**
     * Constructor del controlador de notificaciones
     * @param conn Conexion a la base de datos
     * @param usuarioActual Usuario que esta realizando las operaciones
     */
    public NotificacionController(Connection conn, User usuarioActual) {
        this.notificacionDAO = new NotificacionDAO(conn);
        this.pacienteDAO = new PacienteDAO(conn);
        this.usuarioActual = usuarioActual;
    }

    /**
     * Crea una nueva notificacion del sistema
     * @param notificacion Notificacion a crear
     * @return true si se creo exitosamente
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no tiene permisos
     */
    public boolean crearNotificacion(Notificacion notificacion) throws SQLException, SecurityException {
        // Solo ADMIN y MEDICO pueden crear notificaciones generales
        if (notificacion.getTipo() == TipoNotificacion.GENERAL && 
            !usuarioActual.getRole().equals("ADMIN") && 
            !usuarioActual.getRole().equals("MEDICO")) {
            throw new SecurityException("Solo administradores y medicos pueden crear notificaciones generales");
        }

        // Validar que el mensaje no este vacio
        if (notificacion.getMensaje() == null || notificacion.getMensaje().trim().isEmpty()) {
            throw new IllegalArgumentException("El mensaje de la notificacion no puede estar vacio");
        }

        // Si hay paciente asociado, verificar que existe
        if (notificacion.getPacienteId() != null) {
            Paciente paciente = pacienteDAO.getPaciente(notificacion.getPacienteId());
            if (paciente == null) {
                throw new IllegalArgumentException("El paciente especificado no existe");
            }
        }

        notificacionDAO.crearNotificacion(notificacion);
        return true;
    }

    /**
     * Obtiene las notificaciones para el usuario actual segun su rol
     * @param soloNoLeidas Si solo obtener las no leidas
     * @return Lista de notificaciones
     * @throws SQLException si ocurre error en base de datos
     */
    public List<Notificacion> obtenerNotificacionesParaUsuarioActual(boolean soloNoLeidas) throws SQLException {
        RolDestinatario rolDestino = convertirStringARol(usuarioActual.getRole());
        
        if (soloNoLeidas) {
            return notificacionDAO.obtenerNotificacionesNoLeidas(rolDestino, true);
        } else {
            return notificacionDAO.obtenerNotificacionesPorRol(rolDestino, true);
        }
    }

    /**
     * Obtiene todas las notificaciones (solo para ADMIN)
     * @return Lista de todas las notificaciones
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no es administrador
     */
    public List<Notificacion> obtenerTodasLasNotificaciones() throws SQLException, SecurityException {
        if (!usuarioActual.getRole().equals("ADMIN")) {
            throw new SecurityException("Solo los administradores pueden ver todas las notificaciones");
        }
        
        return notificacionDAO.obtenerNotificacionesPorRol(RolDestinatario.TODOS, false)
                .stream()
                .sorted((a, b) -> b.getFechaHora().compareTo(a.getFechaHora()))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene notificaciones de un paciente especifico
     * @param pacienteId ID del paciente
     * @return Lista de notificaciones del paciente
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no tiene permisos
     */
    public List<Notificacion> obtenerNotificacionesPorPaciente(int pacienteId) throws SQLException, SecurityException {
        // Verificar que el paciente existe
        Paciente paciente = pacienteDAO.getPaciente(pacienteId);
        if (paciente == null) {
            throw new IllegalArgumentException("El paciente especificado no existe");
        }

        // Solo ADMIN y MEDICO pueden ver notificaciones especificas de pacientes
        if (!usuarioActual.getRole().equals("ADMIN") && !usuarioActual.getRole().equals("MEDICO")) {
            throw new SecurityException("Solo administradores y medicos pueden consultar notificaciones por paciente");
        }

        return notificacionDAO.obtenerNotificacionesPorPaciente(pacienteId);
    }

    /**
     * Marca una notificacion como leida
     * @param notificacionId ID de la notificacion
     * @return true si se marco exitosamente
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no tiene permisos
     */
    public boolean marcarComoLeida(int notificacionId) throws SQLException, SecurityException {
        // Obtener la notificacion para validar permisos
        Notificacion notificacion = notificacionDAO.obtenerNotificacionPorId(notificacionId);
        if (notificacion == null) {
            throw new IllegalArgumentException("La notificacion especificada no existe");
        }

        // Verificar que el usuario tiene permiso para marcar esta notificacion
        RolDestinatario rolUsuario = convertirStringARol(usuarioActual.getRole());
        if (notificacion.getDestinatarioRol() != RolDestinatario.TODOS && 
            notificacion.getDestinatarioRol() != rolUsuario && 
            !usuarioActual.getRole().equals("ADMIN")) {
            throw new SecurityException("No tiene permisos para marcar esta notificacion como leida");
        }

        notificacionDAO.marcarComoLeida(notificacionId);
        return true;
    }

    /**
     * Marca todas las notificaciones del usuario actual como leidas
     * @return true si se marcaron exitosamente
     * @throws SQLException si ocurre error en base de datos
     */
    public boolean marcarTodasComoLeidas() throws SQLException {
        RolDestinatario rolDestino = convertirStringARol(usuarioActual.getRole());
        notificacionDAO.marcarTodasComoLeidas(rolDestino, true);
        return true;
    }

    /**
     * Cuenta las notificaciones no leidas para el usuario actual
     * @return Numero de notificaciones no leidas
     * @throws SQLException si ocurre error en base de datos
     */
    public int contarNotificacionesNoLeidas() throws SQLException {
        RolDestinatario rolDestino = convertirStringARol(usuarioActual.getRole());
        return notificacionDAO.contarNotificacionesNoLeidas(rolDestino, true);
    }

    /**
     * Obtiene notificaciones por tipo (solo para ADMIN y MEDICO)
     * @param tipo Tipo de notificacion
     * @return Lista de notificaciones del tipo
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no tiene permisos
     */
    public List<Notificacion> obtenerNotificacionesPorTipo(TipoNotificacion tipo) throws SQLException, SecurityException {
        if (!usuarioActual.getRole().equals("ADMIN") && !usuarioActual.getRole().equals("MEDICO")) {
            throw new SecurityException("Solo administradores y medicos pueden filtrar notificaciones por tipo");
        }

        return notificacionDAO.obtenerNotificacionesPorTipo(tipo);
    }

    /**
     * Obtiene notificaciones en un rango de fechas (solo para ADMIN)
     * @param desde Fecha inicio
     * @param hasta Fecha fin
     * @return Lista de notificaciones en el rango
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no es administrador
     */
    public List<Notificacion> obtenerNotificacionesPorFecha(LocalDateTime desde, LocalDateTime hasta) throws SQLException, SecurityException {
        if (!usuarioActual.getRole().equals("ADMIN")) {
            throw new SecurityException("Solo los administradores pueden consultar notificaciones por fecha");
        }

        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }

        return notificacionDAO.obtenerNotificacionesPorFecha(desde, hasta);
    }

    /**
     * Limpia notificaciones antiguas (solo para ADMIN)
     * @param diasAntiguedad Dias de antigüedad a partir de los cuales eliminar
     * @return Numero de notificaciones eliminadas
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no es administrador
     */
    public int limpiarNotificacionesAntiguas(int diasAntiguedad) throws SQLException, SecurityException {
        if (!usuarioActual.getRole().equals("ADMIN")) {
            throw new SecurityException("Solo los administradores pueden limpiar notificaciones antiguas");
        }

        if (diasAntiguedad < 1) {
            throw new IllegalArgumentException("Los dias de antigüedad deben ser al menos 1");
        }

        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasAntiguedad);
        return notificacionDAO.limpiarNotificacionesAntiguas(fechaLimite);
    }

    /**
     * Crea notificaciones automaticas del sistema (para uso interno)
     * Estos metodos son llamados desde otros controladores
     */
    
    /**
     * Crea notificacion de paciente creado
     */
    public void notificarPacienteCreado(int pacienteId, String nombrePaciente) throws SQLException {
        String mensaje = String.format("Nuevo paciente registrado: %s. Requiere asignacion de plan terapeutico.", nombrePaciente);
        Notificacion notificacion = new Notificacion(
            pacienteId, 
            mensaje, 
            TipoNotificacion.PACIENTE_CREADO, 
            RolDestinatario.MEDICO
        );
        notificacionDAO.crearNotificacion(notificacion);
    }

    /**
     * Crea notificacion de paciente actualizado
     */
    public void notificarPacienteActualizado(int pacienteId, String nombrePaciente, String cambiosRealizados) throws SQLException {
        String mensaje = String.format("Paciente %s actualizado. Cambios: %s", nombrePaciente, cambiosRealizados);
        Notificacion notificacion = new Notificacion(
            pacienteId, 
            mensaje, 
            TipoNotificacion.PACIENTE_ACTUALIZADO, 
            RolDestinatario.MEDICO
        );
        notificacionDAO.crearNotificacion(notificacion);
    }

    /**
     * Crea notificacion de paciente dado de baja
     */
    public void notificarPacienteDadoDeBaja(int pacienteId, String nombrePaciente) throws SQLException {
        String mensaje = String.format("Paciente %s dado de baja. Cancelar sesiones pendientes.", nombrePaciente);
        Notificacion notificacion = new Notificacion(
            pacienteId, 
            mensaje, 
            TipoNotificacion.PACIENTE_BAJA, 
            RolDestinatario.TODOS
        );
        notificacionDAO.crearNotificacion(notificacion);
    }

    /**
     * Crea notificacion de cambio en cronograma
     */
    public void notificarCambioCronograma(int pacienteId, String nombrePaciente, List<String> terapeutasAfectados) throws SQLException {
        String terapeutas = String.join(", ", terapeutasAfectados);
        String mensaje = String.format("Cronograma modificado para %s. Terapeutas afectados: %s", nombrePaciente, terapeutas);
        Notificacion notificacion = new Notificacion(
            pacienteId, 
            mensaje, 
            TipoNotificacion.CRONOGRAMA_CAMBIO, 
            RolDestinatario.TERAPEUTA
        );
        notificacionDAO.crearNotificacion(notificacion);
    }

    /**
     * Crea notificacion de plan de tratamiento creado
     */
    public void notificarPlanCreado(int pacienteId, String nombrePaciente, int horasSemanales) throws SQLException {
        String mensaje = String.format("Plan de tratamiento creado para %s. Total: %d horas semanales.", nombrePaciente, horasSemanales);
        Notificacion notificacion = new Notificacion(
            pacienteId, 
            mensaje, 
            TipoNotificacion.PLAN_CREADO, 
            RolDestinatario.TERAPEUTA
        );
        notificacionDAO.crearNotificacion(notificacion);
    }

    /**
     * Convierte string de rol a enum
     */
    private RolDestinatario convertirStringARol(String rol) {
        try {
            return RolDestinatario.valueOf(rol);
        } catch (IllegalArgumentException e) {
            return RolDestinatario.TODOS;
        }
    }

    /**
     * Obtiene resumen de notificaciones para el dashboard
     */
    public ResumenNotificaciones obtenerResumen() throws SQLException {
        RolDestinatario rolDestino = convertirStringARol(usuarioActual.getRole());
        
        int noLeidas = notificacionDAO.contarNotificacionesNoLeidas(rolDestino, true);
        List<Notificacion> recientes = notificacionDAO.obtenerNotificacionesPorRol(rolDestino, true)
                .stream()
                .limit(5)
                .collect(Collectors.toList());
        
        return new ResumenNotificaciones(noLeidas, recientes);
    }

    /**
     * Clase interna para el resumen de notificaciones
     */
    public static class ResumenNotificaciones {
        private final int noLeidas;
        private final List<Notificacion> recientes;

        public ResumenNotificaciones(int noLeidas, List<Notificacion> recientes) {
            this.noLeidas = noLeidas;
            this.recientes = recientes;
        }

        public int getNoLeidas() { return noLeidas; }
        public List<Notificacion> getRecientes() { return recientes; }
    }
} 