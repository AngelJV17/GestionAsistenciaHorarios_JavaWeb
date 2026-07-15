package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.EmpleadoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.SimpleEntityDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.SolicitudDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.VariableGlobalDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import com.hospital.gestionasistenciashorarioshospital.modelo.Solicitud;
import com.hospital.gestionasistenciashorarioshospital.modelo.TipoSolicitud;
import com.hospital.gestionasistenciashorarioshospital.modelo.Usuario;
import com.hospital.gestionasistenciashorarioshospital.modelo.VariableGlobal;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Named
@ViewScoped
public class MedicoSolicitudesController implements Serializable {

    private final SolicitudDAO solicitudDAO = new SolicitudDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final VariableGlobalDAO variableGlobalDAO = new VariableGlobalDAO();
    private final SimpleEntityDAO<TipoSolicitud> tipoSolicitudDAO = new SimpleEntityDAO<>(TipoSolicitud.class);
    private static final Pattern TURNO_SOLICITADO_PATTERN = Pattern.compile("\\[\\[TURNO_ID:(\\d+)]]");

    private Empleado empleado;
    private List<TipoSolicitud> tiposSolicitud = new ArrayList<>();
    private List<Solicitud> solicitudes = new ArrayList<>();
    private Long tipoSolicitudId;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String motivo;
    private Part archivo;
    private String busqueda;
    private int pagina = 1;
    private final int tamanioPagina = 6;
    private long totalSolicitudes;

    @PostConstruct
    public void init() {
        Usuario usuario = getUsuarioSesion();
        if (usuario != null && usuario.getId() != null) {
            empleado = empleadoDAO.buscarPorUsuarioId(usuario.getId());
        }
        tiposSolicitud = tipoSolicitudDAO.listarTodos().stream()
                .filter(tipo -> Boolean.TRUE.equals(tipo.getActivo()))
                .collect(Collectors.toList());
        precargarJustificacionDesdeCalendario();
        cargarSolicitudes();
    }

    public String registrarSolicitud() {
        Path rutaNueva = null;
        if (empleado == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Perfil no disponible",
                    "No se encontró un empleado vinculado al usuario actual.");
            return null;
        }
        if (!validarFormulario()) {
            return null;
        }

        TipoSolicitud tipo = buscarTipoSeleccionado();
        VariableGlobal estadoPendiente = variableGlobalDAO.buscarPorCodigo("PENDIENTE");
        if (tipo == null || estadoPendiente == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Catálogo incompleto",
                    "Revise que existan tipos de solicitud activos y el estado PENDIENTE.");
            return null;
        }

        try {
            Solicitud solicitud = new Solicitud();
            solicitud.setEmpleado(empleado);
            solicitud.setTipoSolicitud(tipo);
            solicitud.setEstado(estadoPendiente);
            solicitud.setFechaInicio(fechaInicio);
            solicitud.setFechaFin(fechaFin);
            solicitud.setMotivo(limpiar(motivo));

            if (archivoSeleccionado()) {
                String nombreOriginal = obtenerNombreArchivo(archivo);
                rutaNueva = guardarArchivoFisico(nombreOriginal);
                solicitud.setNombreArchivo(nombreOriginal);
                solicitud.setRutaArchivo(rutaNueva.toString());
                solicitud.setTamanoArchivo(archivo.getSize());
                solicitud.setExtension(obtenerExtension(nombreOriginal));
            }

            solicitudDAO.guardar(solicitud);
            limpiarFormulario();
            pagina = 1;
            cargarSolicitudes();
            mensaje(FacesMessage.SEVERITY_INFO, "Solicitud registrada",
                    "Su solicitud fue enviada para revisión administrativa.");
            FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
            return "/views/medico/solicitudes.xhtml?faces-redirect=true";
        } catch (Exception e) {
            borrarArchivoFisico(rutaNueva);
            mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo registrar la solicitud", extraerMensaje(e));
            return null;
        }
    }

    public void cargarSolicitudes() {
        if (empleado == null || empleado.getId() == null) {
            solicitudes = new ArrayList<>();
            totalSolicitudes = 0;
            return;
        }

        totalSolicitudes = solicitudDAO.contarPorEmpleado(empleado.getId(), busqueda);
        if (pagina > getTotalPaginas()) {
            pagina = getTotalPaginas();
        }
        solicitudes = solicitudDAO.listarPorEmpleado(Math.max(0, (pagina - 1) * tamanioPagina),
                tamanioPagina, empleado.getId(), busqueda);
    }

    public void filtrar() {
        pagina = 1;
        cargarSolicitudes();
    }

    public void limpiarFiltros() {
        busqueda = null;
        pagina = 1;
        cargarSolicitudes();
    }

    public void abrirAdjunto(Solicitud solicitud) {
        if (solicitud == null || solicitud.getRutaArchivo() == null || solicitud.getRutaArchivo().isBlank()) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Adjunto no disponible", "La solicitud no tiene archivo registrado.");
            return;
        }
        if (empleado == null || solicitud.getEmpleado() == null
                || !empleado.getId().equals(solicitud.getEmpleado().getId())) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Acceso denegado", "No puede abrir adjuntos de otro empleado.");
            return;
        }
        servirArchivo(solicitud.getRutaArchivo(), solicitud.getNombreArchivo());
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

    public String obtenerClaseEstado(Solicitud solicitud) {
        String codigo = solicitud == null || solicitud.getEstado() == null ? "" : solicitud.getEstado().getCodigo();
        switch (codigo == null ? "" : codigo.toUpperCase(Locale.ROOT)) {
            case "APROBADA":
                return "badge bg-success";
            case "RECHAZADA":
                return "badge bg-danger";
            case "PENDIENTE":
                return "badge bg-warning text-dark";
            default:
                return "badge bg-primary";
        }
    }

    public String formatearTamano(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.US, "%.1f KB", kb);
        }
        return String.format(Locale.US, "%.1f MB", kb / 1024.0);
    }

    public String obtenerMotivoVisible(Solicitud solicitud) {
        String motivo = solicitud == null ? null : solicitud.getMotivo();
        if (motivo == null) {
            return "";
        }
        return TURNO_SOLICITADO_PATTERN.matcher(motivo).replaceAll("").trim();
    }

    public long getTotalPendientes() {
        return contarEstado("PENDIENTE");
    }

    public long getTotalAprobadas() {
        return contarEstado("APROBADA");
    }

    public long getTotalRechazadas() {
        return contarEstado("RECHAZADA");
    }

    public long getTotalHistorico() {
        return empleado == null ? 0 : solicitudDAO.contarPorEmpleado(empleado.getId());
    }

    private long contarEstado(String codigo) {
        return empleado == null ? 0 : solicitudDAO.contarPorEmpleadoYEstadoCodigo(empleado.getId(), codigo);
    }

    private boolean validarFormulario() {
        if (tipoSolicitudId == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Tipo requerido", "Seleccione el tipo de solicitud.");
            return false;
        }
        if (fechaInicio == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Fecha requerida", "Ingrese la fecha desde la que aplica la solicitud.");
            return false;
        }
        if (fechaFin != null && fechaFin.isBefore(fechaInicio)) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Rango inválido", "La fecha fin no puede ser anterior a la fecha inicio.");
            return false;
        }
        if (motivo == null || motivo.trim().length() < 10) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Motivo requerido", "Explique el motivo con al menos 10 caracteres.");
            return false;
        }
        if (archivoSeleccionado() && archivo.getSize() > 10 * 1024 * 1024) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Archivo muy grande", "El archivo no debe superar 10 MB.");
            return false;
        }
        return true;
    }

    private Path guardarArchivoFisico(String nombreOriginal) throws IOException {
        Path carpetaEmpleado = resolverCarpetaBase()
                .resolve("solicitudes")
                .resolve(empleado.getCodigoEmpleado() == null ? String.valueOf(empleado.getId()) : empleado.getCodigoEmpleado());
        Files.createDirectories(carpetaEmpleado);

        String nombreSeguro = UUID.randomUUID() + "-" + normalizarNombre(nombreOriginal);
        Path destino = carpetaEmpleado.resolve(nombreSeguro);
        try (InputStream inputStream = archivo.getInputStream()) {
            Files.copy(inputStream, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        return destino;
    }

    private Path resolverCarpetaBase() {
        String ruta = FacesContext.getCurrentInstance()
                .getExternalContext()
                .getInitParameter("hospital.documentos.path");
        if (ruta == null || ruta.isBlank()) {
            ruta = System.getProperty("hospital.documentos.path");
        }
        if (ruta == null || ruta.isBlank()) {
            ruta = System.getenv("HOSPITAL_DOCUMENTOS_DIR");
        }
        if (ruta == null || ruta.isBlank()) {
            return Paths.get(System.getProperty("user.home"), "Documents", "hospital-documentos");
        }
        return Paths.get(ruta);
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

    private void borrarArchivoFisico(Path ruta) {
        if (ruta == null) {
            return;
        }
        try {
            Files.deleteIfExists(ruta);
        } catch (IOException ignored) {
            // Si falla la limpieza, no debe ocultar el mensaje principal de error.
        }
    }

    private boolean archivoSeleccionado() {
        return archivo != null && archivo.getSize() > 0;
    }

    private String obtenerNombreArchivo(Part part) {
        String submitted = part.getSubmittedFileName();
        return submitted == null || submitted.isBlank() ? "adjunto" : Paths.get(submitted).getFileName().toString();
    }

    private String obtenerExtension(String nombreArchivo) {
        int punto = nombreArchivo == null ? -1 : nombreArchivo.lastIndexOf('.');
        return punto >= 0 && punto < nombreArchivo.length() - 1
                ? nombreArchivo.substring(punto + 1).toLowerCase(Locale.ROOT)
                : null;
    }

    private String normalizarNombre(String nombre) {
        String limpio = Normalizer.normalize(nombre == null ? "adjunto" : nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9._-]", "_");
        return limpio.isBlank() ? "adjunto" : limpio;
    }

    private TipoSolicitud buscarTipoSeleccionado() {
        return tiposSolicitud.stream()
                .filter(tipo -> tipo.getId() != null && tipo.getId().equals(tipoSolicitudId))
                .findFirst()
                .orElse(null);
    }

    private void precargarJustificacionDesdeCalendario() {
        String fecha = FacesContext.getCurrentInstance().getExternalContext()
                .getRequestParameterMap().get("fecha");
        if (fecha == null || fecha.isBlank()) {
            return;
        }
        try {
            fechaInicio = LocalDate.parse(fecha);
            fechaFin = fechaInicio;
            motivo = "Solicito justificar la incidencia registrada el " + fecha + ".";
            tiposSolicitud.stream()
                    .filter(tipo -> tipo.getCodigo() != null && "JUSTIFICACION".equalsIgnoreCase(tipo.getCodigo()))
                    .findFirst()
                    .ifPresent(tipo -> tipoSolicitudId = tipo.getId());
        } catch (Exception ignored) {
            // Si el parámetro no es una fecha válida, el formulario queda limpio.
        }
    }

    private Usuario getUsuarioSesion() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        Object usuarioSesion = externalContext.getSessionMap().get("usuarioLogueado");
        return usuarioSesion instanceof Usuario ? (Usuario) usuarioSesion : null;
    }

    private String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private void limpiarFormulario() {
        tipoSolicitudId = null;
        fechaInicio = null;
        fechaFin = null;
        motivo = null;
        archivo = null;
    }

    private String extraerMensaje(Exception e) {
        String mensaje = e.getMessage();
        Throwable causa = e.getCause();
        while (causa != null) {
            if (causa.getMessage() != null) {
                mensaje = causa.getMessage();
            }
            causa = causa.getCause();
        }
        return mensaje == null ? "Revise los datos e intente nuevamente." : mensaje;
    }

    private void mensaje(FacesMessage.Severity severity, String resumen, String detalle) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, resumen, detalle));
    }

    public Empleado getEmpleado() { return empleado; }
    public List<TipoSolicitud> getTiposSolicitud() { return tiposSolicitud; }
    public List<Solicitud> getSolicitudes() { return solicitudes; }
    public Long getTipoSolicitudId() { return tipoSolicitudId; }
    public void setTipoSolicitudId(Long tipoSolicitudId) { this.tipoSolicitudId = tipoSolicitudId; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Part getArchivo() { return archivo; }
    public void setArchivo(Part archivo) { this.archivo = archivo; }
    public String getBusqueda() { return busqueda; }
    public void setBusqueda(String busqueda) { this.busqueda = busqueda; }
    public int getPagina() { return pagina; }
    public long getTotalSolicitudes() { return totalSolicitudes; }
}
