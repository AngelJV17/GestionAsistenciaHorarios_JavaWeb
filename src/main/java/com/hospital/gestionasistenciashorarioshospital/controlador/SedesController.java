package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.SedeDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Sede;
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
public class SedesController implements Serializable {

    private final SedeDAO sedeDAO = new SedeDAO();
    private List<Sede> sedes = new ArrayList<>();
    private Sede sedeSeleccionada = new Sede();
    private int pagina = 1;
    private int tamanioPagina = 5;
    private long totalSedes;
    private boolean mostrarModal;
    private String filtroEstado = "todos";
    private String busqueda;

    @PostConstruct
    public void init() {
        cargarSedes();
        prepararNuevaSede(false);
    }

    public void cargarSedes() {
        totalSedes = sedeDAO.contarFiltrado(filtroEstado, busqueda);
        int totalPaginas = getTotalPaginas();
        if (pagina > totalPaginas) {
            pagina = totalPaginas;
        }
        int inicio = Math.max(0, (pagina - 1) * tamanioPagina);
        sedes = sedeDAO.listarFiltrado(inicio, tamanioPagina, filtroEstado, busqueda);
    }

    public void nuevaSede() {
        prepararNuevaSede(true);
    }

    private void prepararNuevaSede(boolean abrirModal) {
        sedeSeleccionada = new Sede();
        sedeSeleccionada.setCodigo(CodigoUtil.generarCodigoSecuencial("SEDE", sedeDAO.obtenerUltimoNumeroCodigo("SEDE"), 4));
        sedeSeleccionada.setActivo(true);
        mostrarModal = abrirModal;
    }

    public void editarSede(Long id) {
        sedeSeleccionada = sedeDAO.buscarPorId(id);
        mostrarModal = true;
    }

    public void guardarSede() {
        if (sedeSeleccionada.getCodigo() == null || sedeSeleccionada.getCodigo().isBlank()) {
            sedeSeleccionada.setCodigo(CodigoUtil.generarCodigoSecuencial("SEDE", sedeDAO.obtenerUltimoNumeroCodigo("SEDE"), 4));
        }
        sedeDAO.guardar(sedeSeleccionada);
        cargarSedes();
        prepararNuevaSede(false);
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Sede guardada", "Los datos se guardaron correctamente."));
    }

    public void cambiarEstado(Long id) {
        Sede sede = sedeDAO.buscarPorId(id);
        if (sede != null) {
            sede.setActivo(!Boolean.TRUE.equals(sede.getActivo()));
            sedeDAO.guardar(sede);
            cargarSedes();
        }
    }

    public void filtrar() {
        pagina = 1;
        cargarSedes();
    }

    public void paginaAnterior() {
        if (pagina > 1) {
            pagina--;
            cargarSedes();
        }
    }

    public void paginaSiguiente() {
        if (pagina < getTotalPaginas()) {
            pagina++;
            cargarSedes();
        }
    }

    public void irPagina(int pagina) {
        if (pagina >= 1 && pagina <= getTotalPaginas()) {
            this.pagina = pagina;
            cargarSedes();
        }
    }

    public int getTotalPaginas() {
        return Math.max(1, (int) Math.ceil((double) totalSedes / tamanioPagina));
    }

    public List<Integer> getPaginas() {
        List<Integer> paginas = new ArrayList<>();
        for (int i = 1; i <= getTotalPaginas(); i++) {
            paginas.add(i);
        }
        return paginas;
    }

    public long getTotalActivas() {
        return sedeDAO.contarActivas();
    }

    public long getTotalInactivas() {
        return Math.max(0, totalSedes - getTotalActivas());
    }

    public List<Sede> getSedes() {
        return sedes;
    }

    public Sede getSedeSeleccionada() {
        return sedeSeleccionada;
    }

    public void setSedeSeleccionada(Sede sedeSeleccionada) {
        this.sedeSeleccionada = sedeSeleccionada;
    }

    public int getPagina() {
        return pagina;
    }

    public long getTotalSedes() {
        return totalSedes;
    }

    public boolean isMostrarModal() {
        return mostrarModal;
    }

    public boolean getMostrarModal() {
        return mostrarModal;
    }

    public String getFiltroEstado() {
        return filtroEstado;
    }

    public void setFiltroEstado(String filtroEstado) {
        this.filtroEstado = filtroEstado;
    }

    public String getBusqueda() {
        return busqueda;
    }

    public void setBusqueda(String busqueda) {
        this.busqueda = busqueda;
    }
}
