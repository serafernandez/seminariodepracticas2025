package com.sigcr.controllers;

import com.sigcr.dao.NotificacionDAO;
import com.sigcr.dao.PacienteDAO;
import com.sigcr.models.Notificacion;
import com.sigcr.models.Paciente;
import com.sigcr.models.User;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Controlador que maneja toda la lógica de negocio para el mantenimiento de pacientes.
 * Implementa las operaciones del CU-01: Mantener Paciente, incluyendo validaciones,
 * control de acceso y generación de eventos para otros casos de uso.
 */
public class PacienteController {
    
    private PacienteDAO pacienteDAO;
    private NotificacionDAO notificacionDAO;
    private User usuarioActual;

    /**
     * Constructor del controlador de pacientes
     * @param conn Conexión a la base de datos
     * @param usuarioActual Usuario que está realizando las operaciones
     */
    public PacienteController(Connection conn, User usuarioActual) {
        this.pacienteDAO = new PacienteDAO(conn);
        this.notificacionDAO = new NotificacionDAO(conn);
        this.usuarioActual = usuarioActual;
    }

    /**
     * Crea un nuevo paciente validando permisos y datos
     * @param paciente Datos del paciente a crear
     * @return true si se creó exitosamente
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no tiene permisos
     * @throws IllegalArgumentException si los datos son inválidos
     */
    public boolean crearPaciente(Paciente paciente) throws SQLException, SecurityException, IllegalArgumentException {
        // Verificar permisos (solo ADMIN puede crear pacientes)
        if (!usuarioActual.getRole().equals("ADMIN")) {
            throw new SecurityException("Solo los administradores pueden crear pacientes");
        }

        // Validar campos obligatorios
        if (!paciente.validarCamposObligatorios()) {
            throw new IllegalArgumentException("Los campos nombre, documento y diagnóstico son obligatorios");
        }

        // Verificar que no exista un paciente con el mismo documento
        Paciente existente = pacienteDAO.buscarPacientePorDocumento(paciente.getDocumento());
        if (existente != null) {
            throw new IllegalArgumentException("Ya existe un paciente con el documento: " + paciente.getDocumento());
        }

        // Crear el paciente
        pacienteDAO.createPaciente(paciente);

        // Generar notificación para CU-02 (Planificar Cronograma)
        generarEventoPacienteCreado(paciente);

        return true;
    }

    /**
     * Actualiza los datos de un paciente existente
     * @param paciente Datos actualizados del paciente
     * @return true si se actualizó exitosamente
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no tiene permisos
     * @throws IllegalArgumentException si los datos son inválidos
     */
    public boolean actualizarPaciente(Paciente paciente) throws SQLException, SecurityException, IllegalArgumentException {
        // Verificar permisos (solo ADMIN puede actualizar pacientes)
        if (!usuarioActual.getRole().equals("ADMIN")) {
            throw new SecurityException("Solo los administradores pueden actualizar pacientes");
        }

        // Validar que el paciente existe
        Paciente existente = pacienteDAO.getPaciente(paciente.getId());
        if (existente == null) {
            throw new IllegalArgumentException("El paciente con ID " + paciente.getId() + " no existe");
        }

        // Validar campos obligatorios
        if (!paciente.validarCamposObligatorios()) {
            throw new IllegalArgumentException("Los campos nombre, documento y diagnóstico son obligatorios");
        }

        // Verificar que no exista otro paciente con el mismo documento (excepto él mismo)
        Paciente conMismoDoc = pacienteDAO.buscarPacientePorDocumento(paciente.getDocumento());
        if (conMismoDoc != null && conMismoDoc.getId() != paciente.getId()) {
            throw new IllegalArgumentException("Ya existe otro paciente con el documento: " + paciente.getDocumento());
        }

        // Actualizar el paciente
        pacienteDAO.updatePaciente(paciente);

        // Generar notificación de cambio
        generarEventoPacienteActualizado(paciente, existente);

        return true;
    }

    /**
     * Da de baja lógica a un paciente (CU-01: dar de baja)
     * @param pacienteId ID del paciente
     * @return true si se dio de baja exitosamente
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no tiene permisos
     */
    public boolean darDeBajaPaciente(int pacienteId) throws SQLException, SecurityException {
        // Verificar permisos (solo ADMIN puede dar de baja pacientes)
        if (!usuarioActual.getRole().equals("ADMIN")) {
            throw new SecurityException("Solo los administradores pueden dar de baja pacientes");
        }

        // Verificar que el paciente existe y está activo
        Paciente paciente = pacienteDAO.getPaciente(pacienteId);
        if (paciente == null) {
            throw new IllegalArgumentException("El paciente con ID " + pacienteId + " no existe");
        }

        if (paciente.getEstado().equals("Baja")) {
            throw new IllegalArgumentException("El paciente ya está dado de baja");
        }

        // Dar de baja
        pacienteDAO.darDeBajaPaciente(pacienteId);

        // Generar notificación
        generarEventoPacienteDadoDeBaja(paciente);

        return true;
    }

    /**
     * Busca pacientes por diferentes criterios
     * @param criterio Criterio de búsqueda ("nombre", "documento", "estado")
     * @param valor Valor a buscar
     * @return Lista de pacientes que coinciden
     * @throws SQLException si ocurre error en base de datos
     */
    public List<Paciente> buscarPacientes(String criterio, String valor) throws SQLException {
        switch (criterio.toLowerCase()) {
            case "nombre":
                return pacienteDAO.buscarPacientesPorNombre(valor);
            case "estado":
                return pacienteDAO.getPacientesPorEstado(valor);
            case "documento":
                Paciente paciente = pacienteDAO.buscarPacientePorDocumento(valor);
                return paciente != null ? List.of(paciente) : List.of();
            default:
                throw new IllegalArgumentException("Criterio de búsqueda no válido: " + criterio);
        }
    }

    /**
     * Obtiene todos los pacientes del sistema
     * @return Lista de todos los pacientes
     * @throws SQLException si ocurre error en base de datos
     */
    public List<Paciente> obtenerTodosPacientes() throws SQLException {
        return pacienteDAO.getAllPacientes();
    }

    /**
     * Obtiene un paciente por su ID
     * @param id ID del paciente
     * @return Paciente encontrado o null
     * @throws SQLException si ocurre error en base de datos
     */
    public Paciente obtenerPacientePorId(int id) throws SQLException {
        return pacienteDAO.getPaciente(id);
    }

    /**
     * Genera evento/notificación cuando se crea un nuevo paciente
     * @param paciente Paciente creado
     */
    private void generarEventoPacienteCreado(Paciente paciente) {
        try {
            String mensaje = String.format("Nuevo paciente registrado: %s (Doc: %s). Requiere asignación de plan terapéutico.", 
                                         paciente.getNombre(), paciente.getDocumento());
            Notificacion notificacion = new Notificacion(paciente.getId(), mensaje);
            notificacionDAO.crearNotificacion(notificacion);
        } catch (SQLException e) {
            System.err.println("Error al generar notificación de paciente creado: " + e.getMessage());
        }
    }

    /**
     * Genera evento/notificación cuando se actualiza un paciente
     * @param pacienteNuevo Datos actualizados
     * @param pacienteAnterior Datos anteriores
     */
    private void generarEventoPacienteActualizado(Paciente pacienteNuevo, Paciente pacienteAnterior) {
        try {
            StringBuilder cambios = new StringBuilder();
            
            if (!pacienteNuevo.getDiagnostico().equals(pacienteAnterior.getDiagnostico())) {
                cambios.append("Diagnóstico actualizado. ");
            }
            if (!pacienteNuevo.getHabitacion().equals(pacienteAnterior.getHabitacion())) {
                cambios.append("Habitación cambiada. ");
            }

            if (cambios.length() > 0) {
                String mensaje = String.format("Paciente %s actualizado: %s", 
                                             pacienteNuevo.getNombre(), cambios.toString());
                Notificacion notificacion = new Notificacion(pacienteNuevo.getId(), mensaje);
                notificacionDAO.crearNotificacion(notificacion);
            }
        } catch (SQLException e) {
            System.err.println("Error al generar notificación de paciente actualizado: " + e.getMessage());
        }
    }

    /**
     * Genera evento/notificación cuando se da de baja un paciente
     * @param paciente Paciente dado de baja
     */
    private void generarEventoPacienteDadoDeBaja(Paciente paciente) {
        try {
            String mensaje = String.format("Paciente %s dado de baja. Cancelar sesiones pendientes.", 
                                         paciente.getNombre());
            Notificacion notificacion = new Notificacion(paciente.getId(), mensaje);
            notificacionDAO.crearNotificacion(notificacion);
        } catch (SQLException e) {
            System.err.println("Error al generar notificación de paciente dado de baja: " + e.getMessage());
        }
    }
} 