package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.DocumentoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.EmpleadoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.SimpleEntityDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Documento;
import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import com.hospital.gestionasistenciashorarioshospital.modelo.TipoDocumento;
import com.hospital.gestionasistenciashorarioshospital.modelo.Usuario;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Named
@ViewScoped
public class MedicoDocumentosController implements Serializable {

    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final DocumentoDAO documentoDAO = new DocumentoDAO();
    private final SimpleEntityDAO<TipoDocumento> tipoDocumentoDAO = new SimpleEntityDAO<>(TipoDocumento.class);

    private Empleado empleado;
    private List<TipoDocumento> tiposDocumento = new ArrayList<>();
    private List<Documento> documentos = new ArrayList<>();
    private Long filtroTipoDocumentoId;
    private String busqueda;
    private int pagina = 1;
    private final int tamanioPagina = 8;
    private long totalDocumentos;
    private long totalVigentes;

    @PostConstruct
    public void init() {
        Usuario usuario = getUsuarioSesion();
        if (usuario != null) {
            empleado = empleadoDAO.buscarPorUsuarioId(usuario.getId());
        }
        tiposDocumento = tipoDocumentoDAO.listarTodos().stream()
                .filter(tipo -> Boolean.TRUE.equals(tipo.getActivo()))
                .collect(Collectors.toList());
        cargarDocumentos();
    }

    public void cargarDocumentos() {
        if (empleado == null) {
            documentos = new ArrayList<>();
            totalDocumentos = 0;
            totalVigentes = 0;
            return;
        }

        totalDocumentos = documentoDAO.contarPorEmpleado(empleado.getId(), filtroTipoDocumentoId, busqueda);
        totalVigentes = documentoDAO.contarVigentesPorEmpleado(empleado.getId());
        if (pagina > getTotalPaginas()) {
            pagina = getTotalPaginas();
        }
        documentos = documentoDAO.listarPorEmpleado(Math.max(0, (pagina - 1) * tamanioPagina),
                tamanioPagina, empleado.getId(), filtroTipoDocumentoId, busqueda);
    }

    public void filtrar() {
        pagina = 1;
        cargarDocumentos();
    }

    public void limpiarFiltros() {
        filtroTipoDocumentoId = null;
        busqueda = null;
        pagina = 1;
        cargarDocumentos();
    }

    public void descargar(Documento documento) {
        if (documento == null || documento.getRutaArchivo() == null || documento.getRutaArchivo().isBlank()) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Archivo no disponible", "El documento no tiene una ruta registrada.");
            return;
        }

        Path ruta = Paths.get(documento.getRutaArchivo());
        if (!Files.exists(ruta) || !Files.isRegularFile(ruta)) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Archivo no encontrado", "El archivo físico no existe en la ruta registrada.");
            return;
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        try {
            externalContext.responseReset();
            externalContext.setResponseContentType(resolverContentType(ruta));
            externalContext.setResponseContentLength((int) Files.size(ruta));
            externalContext.setResponseHeader("Content-Disposition",
                    "attachment; filename=\"" + documento.getNombreArchivo().replace("\"", "") + "\"");

            try (OutputStream outputStream = externalContext.getResponseOutputStream()) {
                Files.copy(ruta, outputStream);
            }
            facesContext.responseComplete();
        } catch (IOException e) {
            mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo descargar", e.getMessage());
        }
    }

    public void irPagina(int pagina) {
        if (pagina >= 1 && pagina <= getTotalPaginas()) {
            this.pagina = pagina;
            cargarDocumentos();
        }
    }

    public int getTotalPaginas() {
        return Math.max(1, (int) Math.ceil((double) totalDocumentos / tamanioPagina));
    }

    public List<Integer> getPaginas() {
        List<Integer> paginas = new ArrayList<>();
        for (int i = 1; i <= getTotalPaginas(); i++) {
            paginas.add(i);
        }
        return paginas;
    }

    public String formatearTamano(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "Tamaño no registrado";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        return String.format("%.1f MB", kb / 1024.0);
    }

    public String formatearFecha(LocalDate fecha) {
        return fecha == null ? "No aplica" : fecha.format(FECHA_FORMATTER);
    }

    public String formatearFechaHora(LocalDateTime fecha) {
        return fecha == null ? "Sin fecha" : fecha.format(FECHA_HORA_FORMATTER);
    }

    public long getTotalPorVencer() {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(30);
        return documentos.stream()
                .filter(documento -> documento.getFechaVencimiento() != null)
                .filter(documento -> !documento.getFechaVencimiento().isBefore(hoy))
                .filter(documento -> !documento.getFechaVencimiento().isAfter(limite))
                .count();
    }

    public String getUltimoDocumentoTexto() {
        return documentos.stream()
                .filter(documento -> documento.getFechaSubida() != null)
                .max(Comparator.comparing(Documento::getFechaSubida))
                .map(documento -> formatearFechaHora(documento.getFechaSubida()))
                .orElse("Sin registros");
    }

    private Usuario getUsuarioSesion() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        Object usuarioSesion = externalContext.getSessionMap().get("usuarioLogueado");
        return usuarioSesion instanceof Usuario ? (Usuario) usuarioSesion : null;
    }

    private String resolverContentType(Path ruta) throws IOException {
        String contentType = Files.probeContentType(ruta);
        return contentType == null ? "application/octet-stream" : contentType;
    }

    private void mensaje(FacesMessage.Severity severity, String resumen, String detalle) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, resumen, detalle));
    }

    public Empleado getEmpleado() { return empleado; }
    public List<TipoDocumento> getTiposDocumento() { return tiposDocumento; }
    public List<Documento> getDocumentos() { return documentos; }
    public Long getFiltroTipoDocumentoId() { return filtroTipoDocumentoId; }
    public void setFiltroTipoDocumentoId(Long filtroTipoDocumentoId) { this.filtroTipoDocumentoId = filtroTipoDocumentoId; }
    public String getBusqueda() { return busqueda; }
    public void setBusqueda(String busqueda) { this.busqueda = busqueda; }
    public int getPagina() { return pagina; }
    public int getTamanioPagina() { return tamanioPagina; }
    public long getTotalDocumentos() { return totalDocumentos; }
    public long getTotalVigentes() { return totalVigentes; }
}
