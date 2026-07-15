package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.AsistenciaDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.EmpleadoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.JustificacionDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.VariableGlobalDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import com.hospital.gestionasistenciashorarioshospital.modelo.Justificacion;
import com.hospital.gestionasistenciashorarioshospital.modelo.VariableGlobal;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@SessionScoped
public class CorreccionesController implements Serializable {

    private final JustificacionDAO justificacionDAO = new JustificacionDAO();
    private final AsistenciaDAO asistenciaDAO = new AsistenciaDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final VariableGlobalDAO variableGlobalDAO = new VariableGlobalDAO();
    private List<Justificacion> correcciones = new ArrayList<>();
    private List<Empleado> empleados = new ArrayList<>();
    private Justificacion correccionSeleccionada;
    private String filtroEstado = "todos";
    private Long filtroEmpleadoId;
    private String busqueda;
    private int pagina = 1;
    private final int tamanioPagina = 5;
    private long totalCorrecciones;

    @PostConstruct
    public void init() {
        empleados = empleadoDAO.listarFiltrado(0, 500, null, null, "activo", null);
        cargarCorrecciones();
    }

    public void cargarCorrecciones() {
        totalCorrecciones = justificacionDAO.contarFiltrado(filtroEstado, filtroEmpleadoId, busqueda);
        if (pagina > getTotalPaginas()) {
            pagina = getTotalPaginas();
        }
        correcciones = justificacionDAO.listarFiltrado(Math.max(0, (pagina - 1) * tamanioPagina), tamanioPagina,
                filtroEstado, filtroEmpleadoId, busqueda);
    }

    public void filtrar() {
        pagina = 1;
        cargarCorrecciones();
    }

    public void seleccionar(Justificacion justificacion) {
        correccionSeleccionada = justificacion;
    }

    public void aprobar() {
        cambiarEstado(true);
    }

    public void rechazar() {
        cambiarEstado(false);
    }

    private void cambiarEstado(boolean aprobada) {
        if (correccionSeleccionada == null) {
            return;
        }
        justificacionDAO.cambiarEstado(correccionSeleccionada.getId(), aprobada);
        if (aprobada && correccionSeleccionada.getAsistencia() != null) {
            VariableGlobal estadoJustificado = variableGlobalDAO.buscarPorCodigo("PERMISO");
            if (estadoJustificado != null) {
                asistenciaDAO.marcarComoJustificada(correccionSeleccionada.getAsistencia().getId(),
                        estadoJustificado.getId(),
                        "Justificación aprobada por corrección #" + correccionSeleccionada.getId());
            }
        }
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Corrección actualizada correctamente", null));
        cargarCorrecciones();
    }

    public void irPagina(int pagina) {
        if (pagina >= 1 && pagina <= getTotalPaginas()) {
            this.pagina = pagina;
            cargarCorrecciones();
        }
    }

    public int getTotalPaginas() {
        return Math.max(1, (int) Math.ceil((double) totalCorrecciones / tamanioPagina));
    }

    public List<Integer> getPaginas() {
        List<Integer> paginas = new ArrayList<>();
        for (int i = 1; i <= getTotalPaginas(); i++) {
            paginas.add(i);
        }
        return paginas;
    }

    public long getTotalPendientes() { return justificacionDAO.contarPorEstado(false); }
    public long getTotalAprobadas() { return justificacionDAO.contarPorEstado(true); }
    public long getTotalHistorico() { return justificacionDAO.contarFiltrado("todos", null, null); }

    public List<Justificacion> getCorrecciones() { return correcciones; }
    public List<Empleado> getEmpleados() { return empleados; }
    public Justificacion getCorreccionSeleccionada() { return correccionSeleccionada; }
    public void setCorreccionSeleccionada(Justificacion correccionSeleccionada) { this.correccionSeleccionada = correccionSeleccionada; }
    public String getFiltroEstado() { return filtroEstado; }
    public void setFiltroEstado(String filtroEstado) { this.filtroEstado = filtroEstado; }
    public Long getFiltroEmpleadoId() { return filtroEmpleadoId; }
    public void setFiltroEmpleadoId(Long filtroEmpleadoId) { this.filtroEmpleadoId = filtroEmpleadoId; }
    public String getBusqueda() { return busqueda; }
    public void setBusqueda(String busqueda) { this.busqueda = busqueda; }
    public int getPagina() { return pagina; }
    public long getTotalCorrecciones() { return totalCorrecciones; }
}
