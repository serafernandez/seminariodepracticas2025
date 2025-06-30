package com.sigcr.models;

import java.time.LocalDate;

/**
 * Modelo que representa a un paciente del sistema SIGCR.
 * Contiene toda la informacion personal, medica y administrativa necesaria
 * para la gestion integral del paciente en el centro de rehabilitacion.
 */
public class Paciente {
    private int id;
    private String nombre;
    private String documento;
    private LocalDate fechaNacimiento;
    private String diagnostico;
    private String habitacion;
    private String estado; // 'Activo', 'Alta', 'Baja'

    // Constructor completo
    public Paciente(int id, String nombre, String documento, LocalDate fechaNacimiento, 
                   String diagnostico, String habitacion, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.documento = documento;
        this.fechaNacimiento = fechaNacimiento;
        this.diagnostico = diagnostico;
        this.habitacion = habitacion;
        this.estado = estado;
    }

    // Constructor para creacion (sin ID)
    public Paciente(String nombre, String documento, LocalDate fechaNacimiento,
                   String diagnostico, String habitacion, String estado) {
        this(-1, nombre, documento, fechaNacimiento, diagnostico, habitacion, estado);
    }

    // Constructor con estado por defecto
    public Paciente(String nombre, String documento, LocalDate fechaNacimiento,
                   String diagnostico, String habitacion) {
        this(nombre, documento, fechaNacimiento, diagnostico, habitacion, "Activo");
    }

    // Constructor backward compatibility (para no romper codigo existente)
    public Paciente(int id, String nombre, String documento, String diagnostico, String habitacion) {
        this(id, nombre, documento, null, diagnostico, habitacion, "Activo");
    }

    public Paciente(String nombre, String documento, String diagnostico, String habitacion) {
        this(nombre, documento, null, diagnostico, habitacion, "Activo");
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getHabitacion() {
        return habitacion;
    }

    public void setHabitacion(String habitacion) {
        this.habitacion = habitacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    /**
     * Valida que los campos obligatorios del paciente esten completos
     * @return true si todos los campos obligatorios estan presentes
     */
    public boolean validarCamposObligatorios() {
        return nombre != null && !nombre.trim().isEmpty() &&
               documento != null && !documento.trim().isEmpty() &&
               diagnostico != null && !diagnostico.trim().isEmpty();
    }

    @Override
    public String toString() {
        return nombre + " (" + documento + ") - " + estado;
    }
}