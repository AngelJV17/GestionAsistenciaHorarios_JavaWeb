package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.FeriadoDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Feriado;
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
public class FeriadosController implements Serializable {

    private final FeriadoDAO feriadoDAO = new FeriadoDAO();
    private List<Feriado> feriados = new ArrayList<>();
    private Feriado feriadoSeleccionado = new Feriado();
    private int pagina = 1;
    private int tamanioPagina = 5;
    private long totalFeriados;
    private boolean mostrarModal;
    private String filtroTipo = "todos";
    private String busqueda;

    @PostConstruct
    public void init() {
        cargarFeriados();
        nuevoFeriado(false);
    }

    public void cargarFeriados() {
        totalFeriados = feriadoDAO.contarFiltrado(filtroTipo, busqueda);
        if (pagina > getTotalPaginas()) {
            pagina = getTotalPaginas();
        }
        feriados = feriadoDAO.listarFiltrado(Math.max(0, (pagina - 1) * tamanioPagina), tamanioPagina, filtroTipo, busqueda);
    }

    public void nuevoFeriado() {
        nuevoFeriado(true);
    }

    private void nuevoFeriado(boolean abrirModal) {
        feriadoSeleccionado = new Feriado();
        feriadoSeleccionado.setEsRecuperable(false);
        mostrarModal = abrirModal;
    }

    public void editarFeriado(Long id) {
        feriadoSeleccionado = feriadoDAO.buscarPorId(id);
        mostrarModal = true;
    }

    public void guardarFeriado() {
        feriadoDAO.guardar(feriadoSeleccionado);
        cargarFeriados();
        nuevoFeriado(false);
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Feriado guardado", "Los datos se guardaron correctamente."));
    }

    public void filtrar() {
        pagina = 1;
        cargarFeriados();
    }

    public void paginaAnterior() {
        if (pagina > 1) {
            pagina--;
            cargarFeriados();
        }
    }

    public void paginaSiguiente() {
        if (pagina < getTotalPaginas()) {
            pagina++;
            cargarFeriados();
        }
    }

    public void irPagina(int pagina) {
        if (pagina >= 1 && pagina <= getTotalPaginas()) {
            this.pagina = pagina;
            cargarFeriados();
        }
    }

    public int getTotalPaginas() {
        return Math.max(1, (int) Math.ceil((double) totalFeriados / tamanioPagina));
    }

    public List<Integer> getPaginas() {
        List<Integer> paginas = new ArrayList<>();
        for (int i = 1; i <= getTotalPaginas(); i++) {
            paginas.add(i);
        }
        return paginas;
    }

    public long getTotalRecuperables() {
        return feriadoDAO.contarRecuperables();
    }

    public List<Feriado> getFeriados() {
        return feriados;
    }

    public Feriado getFeriadoSeleccionado() {
        return feriadoSeleccionado;
    }

    public void setFeriadoSeleccionado(Feriado feriadoSeleccionado) {
        this.feriadoSeleccionado = feriadoSeleccionado;
    }

    public int getPagina() {
        return pagina;
    }

    public long getTotalFeriados() {
        return totalFeriados;
    }

    public boolean getMostrarModal() {
        return mostrarModal;
    }

    public String getFiltroTipo() {
        return filtroTipo;
    }

    public void setFiltroTipo(String filtroTipo) {
        this.filtroTipo = filtroTipo;
    }

    public String getBusqueda() {
        return busqueda;
    }

    public void setBusqueda(String busqueda) {
        this.busqueda = busqueda;
    }
}
