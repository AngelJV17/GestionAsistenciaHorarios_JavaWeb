package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.AuditoriaDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Auditoria;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Named
@SessionScoped
public class AuditoriaController implements Serializable {

    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();
    private List<Auditoria> registros = new ArrayList<>();
    private LocalDate filtroDesde;
    private LocalDate filtroHasta;
    private String filtroUsuario = "todos";
    private String busqueda;
    private int pagina = 1;
    private final int tamanioPagina = 5;
    private long totalRegistros;

    @PostConstruct
    public void init() {
        cargarRegistros();
    }

    public void cargarRegistros() {
        totalRegistros = auditoriaDAO.contarFiltrado(filtroDesde, filtroHasta, filtroUsuario, busqueda);
        if (pagina > getTotalPaginas()) {
            pagina = getTotalPaginas();
        }
        registros = auditoriaDAO.listarFiltrado(Math.max(0, (pagina - 1) * tamanioPagina), tamanioPagina,
                filtroDesde, filtroHasta, filtroUsuario, busqueda);
    }

    public void filtrar() {
        pagina = 1;
        cargarRegistros();
    }

    public void limpiar() {
        filtroDesde = null;
        filtroHasta = null;
        filtroUsuario = "todos";
        busqueda = null;
        filtrar();
    }

    public void irPagina(int pagina) {
        if (pagina >= 1 && pagina <= getTotalPaginas()) {
            this.pagina = pagina;
            cargarRegistros();
        }
    }

    public int getTotalPaginas() {
        return Math.max(1, (int) Math.ceil((double) totalRegistros / tamanioPagina));
    }

    public List<Integer> getPaginas() {
        List<Integer> paginas = new ArrayList<>();
        for (int i = 1; i <= getTotalPaginas(); i++) {
            paginas.add(i);
        }
        return paginas;
    }

    public long getTotalIngresos() { return auditoriaDAO.contarPorAccion("LOGIN"); }
    public long getTotalActualizaciones() { return auditoriaDAO.contarPorAccion("UPDATE"); }
    public long getTotalEliminaciones() { return auditoriaDAO.contarPorAccion("DELETE"); }

    public List<Auditoria> getRegistros() { return registros; }
    public LocalDate getFiltroDesde() { return filtroDesde; }
    public void setFiltroDesde(LocalDate filtroDesde) { this.filtroDesde = filtroDesde; }
    public LocalDate getFiltroHasta() { return filtroHasta; }
    public void setFiltroHasta(LocalDate filtroHasta) { this.filtroHasta = filtroHasta; }
    public String getFiltroUsuario() { return filtroUsuario; }
    public void setFiltroUsuario(String filtroUsuario) { this.filtroUsuario = filtroUsuario; }
    public String getBusqueda() { return busqueda; }
    public void setBusqueda(String busqueda) { this.busqueda = busqueda; }
    public int getPagina() { return pagina; }
    public long getTotalRegistros() { return totalRegistros; }
}
