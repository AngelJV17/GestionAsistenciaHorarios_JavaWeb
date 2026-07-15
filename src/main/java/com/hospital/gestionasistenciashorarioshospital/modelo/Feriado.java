package com.hospital.gestionasistenciashorarioshospital.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "feriados")
public class Feriado implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate fecha;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "es_recuperable", nullable = false)
    private Boolean esRecuperable = false;

    @PrePersist
    public void prePersist() {
        if (esRecuperable == null) {
            esRecuperable = false;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Boolean getEsRecuperable() {
        return esRecuperable;
    }

    public void setEsRecuperable(Boolean esRecuperable) {
        this.esRecuperable = esRecuperable;
    }
}
