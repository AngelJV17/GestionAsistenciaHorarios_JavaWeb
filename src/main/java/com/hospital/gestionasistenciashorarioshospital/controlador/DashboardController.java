package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.AuditoriaDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.DashboardDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Auditoria;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class DashboardController implements Serializable {

    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    private long totalEmpleados;
    private long empleadosActivos;
    private long asistenciasHoy;
    private long correccionesPendientes;
    private long solicitudesHoy;
    private long solicitudesPendientes;
    private List<Auditoria> actividadReciente = new ArrayList<>();

    @PostConstruct
    public void init() {
        cargar();
    }

    public void cargar() {
        totalEmpleados = dashboardDAO.contarEmpleados();
        empleadosActivos = dashboardDAO.contarEmpleadosActivos();
        asistenciasHoy = dashboardDAO.contarAsistenciasHoy();
        correccionesPendientes = dashboardDAO.contarCorreccionesPendientes();
        solicitudesHoy = dashboardDAO.contarSolicitudesHoy();
        solicitudesPendientes = dashboardDAO.contarSolicitudesPendientes();
        actividadReciente = auditoriaDAO.listarRecientes(5);
    }

    public double getPorcentajeAsistenciaHoy() {
        if (empleadosActivos == 0) {
            return 0;
        }
        return Math.round(((double) asistenciasHoy / empleadosActivos) * 1000.0) / 10.0;
    }

    public long getTotalEmpleados() { return totalEmpleados; }
    public long getEmpleadosActivos() { return empleadosActivos; }
    public long getAsistenciasHoy() { return asistenciasHoy; }
    public long getCorreccionesPendientes() { return correccionesPendientes; }
    public long getSolicitudesHoy() { return solicitudesHoy; }
    public long getSolicitudesPendientes() { return solicitudesPendientes; }
    public List<Auditoria> getActividadReciente() { return actividadReciente; }
}
