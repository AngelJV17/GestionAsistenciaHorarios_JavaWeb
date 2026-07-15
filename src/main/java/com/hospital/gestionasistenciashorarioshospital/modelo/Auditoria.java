package com.hospital.gestionasistenciashorarioshospital.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
public class Auditoria implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tabla_afectada", nullable = false, length = 100)
    private String tablaAfectada;

    @Column(name = "registro_id")
    private Long registroId;

    @Column(nullable = false, length = 50)
    private String accion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "usuario_sistema", length = 100)
    private String usuarioSistema;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(length = 50)
    private String ip;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTablaAfectada() { return tablaAfectada; }
    public void setTablaAfectada(String tablaAfectada) { this.tablaAfectada = tablaAfectada; }
    public Long getRegistroId() { return registroId; }
    public void setRegistroId(Long registroId) { this.registroId = registroId; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getUsuarioSistema() { return usuarioSistema; }
    public void setUsuarioSistema(String usuarioSistema) { this.usuarioSistema = usuarioSistema; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
}
