package com.hospital.gestionasistenciashorarioshospital.modelo;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 50)
    private String nombreUsuario;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "correo_recuperacion", length = 150)
    private String correoRecuperacion;

    @Column(name = "estado_id")
    private Long estadoId;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    public Usuario() {
    }

    public Usuario(Long id, String nombreUsuario, String passwordHash, String correoRecuperacion, Long estadoId, LocalDateTime ultimoAcceso) {
        this.id = id;
        this.nombreUsuario = nombreUsuario;
        this.passwordHash = passwordHash;
        this.correoRecuperacion = correoRecuperacion;
        this.estadoId = estadoId;
        this.ultimoAcceso = ultimoAcceso;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getCorreoRecuperacion() {
        return correoRecuperacion;
    }

    public void setCorreoRecuperacion(String correoRecuperacion) {
        this.correoRecuperacion = correoRecuperacion;
    }

    public Long getEstadoId() {
        return estadoId;
    }

    public void setEstadoId(Long estadoId) {
        this.estadoId = estadoId;
    }

    public LocalDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }

    public void setUltimoAcceso(LocalDateTime ultimoAcceso) {
        this.ultimoAcceso = ultimoAcceso;
    }
}
