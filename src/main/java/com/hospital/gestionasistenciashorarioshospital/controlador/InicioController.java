package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.AsistenciaDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.DocumentoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.EmpleadoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.HorarioDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.SolicitudDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Asistencia;
import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import com.hospital.gestionasistenciashorarioshospital.modelo.Horario;
import com.hospital.gestionasistenciashorarioshospital.modelo.Solicitud;
import com.hospital.gestionasistenciashorarioshospital.modelo.Usuario;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Named
@RequestScoped
public class InicioController implements Serializable {

    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final AsistenciaDAO asistenciaDAO = new AsistenciaDAO();
    private final HorarioDAO horarioDAO = new HorarioDAO();
    private final SolicitudDAO solicitudDAO = new SolicitudDAO();
    private final DocumentoDAO documentoDAO = new DocumentoDAO();

    private Empleado empleado;
    private Horario horarioActivo;
    private Asistencia asistenciaHoy;
    private Solicitud ultimaSolicitud;
    private long totalAsistencias;
    private long totalTardanzas;
    private long totalFaltas;
    private long solicitudesPendientes;
    private long solicitudesAprobadas;
    private long solicitudesRechazadas;
    private long documentosVigentes;

    @PostConstruct
    public void init() {
        Usuario usuario = getUsuarioSesion();
        if (usuario == null || usuario.getId() == null) {
            return;
        }
        empleado = empleadoDAO.buscarPorUsuarioId(usuario.getId());
        if (empleado == null || empleado.getId() == null) {
            return;
        }

        LocalDate hoy = LocalDate.now();
        int mes = hoy.getMonthValue();
        int anio = hoy.getYear();
        horarioActivo = horarioDAO.buscarHorarioActivo(empleado.getId());
        asistenciaHoy = asistenciaDAO.buscarHoy(empleado.getId());
        totalAsistencias = contarAsistencias(mes, anio);
        totalTardanzas = asistenciaDAO.contarPorEstado(empleado.getId(), "TARDANZA", mes, anio);
        totalFaltas = asistenciaDAO.contarPorEstado(empleado.getId(), "FALTA", mes, anio);
        solicitudesPendientes = solicitudDAO.contarPorEmpleadoYEstadoCodigo(empleado.getId(), "PENDIENTE");
        solicitudesAprobadas = solicitudDAO.contarPorEmpleadoYEstadoCodigo(empleado.getId(), "APROBADA");
        solicitudesRechazadas = solicitudDAO.contarPorEmpleadoYEstadoCodigo(empleado.getId(), "RECHAZADA");
        documentosVigentes = documentoDAO.contarVigentesPorEmpleado(empleado.getId());
        List<Solicitud> recientes = solicitudDAO.listarPorEmpleado(0, 1, empleado.getId(), null);
        ultimaSolicitud = recientes.isEmpty() ? null : recientes.get(0);
    }

    public void redirigirLogin() throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.getExternalContext().redirect(
                facesContext.getExternalContext().getRequestContextPath()
                + "/views/login/login.xhtml"
        );
        facesContext.responseComplete();
    }

    private long contarAsistencias(int mes, int anio) {
        return asistenciaDAO.contarPorEstado(empleado.getId(), "ASISTIO", mes, anio)
                + asistenciaDAO.contarPorEstado(empleado.getId(), "ASISTIÓ", mes, anio);
    }

    private Usuario getUsuarioSesion() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        Object usuarioSesion = externalContext.getSessionMap().get("usuarioLogueado");
        return usuarioSesion instanceof Usuario ? (Usuario) usuarioSesion : null;
    }

    public String getNombreMostrado() {
        return empleado == null ? "Médico" : empleado.getNombreCompleto();
    }

    public String getHorarioHoyNombre() {
        return horarioActivo == null || horarioActivo.getTurno() == null ? "Sin turno activo" : horarioActivo.getTurno().getNombre();
    }

    public String getHorarioHoyRango() {
        if (horarioActivo == null || horarioActivo.getTurno() == null) {
            return "--:-- - --:--";
        }
        return formatearHora(horarioActivo.getTurno().getHoraInicio()) + " - " + formatearHora(horarioActivo.getTurno().getHoraFin());
    }

    public String getHorarioHoyEstado() {
        return horarioActivo == null ? "Sin programación" : "Activo";
    }

    public String getHorarioHoyEstadoClase() {
        return horarioActivo == null ? "badge bg-secondary" : "badge bg-success";
    }

    public String getProximoHorarioTexto() {
        if (horarioActivo != null && horarioActivo.getTurno() != null) {
            return horarioActivo.getTurno().getNombre() + " (" + getHorarioHoyRango() + ")";
        }
        return "Pendiente de asignación";
    }

    public String getActividadAsistenciaTexto() {
        if (asistenciaHoy == null || asistenciaHoy.getFechaHoraEntrada() == null) {
            return "Entrada pendiente";
        }
        return asistenciaHoy.getFechaHoraSalida() == null ? "Entrada registrada" : "Jornada completada";
    }

    public String getActividadAsistenciaHora() {
        if (asistenciaHoy == null || asistenciaHoy.getFechaHoraEntrada() == null) {
            return "Hoy";
        }
        return "Hoy " + asistenciaHoy.getFechaHoraEntrada().toLocalTime().format(HORA_FORMATTER);
    }

    public String getUltimaSolicitudTexto() {
        if (ultimaSolicitud == null || ultimaSolicitud.getTipoSolicitud() == null) {
            return "Sin solicitudes recientes";
        }
        String estado = ultimaSolicitud.getEstado() == null ? "Registrada" : ultimaSolicitud.getEstado().getNombre();
        return ultimaSolicitud.getTipoSolicitud().getNombre() + " - " + estado;
    }

    public String getUltimaSolicitudFecha() {
        return ultimaSolicitud == null || ultimaSolicitud.getFechaRegistro() == null
                ? ""
                : ultimaSolicitud.getFechaRegistro().toLocalDate().toString();
    }

    private String formatearHora(LocalTime hora) {
        return hora == null ? "--:--" : hora.format(HORA_FORMATTER);
    }

    public Empleado getEmpleado() { return empleado; }
    public long getTotalAsistencias() { return totalAsistencias; }
    public long getTotalTardanzas() { return totalTardanzas; }
    public long getTotalFaltas() { return totalFaltas; }
    public long getSolicitudesPendientes() { return solicitudesPendientes; }
    public long getSolicitudesAprobadas() { return solicitudesAprobadas; }
    public long getSolicitudesRechazadas() { return solicitudesRechazadas; }
    public long getDocumentosVigentes() { return documentosVigentes; }
}
