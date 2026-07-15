package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.EmpleadoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.AsistenciaDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.HorarioDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.SolicitudDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.TurnoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.VariableGlobalDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import com.hospital.gestionasistenciashorarioshospital.modelo.Asistencia;
import com.hospital.gestionasistenciashorarioshospital.modelo.Horario;
import com.hospital.gestionasistenciashorarioshospital.modelo.Solicitud;
import com.hospital.gestionasistenciashorarioshospital.modelo.Turno;
import com.hospital.gestionasistenciashorarioshospital.modelo.Usuario;
import com.hospital.gestionasistenciashorarioshospital.modelo.VariableGlobal;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Named
@SessionScoped
public class SolicitudesController implements Serializable {

    private final SolicitudDAO solicitudDAO = new SolicitudDAO();
    private final VariableGlobalDAO variableDAO = new VariableGlobalDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final AsistenciaDAO asistenciaDAO = new AsistenciaDAO();
    private final HorarioDAO horarioDAO = new HorarioDAO();
    private final TurnoDAO turnoDAO = new TurnoDAO();
    private static final Pattern TURNO_SOLICITADO_PATTERN = Pattern.compile("\\[\\[TURNO_ID:(\\d+)]]");

    private List<Solicitud> solicitudes = new ArrayList<>();
    private List<VariableGlobal> estados = new ArrayList<>();
    private List<Empleado> empleados = new ArrayList<>();
    private Solicitud solicitudSeleccionada;
    private Long filtroEstadoId;
    private Long filtroEmpleadoId;
    private String busqueda;
    private String observacion;
    private int pagina = 1;
    private final int tamanioPagina = 5;
    private long totalSolicitudes;

    @PostConstruct
    public void init() {
        estados = variableDAO.listarPorCategoria("ESTADO_SOLICITUD");
        cargarEmpleados();
        cargarSolicitudes();
    }

    private void cargarEmpleados() {
        empleados = empleadoDAO.listarFiltrado(0, 500, null, null, "activo", null);
    }

    public void cargarSolicitudes() {
        totalSolicitudes = solicitudDAO.contarFiltrado(filtroEstadoId, filtroEmpleadoId, busqueda);
        if (pagina > getTotalPaginas()) {
            pagina = getTotalPaginas();
        }
        solicitudes = solicitudDAO.listarFiltrado(Math.max(0, (pagina - 1) * tamanioPagina), tamanioPagina,
                filtroEstadoId, filtroEmpleadoId, busqueda);
    }

    public void filtrar() {
        pagina = 1;
        cargarSolicitudes();
    }

    public void seleccionar(Solicitud solicitud) {
        solicitudSeleccionada = solicitud;
        observacion = solicitud == null ? null : solicitud.getObservacionAprobacion();
    }

    public void aprobar() {
        cambiarEstado("APROBADA");
    }

    public void rechazar() {
        cambiarEstado("RECHAZADA");
    }

    public void abrirAdjunto(Solicitud solicitud) {
        if (solicitud == null || solicitud.getRutaArchivo() == null || solicitud.getRutaArchivo().isBlank()) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Adjunto no disponible", "La solicitud no tiene archivo registrado.");
            return;
        }
        servirArchivo(solicitud.getRutaArchivo(), solicitud.getNombreArchivo());
    }

    private void cambiarEstado(String codigoEstado) {
        if (solicitudSeleccionada == null) {
            return;
        }
        if (!estaPendiente(solicitudSeleccionada)) {
            mensaje(FacesMessage.SEVERITY_WARN, "Solicitud ya atendida",
                    "Las solicitudes aprobadas o rechazadas no pueden volver a procesarse.");
            return;
        }
        VariableGlobal estado = variableDAO.buscarPorCodigo(codigoEstado);
        if (estado == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "No existe el estado " + codigoEstado, null));
            return;
        }
        solicitudDAO.cambiarEstado(solicitudSeleccionada.getId(), estado.getId(), getUsuarioSesion(), observacion);
        if ("APROBADA".equalsIgnoreCase(codigoEstado)) {
            aplicarCambioTurnoSiCorresponde(solicitudSeleccionada.getId());
            aplicarJustificacionSiCorresponde(solicitudSeleccionada.getId());
        }
        mensaje(FacesMessage.SEVERITY_INFO, "Solicitud actualizada correctamente", null);
        cargarSolicitudes();
    }

    private void aplicarCambioTurnoSiCorresponde(Long solicitudId) {
        Solicitud solicitud = solicitudDAO.buscarPorId(solicitudId);
        if (solicitud == null || solicitud.getTipoSolicitud() == null
                || solicitud.getTipoSolicitud().getCodigo() == null
                || !"CAMBIO_TURNO".equalsIgnoreCase(solicitud.getTipoSolicitud().getCodigo())) {
            return;
        }
        Long turnoId = extraerTurnoSolicitadoId(solicitud.getMotivo());
        if (turnoId == null || solicitud.getEmpleado() == null || solicitud.getFechaInicio() == null) {
            return;
        }
        Turno turno = turnoDAO.buscarPorId(turnoId);
        if (turno == null) {
            return;
        }
        LocalDate fechaInicio = solicitud.getFechaInicio();
        LocalDate fechaFin = solicitud.getFechaFin() == null ? fechaInicio : solicitud.getFechaFin();
        Horario horario = new Horario();
        horario.setEmpleadoId(solicitud.getEmpleado().getId());
        horario.setTurno(turno);
        horario.setFechaInicio(fechaInicio);
        horario.setFechaFin(fechaFin);
        horario.setObservacion("Cambio de turno aprobado por solicitud #" + solicitud.getId());
        horarioDAO.guardar(horario);
    }

    private void aplicarJustificacionSiCorresponde(Long solicitudId) {
        Solicitud solicitud = solicitudDAO.buscarPorId(solicitudId);
        if (solicitud == null || solicitud.getTipoSolicitud() == null
                || solicitud.getTipoSolicitud().getCodigo() == null
                || !"JUSTIFICACION".equalsIgnoreCase(solicitud.getTipoSolicitud().getCodigo())
                || solicitud.getEmpleado() == null || solicitud.getFechaInicio() == null) {
            return;
        }
        Asistencia asistencia = asistenciaDAO.buscarPorEmpleadoYFecha(
                solicitud.getEmpleado().getId(), solicitud.getFechaInicio());
        VariableGlobal estadoJustificado = variableDAO.buscarPorCodigo("PERMISO");
        if (asistencia == null || estadoJustificado == null) {
            return;
        }
        asistenciaDAO.marcarComoJustificada(asistencia.getId(), estadoJustificado.getId(),
                "Justificación aprobada por solicitud #" + solicitud.getId());
    }

    private Long extraerTurnoSolicitadoId(String motivo) {
        if (motivo == null) {
            return null;
        }
        Matcher matcher = TURNO_SOLICITADO_PATTERN.matcher(motivo);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.valueOf(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void servirArchivo(String rutaArchivo, String nombreArchivo) {
        Path ruta = Paths.get(rutaArchivo);
        if (!Files.exists(ruta) || !Files.isRegularFile(ruta)) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Archivo no encontrado", "El archivo físico no existe en el storage.");
            return;
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        try {
            externalContext.responseReset();
            externalContext.setResponseContentType(resolverContentType(ruta));
            externalContext.setResponseContentLength((int) Files.size(ruta));
            externalContext.setResponseHeader("Content-Disposition",
                    "inline; filename=\"" + resolverNombreArchivo(nombreArchivo, ruta).replace("\"", "") + "\"");

            try (OutputStream outputStream = externalContext.getResponseOutputStream()) {
                Files.copy(ruta, outputStream);
            }
            facesContext.responseComplete();
        } catch (IOException e) {
            mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo abrir el adjunto", e.getMessage());
        }
    }

    private String resolverContentType(Path ruta) throws IOException {
        String contentType = Files.probeContentType(ruta);
        return contentType == null ? "application/octet-stream" : contentType;
    }

    private String resolverNombreArchivo(String nombreArchivo, Path ruta) {
        return nombreArchivo == null || nombreArchivo.isBlank()
                ? ruta.getFileName().toString()
                : nombreArchivo;
    }

    private void mensaje(FacesMessage.Severity severity, String resumen, String detalle) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, resumen, detalle));
    }

    public String obtenerMotivoVisible(Solicitud solicitud) {
        String motivo = solicitud == null ? null : solicitud.getMotivo();
        if (motivo == null) {
            return "";
        }
        return TURNO_SOLICITADO_PATTERN.matcher(motivo).replaceAll("").trim();
    }

    public boolean estaPendiente(Solicitud solicitud) {
        return solicitud != null
                && solicitud.getEstado() != null
                && solicitud.getEstado().getCodigo() != null
                && "PENDIENTE".equalsIgnoreCase(solicitud.getEstado().getCodigo());
    }

    private Usuario getUsuarioSesion() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        Object usuario = externalContext.getSessionMap().get("usuarioLogueado");
        return usuario instanceof Usuario ? (Usuario) usuario : null;
    }

    public void irPagina(int pagina) {
        if (pagina >= 1 && pagina <= getTotalPaginas()) {
            this.pagina = pagina;
            cargarSolicitudes();
        }
    }

    public int getTotalPaginas() {
        return Math.max(1, (int) Math.ceil((double) totalSolicitudes / tamanioPagina));
    }

    public List<Integer> getPaginas() {
        List<Integer> paginas = new ArrayList<>();
        for (int i = 1; i <= getTotalPaginas(); i++) {
            paginas.add(i);
        }
        return paginas;
    }

    public long getTotalPendientes() { return solicitudDAO.contarPorEstadoCodigo("PENDIENTE"); }
    public long getTotalAprobadas() { return solicitudDAO.contarPorEstadoCodigo("APROBADA"); }
    public long getTotalRechazadas() { return solicitudDAO.contarPorEstadoCodigo("RECHAZADA"); }
    public long getTotalHistorico() { return solicitudDAO.contarFiltrado(null, null, null); }

    public List<Solicitud> getSolicitudes() { return solicitudes; }
    public List<VariableGlobal> getEstados() { return estados; }
    public List<Empleado> getEmpleados() { return empleados; }
    public Solicitud getSolicitudSeleccionada() { return solicitudSeleccionada; }
    public void setSolicitudSeleccionada(Solicitud solicitudSeleccionada) { this.solicitudSeleccionada = solicitudSeleccionada; }
    public Long getFiltroEstadoId() { return filtroEstadoId; }
    public void setFiltroEstadoId(Long filtroEstadoId) { this.filtroEstadoId = filtroEstadoId; }
    public Long getFiltroEmpleadoId() { return filtroEmpleadoId; }
    public void setFiltroEmpleadoId(Long filtroEmpleadoId) { this.filtroEmpleadoId = filtroEmpleadoId; }
    public String getBusqueda() { return busqueda; }
    public void setBusqueda(String busqueda) { this.busqueda = busqueda; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public int getPagina() { return pagina; }
    public long getTotalSolicitudes() { return totalSolicitudes; }
    public int getTamanioPagina() { return tamanioPagina; }
}
