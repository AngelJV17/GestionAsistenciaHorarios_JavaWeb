package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.TurnoDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Turno;
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
public class TurnosController implements Serializable {

    private final TurnoDAO turnoDAO = new TurnoDAO();
    private List<Turno> turnos = new ArrayList<>();
    private Turno turnoSeleccionado = new Turno();
    private int pagina = 1;
    private int tamanioPagina = 5;
    private long totalTurnos;
    private boolean mostrarModal;
    private String filtroEstado = "todos";
    private String busqueda;

    @PostConstruct
    public void init() {
        cargarTurnos();
        nuevoTurno(false);
    }

    public void cargarTurnos() {
        totalTurnos = turnoDAO.contarFiltrado(filtroEstado, busqueda);
        if (pagina > getTotalPaginas()) {
            pagina = getTotalPaginas();
        }
        turnos = turnoDAO.listarFiltrado(Math.max(0, (pagina - 1) * tamanioPagina), tamanioPagina, filtroEstado, busqueda);
    }

    public void nuevoTurno() {
        nuevoTurno(true);
    }

    private void nuevoTurno(boolean abrirModal) {
        turnoSeleccionado = new Turno();
        turnoSeleccionado.setCodigo(CodigoUtil.generarCodigoSecuencial("TUR", turnoDAO.obtenerUltimoNumeroCodigo("TUR"), 4));
        turnoSeleccionado.setActivo(true);
        turnoSeleccionado.setToleranciaMinutos(15);
        mostrarModal = abrirModal;
    }

    public void editarTurno(Long id) {
        turnoSeleccionado = turnoDAO.buscarPorId(id);
        mostrarModal = true;
    }

    public void guardarTurno() {
        if (turnoSeleccionado.getCodigo() == null || turnoSeleccionado.getCodigo().isBlank()) {
            turnoSeleccionado.setCodigo(CodigoUtil.generarCodigoSecuencial("TUR", turnoDAO.obtenerUltimoNumeroCodigo("TUR"), 4));
        }
        turnoDAO.guardar(turnoSeleccionado);
        cargarTurnos();
        nuevoTurno(false);
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Turno guardado", "Los datos se guardaron correctamente."));
    }

    public void cambiarEstado(Long id) {
        Turno turno = turnoDAO.buscarPorId(id);
        if (turno != null) {
            turno.setActivo(!Boolean.TRUE.equals(turno.getActivo()));
            turnoDAO.guardar(turno);
            cargarTurnos();
        }
    }

    public void filtrar() {
        pagina = 1;
        cargarTurnos();
    }

    public void paginaAnterior() {
        if (pagina > 1) {
            pagina--;
            cargarTurnos();
        }
    }

    public void paginaSiguiente() {
        if (pagina < getTotalPaginas()) {
            pagina++;
            cargarTurnos();
        }
    }

    public void irPagina(int pagina) {
        if (pagina >= 1 && pagina <= getTotalPaginas()) {
            this.pagina = pagina;
            cargarTurnos();
        }
    }

    public int getTotalPaginas() {
        return Math.max(1, (int) Math.ceil((double) totalTurnos / tamanioPagina));
    }

    public List<Integer> getPaginas() {
        List<Integer> paginas = new ArrayList<>();
        for (int i = 1; i <= getTotalPaginas(); i++) {
            paginas.add(i);
        }
        return paginas;
    }

    public long getTotalActivos() {
        return turnoDAO.contarActivos();
    }

    public List<Turno> getTurnos() {
        return turnos;
    }

    public Turno getTurnoSeleccionado() {
        return turnoSeleccionado;
    }

    public void setTurnoSeleccionado(Turno turnoSeleccionado) {
        this.turnoSeleccionado = turnoSeleccionado;
    }

    public int getPagina() {
        return pagina;
    }

    public long getTotalTurnos() {
        return totalTurnos;
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
