package com.sigcr.services;

import com.sigcr.dao.SesionDAO;
import com.sigcr.dao.PacienteDAO;
import com.sigcr.dao.PlanTratamientoDAO;
import com.sigcr.models.Sesion;
import com.sigcr.models.Paciente;
import com.sigcr.models.PlanTratamiento;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio completo para la generación de reportes de evolución de pacientes (CU-05).
 * Proporciona diferentes tipos de reportes con estadísticas detalladas
 * sobre el progreso terapéutico y cumplimiento de planes de tratamiento.
 */
public class ReporteService {
    private SesionDAO sesionDAO;
    private PacienteDAO pacienteDAO;
    private PlanTratamientoDAO planTratamientoDAO;

    public ReporteService(Connection conn) {
        this.sesionDAO = new SesionDAO(conn);
        this.pacienteDAO = new PacienteDAO(conn);
        this.planTratamientoDAO = new PlanTratamientoDAO(conn);
    }

    /**
     * Genera un reporte completo de evolución de un paciente
     * @param pacienteId ID del paciente
     * @param desde Fecha inicial del periodo
     * @param hasta Fecha final del periodo
     * @return Reporte detallado como string
     * @throws SQLException si ocurre error en consultas
     */
    public String generarReporteEvolucionCompleto(int pacienteId, LocalDate desde, LocalDate hasta) throws SQLException {
        // Obtener datos básicos
        Paciente paciente = pacienteDAO.getPaciente(pacienteId);
        if (paciente == null) {
            throw new IllegalArgumentException("Paciente no encontrado");
        }

        List<Sesion> sesiones = sesionDAO.obtenerSesionesPorPacienteYRango(pacienteId, desde, hasta);
        PlanTratamiento planActual = planTratamientoDAO.obtenerPlanActivoPorPaciente(pacienteId);

        // Generar reporte
        StringBuilder reporte = new StringBuilder();
        
        // Encabezado
        reporte.append("=".repeat(80)).append("\n");
        reporte.append("REPORTE DE EVOLUCIÓN DEL PACIENTE - SISTEMA SIGCR\n");
        reporte.append("=".repeat(80)).append("\n\n");
        
        // Información del paciente
        generarSeccionPaciente(reporte, paciente);
        
        // Periodo del reporte
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        reporte.append("PERIODO DEL REPORTE:\n");
        reporte.append("Desde: ").append(desde.format(formatter)).append("\n");
        reporte.append("Hasta: ").append(hasta.format(formatter)).append("\n\n");
        
        // Plan de tratamiento actual
        if (planActual != null) {
            generarSeccionPlanTratamiento(reporte, planActual);
        }
        
        // Estadísticas de sesiones
        generarEstadisticasSesiones(reporte, sesiones);
        
        // Detalle de sesiones por tipo de terapia
        generarDetallePorTipoTerapia(reporte, sesiones);
        
        // Evolución temporal
        generarEvolucionTemporal(reporte, sesiones);
        
        // Conclusiones y recomendaciones
        generarConclusiones(reporte, sesiones, planActual);
        
        return reporte.toString();
    }

    /**
     * Genera reporte resumido de múltiples pacientes
     * @param pacientesIds Lista de IDs de pacientes
     * @param desde Fecha inicial
     * @param hasta Fecha final
     * @return Reporte resumido
     * @throws SQLException si ocurre error en consultas
     */
    public String generarReporteResumidoMultiplesPacientes(List<Integer> pacientesIds, 
                                                          LocalDate desde, LocalDate hasta) throws SQLException {
        StringBuilder reporte = new StringBuilder();
        
        reporte.append("=".repeat(80)).append("\n");
        reporte.append("REPORTE RESUMIDO DE MÚLTIPLES PACIENTES - SISTEMA SIGCR\n");
        reporte.append("=".repeat(80)).append("\n\n");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        reporte.append("Periodo: ").append(desde.format(formatter))
               .append(" al ").append(hasta.format(formatter)).append("\n");
        reporte.append("Total de pacientes: ").append(pacientesIds.size()).append("\n\n");
        
        for (int pacienteId : pacientesIds) {
            try {
                Paciente paciente = pacienteDAO.getPaciente(pacienteId);
                List<Sesion> sesiones = sesionDAO.obtenerSesionesPorPacienteYRango(pacienteId, desde, hasta);
                
                reporte.append("-".repeat(50)).append("\n");
                reporte.append("PACIENTE: ").append(paciente.getNombre()).append(" (ID: ").append(pacienteId).append(")\n");
                reporte.append("Documento: ").append(paciente.getDocumento()).append("\n");
                reporte.append("Total de sesiones: ").append(sesiones.size()).append("\n");
                
                if (!sesiones.isEmpty()) {
                    Map<String, Long> sesionesPorTipo = sesiones.stream()
                        .collect(Collectors.groupingBy(Sesion::getTipoTerapia, Collectors.counting()));
                    
                    reporte.append("Tipos de terapia:\n");
                    sesionesPorTipo.forEach((tipo, cantidad) -> 
                        reporte.append("  - ").append(tipo).append(": ").append(cantidad).append(" sesiones\n"));
                }
                reporte.append("\n");
                
            } catch (Exception e) {
                reporte.append("Error al procesar paciente ID ").append(pacienteId).append(": ").append(e.getMessage()).append("\n\n");
            }
        }
        
        return reporte.toString();
    }

    /**
     * Genera reporte estadístico por tipo de terapia
     * @param desde Fecha inicial
     * @param hasta Fecha final
     * @return Reporte estadístico
     * @throws SQLException si ocurre error en consultas
     */
    public String generarReporteEstadisticoPorTerapia(LocalDate desde, LocalDate hasta) throws SQLException {
        StringBuilder reporte = new StringBuilder();
        
        reporte.append("=".repeat(80)).append("\n");
        reporte.append("REPORTE ESTADÍSTICO POR TIPO DE TERAPIA - SISTEMA SIGCR\n");
        reporte.append("=".repeat(80)).append("\n\n");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        reporte.append("Periodo: ").append(desde.format(formatter))
               .append(" al ").append(hasta.format(formatter)).append("\n\n");
        
        // Obtener todas las sesiones del periodo
        List<Sesion> todasLasSesiones = sesionDAO.obtenerSesionesPorRango(desde, hasta);
        
        if (todasLasSesiones.isEmpty()) {
            reporte.append("No se encontraron sesiones en el periodo especificado.\n");
            return reporte.toString();
        }
        
        // Estadísticas por tipo de terapia
        Map<String, List<Sesion>> sesionesPorTipo = todasLasSesiones.stream()
            .collect(Collectors.groupingBy(Sesion::getTipoTerapia));
        
        reporte.append("ESTADÍSTICAS POR TIPO DE TERAPIA:\n");
        reporte.append("-".repeat(50)).append("\n");
        
        for (Map.Entry<String, List<Sesion>> entry : sesionesPorTipo.entrySet()) {
            String tipoTerapia = entry.getKey();
            List<Sesion> sesiones = entry.getValue();
            
            reporte.append("\n").append(tipoTerapia.toUpperCase()).append(":\n");
            reporte.append("  Total de sesiones: ").append(sesiones.size()).append("\n");
            
            // Duración promedio
            double duracionPromedio = sesiones.stream()
                .mapToInt(Sesion::getDuracion)
                .average()
                .orElse(0.0);
            reporte.append("  Duración promedio: ").append(String.format("%.1f", duracionPromedio)).append(" minutos\n");
            
            // Total de minutos
            int totalMinutos = sesiones.stream()
                .mapToInt(Sesion::getDuracion)
                .sum();
            reporte.append("  Total de minutos: ").append(totalMinutos).append(" (").append(String.format("%.1f", totalMinutos/60.0)).append(" horas)\n");
            
            // Pacientes únicos
            long pacientesUnicos = sesiones.stream()
                .mapToInt(Sesion::getPacienteId)
                .distinct()
                .count();
            reporte.append("  Pacientes únicos atendidos: ").append(pacientesUnicos).append("\n");
            
            // Terapeutas únicos
            long terapeutasUnicos = sesiones.stream()
                .map(Sesion::getTerapeuta)
                .distinct()
                .count();
            reporte.append("  Terapeutas involucrados: ").append(terapeutasUnicos).append("\n");
        }
        
        // Resumen general
        reporte.append("\n").append("=".repeat(50)).append("\n");
        reporte.append("RESUMEN GENERAL:\n");
        reporte.append("Total de sesiones: ").append(todasLasSesiones.size()).append("\n");
        reporte.append("Tipos de terapia diferentes: ").append(sesionesPorTipo.size()).append("\n");
        
        long totalPacientes = todasLasSesiones.stream()
            .mapToInt(Sesion::getPacienteId)
            .distinct()
            .count();
        reporte.append("Total de pacientes atendidos: ").append(totalPacientes).append("\n");
        
        return reporte.toString();
    }

    /**
     * Genera información básica del paciente para el reporte
     */
    private void generarSeccionPaciente(StringBuilder reporte, Paciente paciente) {
        reporte.append("INFORMACIÓN DEL PACIENTE:\n");
        reporte.append("-".repeat(30)).append("\n");
        reporte.append("Nombre: ").append(paciente.getNombre()).append("\n");
        reporte.append("Documento: ").append(paciente.getDocumento()).append("\n");
        if (paciente.getFechaNacimiento() != null) {
            reporte.append("Fecha de nacimiento: ").append(paciente.getFechaNacimiento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        }
        reporte.append("Diagnóstico: ").append(paciente.getDiagnostico()).append("\n");
        if (paciente.getHabitacion() != null) {
            reporte.append("Habitación: ").append(paciente.getHabitacion()).append("\n");
        }
        reporte.append("Estado: ").append(paciente.getEstado()).append("\n\n");
    }

    /**
     * Genera información del plan de tratamiento
     */
    private void generarSeccionPlanTratamiento(StringBuilder reporte, PlanTratamiento plan) throws SQLException {
        reporte.append("PLAN DE TRATAMIENTO ACTUAL:\n");
        reporte.append("-".repeat(30)).append("\n");
        reporte.append("ID del Plan: ").append(plan.getId()).append("\n");
        reporte.append("Fecha de inicio: ").append(plan.getFechaInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        reporte.append("Fecha de fin: ").append(plan.getFechaFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        reporte.append("Estado: ").append(plan.getEstado()).append("\n");
        
        if (plan.getObservaciones() != null && !plan.getObservaciones().trim().isEmpty()) {
            reporte.append("Observaciones: ").append(plan.getObservaciones()).append("\n");
        }
        
        // Horas semanales requeridas
        Map<String, Integer> horasSemanales = plan.getHorasSemanalesPorTipo();
        if (!horasSemanales.isEmpty()) {
            reporte.append("Horas semanales requeridas:\n");
            horasSemanales.forEach((tipo, horas) -> 
                reporte.append("  - ").append(tipo).append(": ").append(horas).append(" horas\n"));
        }
        reporte.append("\n");
    }

    /**
     * Genera estadísticas generales de las sesiones
     */
    private void generarEstadisticasSesiones(StringBuilder reporte, List<Sesion> sesiones) {
        reporte.append("ESTADÍSTICAS GENERALES DE SESIONES:\n");
        reporte.append("-".repeat(40)).append("\n");
        reporte.append("Total de sesiones: ").append(sesiones.size()).append("\n");
        
        if (sesiones.isEmpty()) {
            reporte.append("No se registraron sesiones en el periodo especificado.\n\n");
            return;
        }
        
        // Sesiones por estado - usando estado genérico ya que Sesion no tiene getEstado()
        Map<String, Long> sesionesPorEstado = Map.of("Programadas", (long) sesiones.size());
        
        reporte.append("Sesiones por estado:\n");
        sesionesPorEstado.forEach((estado, cantidad) -> 
            reporte.append("  - ").append(estado).append(": ").append(cantidad).append("\n"));
        
        // Duración total y promedio
        int duracionTotal = sesiones.stream().mapToInt(Sesion::getDuracion).sum();
        double duracionPromedio = sesiones.stream().mapToInt(Sesion::getDuracion).average().orElse(0);
        
        reporte.append("Duración total: ").append(duracionTotal).append(" minutos (").append(String.format("%.1f", duracionTotal/60.0)).append(" horas)\n");
        reporte.append("Duración promedio por sesión: ").append(String.format("%.1f", duracionPromedio)).append(" minutos\n");
        
        // Terapeutas únicos
        long terapeutasUnicos = sesiones.stream().map(Sesion::getTerapeuta).distinct().count();
        reporte.append("Número de terapeutas involucrados: ").append(terapeutasUnicos).append("\n\n");
    }

    /**
     * Genera detalle por tipo de terapia
     */
    private void generarDetallePorTipoTerapia(StringBuilder reporte, List<Sesion> sesiones) {
        if (sesiones.isEmpty()) return;
        
        reporte.append("DETALLE POR TIPO DE TERAPIA:\n");
        reporte.append("-".repeat(40)).append("\n");
        
        Map<String, List<Sesion>> sesionesPorTipo = sesiones.stream()
            .collect(Collectors.groupingBy(Sesion::getTipoTerapia));
        
        for (Map.Entry<String, List<Sesion>> entry : sesionesPorTipo.entrySet()) {
            String tipoTerapia = entry.getKey();
            List<Sesion> sesionesDelTipo = entry.getValue();
            
            reporte.append("\n").append(tipoTerapia).append(":\n");
            reporte.append("  Número de sesiones: ").append(sesionesDelTipo.size()).append("\n");
            
            int duracionTotal = sesionesDelTipo.stream().mapToInt(Sesion::getDuracion).sum();
            reporte.append("  Duración total: ").append(duracionTotal).append(" minutos\n");
            
            // Terapeutas para este tipo
            List<String> terapeutas = sesionesDelTipo.stream()
                .map(Sesion::getTerapeuta)
                .distinct()
                .collect(Collectors.toList());
            reporte.append("  Terapeutas: ").append(String.join(", ", terapeutas)).append("\n");
        }
        reporte.append("\n");
    }

    /**
     * Genera análisis de evolución temporal
     */
    private void generarEvolucionTemporal(StringBuilder reporte, List<Sesion> sesiones) {
        if (sesiones.isEmpty()) return;
        
        reporte.append("EVOLUCIÓN TEMPORAL:\n");
        reporte.append("-".repeat(25)).append("\n");
        
        // Agrupar por semana
        Map<String, List<Sesion>> sesionesPorSemana = sesiones.stream()
            .collect(Collectors.groupingBy(s -> {
                LocalDate fecha = s.getFechaHora().toLocalDate();
                return "Semana del " + fecha.minusDays(fecha.getDayOfWeek().getValue() - 1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }));
        
        reporte.append("Distribución por semanas:\n");
        sesionesPorSemana.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                String semana = entry.getKey();
                List<Sesion> sesionesSemanales = entry.getValue();
                int duracionSemanal = sesionesSemanales.stream().mapToInt(Sesion::getDuracion).sum();
                
                reporte.append("  ").append(semana).append(": ")
                       .append(sesionesSemanales.size()).append(" sesiones, ")
                       .append(duracionSemanal).append(" minutos\n");
            });
        reporte.append("\n");
    }

    /**
     * Genera conclusiones y recomendaciones
     */
    private void generarConclusiones(StringBuilder reporte, List<Sesion> sesiones, PlanTratamiento plan) {
        reporte.append("CONCLUSIONES Y RECOMENDACIONES:\n");
        reporte.append("-".repeat(35)).append("\n");
        
        if (sesiones.isEmpty()) {
            reporte.append("- No se realizaron sesiones en el periodo evaluado.\n");
            reporte.append("- Se recomienda revisar el cronograma y asegurar el cumplimiento del plan.\n");
        } else {
            // Análisis de cumplimiento
            if (plan != null) {
                Map<String, Integer> horasRequeridasPlan = plan.getHorasSemanalesPorTipo();
                Map<String, List<Sesion>> sesionesPorTipo = sesiones.stream()
                    .collect(Collectors.groupingBy(Sesion::getTipoTerapia));
                
                reporte.append("Análisis de cumplimiento del plan:\n");
                for (Map.Entry<String, Integer> entry : horasRequeridasPlan.entrySet()) {
                    String tipo = entry.getKey();
                    int horasRequeridasTipo = entry.getValue();
                    
                    List<Sesion> sesionesDelTipo = sesionesPorTipo.getOrDefault(tipo, List.of());
                    int minutosRealizados = sesionesDelTipo.stream().mapToInt(Sesion::getDuracion).sum();
                    double horasRealizadas = minutosRealizados / 60.0;
                    
                    reporte.append("- ").append(tipo).append(": ")
                           .append(String.format("%.1f", horasRealizadas)).append("/").append(horasRequeridasTipo)
                           .append(" horas");
                    
                    if (horasRealizadas >= horasRequeridasTipo) {
                        reporte.append(" ✓ CUMPLIDO\n");
                    } else {
                        reporte.append(" ⚠ PENDIENTE\n");
                    }
                }
            }
            
            // Recomendaciones generales
            reporte.append("\nRecomendaciones:\n");
            reporte.append("- Continuar con el seguimiento regular del paciente.\n");
            reporte.append("- Mantener la comunicación entre el equipo terapéutico.\n");
            reporte.append("- Evaluar la efectividad de las terapias implementadas.\n");
        }
        
        reporte.append("\n").append("=".repeat(80)).append("\n");
        reporte.append("Fin del reporte - Generado el ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        reporte.append("=".repeat(80)).append("\n");
    }


}