package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.ReporteDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.SimpleEntityDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Area;
import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Named
@SessionScoped
public class ReportesController implements Serializable {

    private final ReporteDAO reporteDAO = new ReporteDAO();
    private final SimpleEntityDAO<Area> areaDAO = new SimpleEntityDAO<>(Area.class);
    private List<Area> areas = new ArrayList<>();
    private List<Empleado> empleados = new ArrayList<>();
    private int filtroMes = LocalDate.now().getMonthValue();
    private Long filtroAreaId;
    private long totalEmpleados;
    private long totalAsistencias;
    private long totalTardanzas;
    private BigDecimal totalHorasExtras = BigDecimal.ZERO;

    @PostConstruct
    public void init() {
        areas = areaDAO.listarTodos();
        generar();
    }

    public void generar() {
        totalEmpleados = reporteDAO.contarEmpleadosActivos(filtroAreaId);
        totalAsistencias = reporteDAO.contarAsistencias(filtroMes, filtroAreaId);
        totalTardanzas = reporteDAO.contarTardanzas(filtroMes, filtroAreaId);
        totalHorasExtras = reporteDAO.sumarHorasExtras(filtroMes, filtroAreaId);
        empleados = reporteDAO.listarEmpleadosReporte(filtroAreaId);
    }

    public double getPorcentajeAsistencia() {
        if (totalEmpleados == 0) {
            return 0;
        }
        return Math.min(100, Math.round(((double) totalAsistencias / (totalEmpleados * 26.0)) * 1000.0) / 10.0);
    }

    public List<Area> getAreas() { return areas; }
    public List<Empleado> getEmpleados() { return empleados; }
    public int getFiltroMes() { return filtroMes; }
    public void setFiltroMes(int filtroMes) { this.filtroMes = filtroMes; }
    public Long getFiltroAreaId() { return filtroAreaId; }
    public void setFiltroAreaId(Long filtroAreaId) { this.filtroAreaId = filtroAreaId; }
    public long getTotalEmpleados() { return totalEmpleados; }
    public long getTotalAsistencias() { return totalAsistencias; }
    public long getTotalTardanzas() { return totalTardanzas; }
    public BigDecimal getTotalHorasExtras() { return totalHorasExtras; }
    public String getTotalHorasExtrasTexto() {
        if (totalHorasExtras == null) {
            return "0 minutos";
        }
        int minutosTotales = totalHorasExtras.multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_UP).intValue();
        int horas = minutosTotales / 60;
        int minutos = minutosTotales % 60;
        if (horas == 0) {
            return minutos == 1 ? "1 minuto" : minutos + " minutos";
        }
        String textoHoras = horas == 1 ? "1 hora" : horas + " horas";
        return minutos == 0 ? textoHoras : textoHoras + " y " + (minutos == 1 ? "1 minuto" : minutos + " minutos");
    }
}
