package com.sigcr.controllers;

import com.sigcr.dao.PacienteDAO;
import com.sigcr.models.Paciente;
import com.sigcr.models.User;
import com.sigcr.services.ReporteService;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador que maneja toda la logica de negocio para la generacion de reportes de evolucion (CU-05).
 * Coordina la creacion de diferentes tipos de reportes con validaciones de permisos,
 * fechas y datos segun el rol del usuario.
 */
public class ReporteController {
    
    private ReporteService reporteService;
    private PacienteDAO pacienteDAO;
    private User usuarioActual;

    /**
     * Constructor del controlador de reportes
     * @param conn Conexion a la base de datos
     * @param usuarioActual Usuario que esta realizando las operaciones
     */
    public ReporteController(Connection conn, User usuarioActual) {
        this.reporteService = new ReporteService(conn);
        this.pacienteDAO = new PacienteDAO(conn);
        this.usuarioActual = usuarioActual;
    }

    /**
     * Genera un reporte de evolucion basico para un paciente
     * @param pacienteId ID del paciente
     * @param desde Fecha inicial del periodo
     * @param hasta Fecha final del periodo
     * @return Contenido del reporte como string
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no tiene permisos
     * @throws IllegalArgumentException si los parametros son invalidos
     */
    public String generarReporteBasico(int pacienteId, LocalDate desde, LocalDate hasta) 
            throws SQLException, SecurityException, IllegalArgumentException {
        
        // Validar permisos (MEDICO, ADMIN y TERAPEUTA pueden generar reportes basicos)
        if (!usuarioActual.getRole().equals("MEDICO") && 
            !usuarioActual.getRole().equals("ADMIN") && 
            !usuarioActual.getRole().equals("TERAPEUTA")) {
            throw new SecurityException("No tiene permisos para generar reportes de pacientes");
        }

        // Validar parametros
        validarParametrosReporte(pacienteId, desde, hasta);

        // Generar reporte basico usando el metodo existente
        return reporteService.generarReporteEvolucionCompleto(pacienteId, desde, hasta);
    }

    /**
     * Genera un reporte completo de evolucion para un paciente
     * @param pacienteId ID del paciente
     * @param desde Fecha inicial del periodo
     * @param hasta Fecha final del periodo
     * @return Contenido del reporte completo como string
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no tiene permisos
     * @throws IllegalArgumentException si los parametros son invalidos
     */
    public String generarReporteCompleto(int pacienteId, LocalDate desde, LocalDate hasta) 
            throws SQLException, SecurityException, IllegalArgumentException {
        
        // Validar permisos (solo MEDICO y ADMIN pueden generar reportes completos)
        if (!usuarioActual.getRole().equals("MEDICO") && !usuarioActual.getRole().equals("ADMIN")) {
            throw new SecurityException("Solo medicos y administradores pueden generar reportes completos");
        }

        // Validar parametros
        validarParametrosReporte(pacienteId, desde, hasta);

        // Generar reporte completo
        return reporteService.generarReporteEvolucionCompleto(pacienteId, desde, hasta);
    }

    /**
     * Genera un reporte resumido de multiples pacientes
     * @param pacientesIds Lista de IDs de pacientes
     * @param desde Fecha inicial del periodo
     * @param hasta Fecha final del periodo
     * @return Contenido del reporte resumido como string
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no tiene permisos
     * @throws IllegalArgumentException si los parametros son invalidos
     */
    public String generarReporteMultiplesPacientes(List<Integer> pacientesIds, LocalDate desde, LocalDate hasta) 
            throws SQLException, SecurityException, IllegalArgumentException {
        
        // Validar permisos (solo MEDICO y ADMIN)
        if (!usuarioActual.getRole().equals("MEDICO") && !usuarioActual.getRole().equals("ADMIN")) {
            throw new SecurityException("Solo medicos y administradores pueden generar reportes de multiples pacientes");
        }

        // Validar parametros
        if (pacientesIds == null || pacientesIds.isEmpty()) {
            throw new IllegalArgumentException("Debe especificar al menos un paciente");
        }

        if (pacientesIds.size() > 50) {
            throw new IllegalArgumentException("No se pueden generar reportes de mas de 50 pacientes a la vez");
        }

        validarRangoFechas(desde, hasta);

        // Validar que todos los pacientes existen
        for (int pacienteId : pacientesIds) {
            Paciente paciente = pacienteDAO.getPaciente(pacienteId);
            if (paciente == null) {
                throw new IllegalArgumentException("El paciente con ID " + pacienteId + " no existe");
            }
        }

        // Generar reporte
        return reporteService.generarReporteResumidoMultiplesPacientes(pacientesIds, desde, hasta);
    }

    /**
     * Genera un reporte estadistico por tipo de terapia
     * @param desde Fecha inicial del periodo
     * @param hasta Fecha final del periodo
     * @return Contenido del reporte estadistico como string
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no tiene permisos
     * @throws IllegalArgumentException si los parametros son invalidos
     */
    public String generarReporteEstadisticoPorTerapia(LocalDate desde, LocalDate hasta) 
            throws SQLException, SecurityException, IllegalArgumentException {
        
        // Validar permisos (solo ADMIN puede generar reportes estadisticos generales)
        if (!usuarioActual.getRole().equals("ADMIN")) {
            throw new SecurityException("Solo los administradores pueden generar reportes estadisticos por tipo de terapia");
        }

        // Validar fechas
        validarRangoFechas(desde, hasta);

        // Generar reporte
        return reporteService.generarReporteEstadisticoPorTerapia(desde, hasta);
    }

    /**
     * Obtiene la lista de pacientes disponibles para reportes segun el rol del usuario
     * @return Lista de pacientes que el usuario puede incluir en reportes
     * @throws SQLException si ocurre error en base de datos
     */
    public List<Paciente> obtenerPacientesDisponibles() throws SQLException {
        // ADMIN y MEDICO pueden ver todos los pacientes activos
        if (usuarioActual.getRole().equals("ADMIN") || usuarioActual.getRole().equals("MEDICO")) {
            return pacienteDAO.getPacientesPorEstado("Activo");
        }
        
        // TERAPEUTA puede ver todos los pacientes (para reportes basicos)
        if (usuarioActual.getRole().equals("TERAPEUTA")) {
            return pacienteDAO.getPacientesPorEstado("Activo");
        }
        
        // Otros roles no tienen acceso
        throw new SecurityException("No tiene permisos para acceder a la lista de pacientes");
    }

    /**
     * Obtiene estadisticas rapidas para el dashboard
     * @return Objeto con estadisticas resumidas
     * @throws SQLException si ocurre error en base de datos
     * @throws SecurityException si el usuario no tiene permisos
     */
    public EstadisticasReporte obtenerEstadisticasRapidas() throws SQLException, SecurityException {
        // Solo ADMIN y MEDICO pueden ver estadisticas generales
        if (!usuarioActual.getRole().equals("ADMIN") && !usuarioActual.getRole().equals("MEDICO")) {
            throw new SecurityException("No tiene permisos para ver estadisticas generales");
        }

        try {
            // Estadisticas del ultimo mes
            LocalDate hasta = LocalDate.now();
            LocalDate desde = hasta.minusMonths(1);
            
            // Obtener pacientes activos
            List<Paciente> pacientesActivos = pacienteDAO.getPacientesPorEstado("Activo");
            
            // Generar reporte estadistico rapido
            String reporteEstadistico = reporteService.generarReporteEstadisticoPorTerapia(desde, hasta);
            
            return new EstadisticasReporte(
                pacientesActivos.size(),
                extraerNumeroSesiones(reporteEstadistico),
                extraerTiposTerapia(reporteEstadistico),
                desde,
                hasta
            );
            
        } catch (Exception e) {
            // Si hay error, devolver estadisticas basicas
            List<Paciente> pacientesActivos = pacienteDAO.getPacientesPorEstado("Activo");
            return new EstadisticasReporte(
                pacientesActivos.size(),
                0,
                0,
                LocalDate.now().minusMonths(1),
                LocalDate.now()
            );
        }
    }

    /**
     * Valida los parametros comunes para reportes de paciente individual
     */
    private void validarParametrosReporte(int pacienteId, LocalDate desde, LocalDate hasta) 
            throws SQLException, IllegalArgumentException {
        
        // Validar que el paciente existe
        Paciente paciente = pacienteDAO.getPaciente(pacienteId);
        if (paciente == null) {
            throw new IllegalArgumentException("El paciente con ID " + pacienteId + " no existe");
        }

        // Validar rango de fechas
        validarRangoFechas(desde, hasta);
    }

    /**
     * Valida el rango de fechas para reportes
     */
    private void validarRangoFechas(LocalDate desde, LocalDate hasta) throws IllegalArgumentException {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }

        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior o igual a la fecha de fin");
        }

        if (desde.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha actual");
        }

        // Limitar a maximo 1 año de diferencia
        if (desde.isBefore(hasta.minusYears(1))) {
            throw new IllegalArgumentException("El rango de fechas no puede ser superior a 1 año");
        }
    }

    /**
     * Extrae el numero total de sesiones de un reporte estadistico
     */
    private int extraerNumeroSesiones(String reporteEstadistico) {
        try {
            // Buscar linea "Total de sesiones: X"
            String[] lineas = reporteEstadistico.split("\n");
            for (String linea : lineas) {
                if (linea.contains("Total de sesiones:")) {
                    String numero = linea.replaceAll(".*Total de sesiones:\\s*(\\d+).*", "$1");
                    return Integer.parseInt(numero);
                }
            }
        } catch (Exception e) {
            // Ignorar errores de parsing
        }
        return 0;
    }

    /**
     * Extrae el numero de tipos de terapia diferentes de un reporte estadistico
     */
    private int extraerTiposTerapia(String reporteEstadistico) {
        try {
            // Buscar linea "Tipos de terapia diferentes: X"
            String[] lineas = reporteEstadistico.split("\n");
            for (String linea : lineas) {
                if (linea.contains("Tipos de terapia diferentes:")) {
                    String numero = linea.replaceAll(".*Tipos de terapia diferentes:\\s*(\\d+).*", "$1");
                    return Integer.parseInt(numero);
                }
            }
        } catch (Exception e) {
            // Ignorar errores de parsing
        }
        return 0;
    }

    /**
     * Genera una sugerencia de rango de fechas para reportes
     * @param tipoReporte Tipo de reporte ("semanal", "mensual", "trimestral")
     * @return Array con [fechaDesde, fechaHasta]
     */
    public LocalDate[] sugerirRangoFechas(String tipoReporte) {
        LocalDate hasta = LocalDate.now();
        LocalDate desde;
        
        switch (tipoReporte.toLowerCase()) {
            case "semanal":
                desde = hasta.minusWeeks(1);
                break;
            case "mensual":
                desde = hasta.minusMonths(1);
                break;
            case "trimestral":
                desde = hasta.minusMonths(3);
                break;
            case "semestral":
                desde = hasta.minusMonths(6);
                break;
            default:
                desde = hasta.minusMonths(1); // Por defecto, ultimo mes
        }
        
        return new LocalDate[]{desde, hasta};
    }

    /**
     * Formatea un reporte para exportacion con metadatos adicionales
     * @param contenidoReporte Contenido original del reporte
     * @param tipoReporte Tipo de reporte generado
     * @return Reporte formateado con metadatos
     */
    public String formatearParaExportacion(String contenidoReporte, String tipoReporte) {
        StringBuilder reporteFormateado = new StringBuilder();
        
        // Metadatos de exportacion
        reporteFormateado.append("SISTEMA INTEGRAL DE GESTION PARA CLiNICAS DE REHABILITACION (SIGCR)\n");
        reporteFormateado.append("=".repeat(80)).append("\n");
        reporteFormateado.append("Tipo de reporte: ").append(tipoReporte).append("\n");
        reporteFormateado.append("Generado por: ").append(usuarioActual.getUsername()).append(" (").append(usuarioActual.getRole()).append(")\n");
        reporteFormateado.append("Fecha de generacion: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        reporteFormateado.append("Hora de generacion: ").append(java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))).append("\n");
        reporteFormateado.append("=".repeat(80)).append("\n\n");
        
        // Contenido original del reporte
        reporteFormateado.append(contenidoReporte);
        
        // Pie de pagina
        reporteFormateado.append("\n").append("=".repeat(80)).append("\n");
        reporteFormateado.append("Este reporte es confidencial y esta destinado unicamente para uso medico profesional.\n");
        reporteFormateado.append("Prohibida su reproduccion o distribucion sin autorizacion.\n");
        reporteFormateado.append("=".repeat(80)).append("\n");
        
        return reporteFormateado.toString();
    }

    /**
     * Clase interna para estadisticas rapidas
     */
    public static class EstadisticasReporte {
        private final int totalPacientesActivos;
        private final int totalSesiones;
        private final int tiposTerapiaDiferentes;
        private final LocalDate periodoDesde;
        private final LocalDate periodoHasta;

        public EstadisticasReporte(int totalPacientesActivos, int totalSesiones, 
                                 int tiposTerapiaDiferentes, LocalDate periodoDesde, LocalDate periodoHasta) {
            this.totalPacientesActivos = totalPacientesActivos;
            this.totalSesiones = totalSesiones;
            this.tiposTerapiaDiferentes = tiposTerapiaDiferentes;
            this.periodoDesde = periodoDesde;
            this.periodoHasta = periodoHasta;
        }

        // Getters
        public int getTotalPacientesActivos() { return totalPacientesActivos; }
        public int getTotalSesiones() { return totalSesiones; }
        public int getTiposTerapiaDiferentes() { return tiposTerapiaDiferentes; }
        public LocalDate getPeriodoDesde() { return periodoDesde; }
        public LocalDate getPeriodoHasta() { return periodoHasta; }
        
        public String getResumen() {
            return String.format("Pacientes activos: %d | Sesiones (ultimo mes): %d | Tipos de terapia: %d", 
                               totalPacientesActivos, totalSesiones, tiposTerapiaDiferentes);
        }
    }
} 