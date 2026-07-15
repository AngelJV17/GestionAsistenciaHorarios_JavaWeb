package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.VariableGlobalDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.VariableGlobal;
import com.hospital.gestionasistenciashorarioshospital.util.CodigoUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class VariablesController implements Serializable {

    private final VariableGlobalDAO variableDAO = new VariableGlobalDAO();
    private List<VariableGlobal> variables = new ArrayList<>();
    private VariableGlobal variableSeleccionada = new VariableGlobal();
    private int pagina = 1;
    private int tamanioPagina = 8;
    private long totalVariables;
    private boolean mostrarModal;
    private String filtroCategoria = "todas";
    private String busqueda;

    @PostConstruct
    public void init() {
        cargarVariables();
        nuevaVariable(false);
    }

    public void cargarVariables() {
        totalVariables = variableDAO.contarFiltrado(filtroCategoria, busqueda);
        if (pagina > getTotalPaginas()) {
            pagina = getTotalPaginas();
        }
        variables = variableDAO.listarFiltrado(Math.max(0, (pagina - 1) * tamanioPagina), tamanioPagina, filtroCategoria, busqueda);
    }

    public void nuevaVariable() {
        nuevaVariable(true);
    }

    private void nuevaVariable(boolean abrirModal) {
        variableSeleccionada = new VariableGlobal();
        variableSeleccionada.setActivo(true);
        variableSeleccionada.setOrdenVisualizacion(0);
        mostrarModal = abrirModal;
    }

    public void editarVariable(Long id) {
        variableSeleccionada = variableDAO.buscarPorId(id);
        mostrarModal = true;
    }

    public void guardarVariable() {
        if ((variableSeleccionada.getCodigo() == null || variableSeleccionada.getCodigo().isBlank())
                && variableSeleccionada.getNombre() != null) {
            variableSeleccionada.setCodigo(CodigoUtil.normalizarCodigo(variableSeleccionada.getNombre()));
        }
        variableDAO.guardar(variableSeleccionada);
        cargarVariables();
        nuevaVariable(false);
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Variable guardada", "Los datos se guardaron correctamente."));
    }

    public void cambiarEstado(Long id) {
        VariableGlobal variable = variableDAO.buscarPorId(id);
        if (variable != null) {
            variable.setActivo(!Boolean.TRUE.equals(variable.getActivo()));
            variableDAO.guardar(variable);
            cargarVariables();
        }
    }

    public void filtrar() {
        pagina = 1;
        cargarVariables();
    }

    public void paginaAnterior() {
        if (pagina > 1) {
            pagina--;
            cargarVariables();
        }
    }

    public void paginaSiguiente() {
        if (pagina < getTotalPaginas()) {
            pagina++;
            cargarVariables();
        }
    }

    public void irPagina(int pagina) {
        if (pagina >= 1 && pagina <= getTotalPaginas()) {
            this.pagina = pagina;
            cargarVariables();
        }
    }

    public int getTotalPaginas() {
        return Math.max(1, (int) Math.ceil((double) totalVariables / tamanioPagina));
    }

    public List<Integer> getPaginas() {
        List<Integer> paginas = new ArrayList<>();
        for (int i = 1; i <= getTotalPaginas(); i++) {
            paginas.add(i);
        }
        return paginas;
    }

    public long getTotalActivas() {
        return variableDAO.contarActivas();
    }

    public long getTotalCategorias() {
        return variableDAO.contarCategorias();
    }

    public List<VariableGlobal> getVariables() {
        return variables;
    }

    public VariableGlobal getVariableSeleccionada() {
        return variableSeleccionada;
    }

    public void setVariableSeleccionada(VariableGlobal variableSeleccionada) {
        this.variableSeleccionada = variableSeleccionada;
    }

    public int getPagina() {
        return pagina;
    }

    public long getTotalVariables() {
        return totalVariables;
    }

    public boolean getMostrarModal() {
        return mostrarModal;
    }

    public String getFiltroCategoria() {
        return filtroCategoria;
    }

    public void setFiltroCategoria(String filtroCategoria) {
        this.filtroCategoria = filtroCategoria;
    }

    public String getBusqueda() {
        return busqueda;
    }

    public void setBusqueda(String busqueda) {
        this.busqueda = busqueda;
    }
}
