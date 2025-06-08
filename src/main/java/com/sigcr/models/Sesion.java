package com.sigcr.models;

import java.time.LocalDateTime;

public class Sesion {
    private int id;
    private int pacienteId;
    private String terapeuta;
    private String tipoTerapia;
    private LocalDateTime fechaHora;
    private int duracion;

    public Sesion(int pacienteId, String terapeuta, String tipoTerapia, LocalDateTime fechaHora, int duracion) {
        this.pacienteId = pacienteId;
        this.terapeuta = terapeuta;
        this.tipoTerapia = tipoTerapia;
        this.fechaHora = fechaHora;
        this.duracion = duracion;
    }

    // Getters y setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPacienteId() {
        return pacienteId;
    }

    public String getTerapeuta() {
        return terapeuta;
    }

    public String getTipoTerapia() {
        return tipoTerapia;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public int getDuracion() {
        return duracion;
    }
}