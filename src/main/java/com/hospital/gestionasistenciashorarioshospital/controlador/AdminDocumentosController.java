package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.DocumentoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.EmpleadoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.SimpleEntityDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Documento;
import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import com.hospital.gestionasistenciashorarioshospital.modelo.TipoDocumento;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Named
@ViewScoped
public class AdminDocumentosController implements Serializable {

    private final DocumentoDAO documentoDAO = new DocumentoDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final SimpleEntityDAO<TipoDocumento> tipoDocumentoDAO = new SimpleEntityDAO<>(TipoDocumento.class);

    private List<Documento> documentos = new ArrayList<>();
    private List<Empleado> empleados = new ArrayList<>();
    private List<TipoDocumento> tiposDocumento = new ArrayList<>();

    private Long empleadoId;
    private Long tipoDocumentoId;
    private String descripcion;
    private LocalDate fechaVencimiento;
    private Part archivo;
    private Long documentoEditandoId;
    private String nombreArchivoActual;

    private Long filtroEmpleadoId;
    private Long filtroTipoDocumentoId;
    private String busqueda;
    private int pagina = 1;
    private final int tamanioPagina = 8;
    private long totalDocumentos;

    @PostConstruct
    public void init() {
        empleados = empleadoDAO.listarFiltrado(0, 500, null, null, "activo", null);
        tiposDocumento = tipoDocumentoDAO.listarTodos();
        cargarDocumentos();
    }

    public void cargarDocumentos() {
        totalDocumentos = documentoDAO.contarFiltrado(filtroEmpleadoId, filtroTipoDocumentoId, busqueda);
        if (pagina > getTotalPaginas()) {
            pagina = getTotalPaginas();
        }
        documentos = documentoDAO.listarFiltrado(Math.max(0, (pagina - 1) * tamanioPagina),
                tamanioPagina, filtroEmpleadoId, filtroTipoDocumentoId, busqueda);
    }

    public void guardarDocumento() {
        boolean modoEdicion = isModoEdicion();
        Path rutaArchivoNueva = null;
        String rutaArchivoAnterior = null;

        try {
            if (!validar()) {
                return;
            }

            Empleado empleado = empleadoDAO.buscarPorId(empleadoId);
            TipoDocumento tipoDocumento = tipoDocumentoDAO.buscarPorId(tipoDocumentoId);
            Documento documento = modoEdicion ? documentoDAO.buscarPorId(documentoEditandoId) : new Documento();

            if (documento == null) {
                mensaje(FacesMessage.SEVERITY_ERROR, "Documento no disponible",
                        "El documento seleccionado ya no existe.");
                limpiarFormulario();
                cargarDocumentos();
                return;
            }

            if (archivoSeleccionado()) {
                rutaArchivoAnterior = documento.getRutaArchivo();
                String nombreOriginal = obtenerNombreArchivo(archivo);
                rutaArchivoNueva = guardarArchivoFisico(empleado, nombreOriginal);
                documento.setNombreArchivo(nombreOriginal);
                documento.setRutaArchivo(rutaArchivoNueva.toString());
                documento.setTamanoArchivo(archivo.getSize());
                documento.setExtension(obtenerExtension(nombreOriginal));
                documento.setFechaSubida(LocalDateTime.now());
            }

            documento.setEmpleado(empleado);
            documento.setTipoDocumento(tipoDocumento);
            documento.setDescripcion(limpiar(descripcion));
            documento.setFechaVencimiento(fechaVencimiento);

            documentoDAO.guardar(documento);
            borrarArchivoFisico(rutaArchivoAnterior);
            limpiarFormulario();
            cargarDocumentos();
            mensaje(FacesMessage.SEVERITY_INFO,
                    modoEdicion ? "Documento actualizado" : "Documento cargado",
                    modoEdicion
                            ? "Los cambios se guardaron correctamente."
                            : "El archivo quedo asociado solo al empleado seleccionado.");
        } catch (Exception e) {
            borrarArchivoFisico(rutaArchivoNueva == null ? null : rutaArchivoNueva.toString());
            mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo guardar el documento", extraerMensaje(e));
        }
    }

    public void editarDocumento(Long documentoId) {
        Documento documento = documentoDAO.buscarPorId(documentoId);
        if (documento == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Documento no disponible",
                    "El documento seleccionado ya no existe.");
            cargarDocumentos();
            return;
        }

        documentoEditandoId = documento.getId();
        empleadoId = documento.getEmpleado().getId();
        tipoDocumentoId = documento.getTipoDocumento().getId();
        descripcion = documento.getDescripcion();
        fechaVencimiento = documento.getFechaVencimiento();
        nombreArchivoActual = documento.getNombreArchivo();
        archivo = null;
    }

    public void cancelarEdicion() {
        limpiarFormulario();
    }

    public void eliminarDocumento(Long documentoId) {
        Documento documento = documentoDAO.buscarPorId(documentoId);
        if (documento == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Documento no disponible",
                    "El documento seleccionado ya no existe.");
            cargarDocumentos();
            return;
        }

        String rutaArchivo = documento.getRutaArchivo();
        try {
            documentoDAO.eliminar(documentoId);
            borrarArchivoFisico(rutaArchivo);
            if (documentoId.equals(documentoEditandoId)) {
                limpiarFormulario();
            }
            cargarDocumentos();
            mensaje(FacesMessage.SEVERITY_INFO, "Documento eliminado",
                    "Se elimino el registro y el archivo fisico asociado.");
        } catch (Exception e) {
            mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo eliminar el documento", extraerMensaje(e));
        }
    }

    public void filtrar() {
        pagina = 1;
        cargarDocumentos();
    }

    public void limpiarFiltros() {
        filtroEmpleadoId = null;
        filtroTipoDocumentoId = null;
        busqueda = null;
        pagina = 1;
        cargarDocumentos();
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

    public long getTotalVigentes() {
        return documentoDAO.contarVigentes();
    }

    private boolean validar() {
        if (empleadoId == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Empleado requerido", "Seleccione el empleado propietario del documento.");
            return false;
        }
        if (tipoDocumentoId == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Tipo requerido", "Seleccione el tipo de documento.");
            return false;
        }
        if (!isModoEdicion() && !archivoSeleccionado()) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Archivo requerido", "Seleccione un archivo para cargar.");
            return false;
        }
        if (archivoSeleccionado() && archivo.getSize() > 10 * 1024 * 1024) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Archivo muy grande", "El archivo no debe superar 10 MB.");
            return false;
        }
        return true;
    }

    private Path guardarArchivoFisico(Empleado empleado, String nombreOriginal) throws IOException {
        Path carpetaEmpleado = resolverCarpetaBase().resolve(
                empleado.getCodigoEmpleado() == null ? String.valueOf(empleado.getId()) : empleado.getCodigoEmpleado());
        Files.createDirectories(carpetaEmpleado);

        String nombreSeguro = UUID.randomUUID() + "-" + normalizarNombre(nombreOriginal);
        Path destino = carpetaEmpleado.resolve(nombreSeguro);
        try (InputStream inputStream = archivo.getInputStream()) {
            Files.copy(inputStream, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        return destino;
    }

    private void borrarArchivoFisico(String rutaArchivo) {
        if (rutaArchivo == null || rutaArchivo.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(rutaArchivo));
        } catch (IOException e) {
            mensaje(FacesMessage.SEVERITY_WARN, "Archivo pendiente de limpieza",
                    "No se pudo eliminar del storage: " + rutaArchivo);
        }
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

    private boolean archivoSeleccionado() {
        return archivo != null && archivo.getSize() > 0;
    }

    private String obtenerNombreArchivo(Part part) {
        String submitted = part.getSubmittedFileName();
        return submitted == null || submitted.isBlank() ? "documento" : Paths.get(submitted).getFileName().toString();
    }

    private String obtenerExtension(String nombreArchivo) {
        int punto = nombreArchivo == null ? -1 : nombreArchivo.lastIndexOf('.');
        return punto >= 0 && punto < nombreArchivo.length() - 1
                ? nombreArchivo.substring(punto + 1).toLowerCase(Locale.ROOT)
                : null;
    }

    private String normalizarNombre(String nombre) {
        String limpio = Normalizer.normalize(nombre == null ? "documento" : nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9._-]", "_");
        return limpio.isBlank() ? "documento" : limpio;
    }

    private String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private void limpiarFormulario() {
        empleadoId = null;
        tipoDocumentoId = null;
        descripcion = null;
        fechaVencimiento = null;
        archivo = null;
        documentoEditandoId = null;
        nombreArchivoActual = null;
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

    public List<Documento> getDocumentos() { return documentos; }
    public List<Empleado> getEmpleados() { return empleados; }
    public List<TipoDocumento> getTiposDocumento() { return tiposDocumento; }
    public Long getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(Long empleadoId) { this.empleadoId = empleadoId; }
    public Long getTipoDocumentoId() { return tipoDocumentoId; }
    public void setTipoDocumentoId(Long tipoDocumentoId) { this.tipoDocumentoId = tipoDocumentoId; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public Part getArchivo() { return archivo; }
    public void setArchivo(Part archivo) { this.archivo = archivo; }
    public Long getFiltroEmpleadoId() { return filtroEmpleadoId; }
    public void setFiltroEmpleadoId(Long filtroEmpleadoId) { this.filtroEmpleadoId = filtroEmpleadoId; }
    public Long getFiltroTipoDocumentoId() { return filtroTipoDocumentoId; }
    public void setFiltroTipoDocumentoId(Long filtroTipoDocumentoId) { this.filtroTipoDocumentoId = filtroTipoDocumentoId; }
    public String getBusqueda() { return busqueda; }
    public void setBusqueda(String busqueda) { this.busqueda = busqueda; }
    public int getPagina() { return pagina; }
    public long getTotalDocumentos() { return totalDocumentos; }
    public boolean isModoEdicion() { return documentoEditandoId != null; }
    public Long getDocumentoEditandoId() { return documentoEditandoId; }
    public String getNombreArchivoActual() { return nombreArchivoActual; }
}
