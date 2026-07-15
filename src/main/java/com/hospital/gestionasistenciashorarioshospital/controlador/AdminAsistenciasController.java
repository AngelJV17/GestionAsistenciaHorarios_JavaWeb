package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.AsistenciaDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.EmpleadoDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Asistencia;
import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Named
@ViewScoped
public class AdminAsistenciasController implements Serializable {

    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AsistenciaDAO asistenciaDAO = new AsistenciaDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();

    private List<Empleado> empleados = new ArrayList<>();
    private List<Asistencia> asistencias = new ArrayList<>();
    private Long filtroEmpleadoId;
    private int mesFiltro;
    private int anioFiltro;

    @PostConstruct
    public void init() {
        LocalDate hoy = LocalDate.now();
        mesFiltro = hoy.getMonthValue();
        anioFiltro = hoy.getYear();
        empleados = empleadoDAO.listarFiltrado(0, 500, null, null, "activo", null);
        buscar();
    }

    public void buscar() {
        asistencias = asistenciaDAO.listarFiltradoPorMes(filtroEmpleadoId, mesFiltro, anioFiltro);
    }

    public String obtenerEstadoCodigo(Asistencia asistencia) {
        if (asistencia == null || asistencia.getEstadoAsistencia() == null
                || asistencia.getEstadoAsistencia().getCodigo() == null) {
            return "SIN_REGISTRO";
        }
        return asistencia.getEstadoAsistencia().getCodigo().toUpperCase(Locale.ROOT);
    }

    public String obtenerEstadoTexto(Asistencia asistencia) {
        if (esJustificacionAprobada(asistencia)) {
            return "Justificado";
        }
        return asistencia == null || asistencia.getEstadoAsistencia() == null
                ? "Sin registro"
                : asistencia.getEstadoAsistencia().getNombre();
    }

    public String obtenerEstadoClase(Asistencia asistencia) {
        switch (obtenerEstadoCodigo(asistencia)) {
            case "ASISTIO":
            case "ASISTIÓ":
                return "badge bg-success";
            case "TARDANZA":
                return "badge bg-warning text-dark";
            case "FALTA":
                return "badge bg-danger";
            case "PERMISO":
            case "LICENCIA":
                return "badge bg-info text-dark";
            default:
                return "badge bg-secondary";
        }
    }

    public String formatearHora(LocalDateTime fechaHora) {
        return fechaHora == null ? "--:--" : fechaHora.toLocalTime().format(HORA_FORMATTER);
    }

    public String formatearFecha(LocalDate fecha) {
        return fecha == null ? "--/--/----" : fecha.format(FECHA_FORMATTER);
    }

    public String formatearDuracionMinutos(Integer minutos) {
        int total = minutos == null ? 0 : Math.max(0, minutos);
        int horas = total / 60;
        int resto = total % 60;
        if (horas == 0) {
            return resto == 1 ? "1 minuto" : resto + " minutos";
        }
        String textoHoras = horas == 1 ? "1 hora" : horas + " horas";
        if (resto == 0) {
            return textoHoras;
        }
        return textoHoras + " y " + (resto == 1 ? "1 minuto" : resto + " minutos");
    }

    public String formatearHorasTrabajadas(BigDecimal horas) {
        if (horas == null) {
            return "Sin registrar";
        }
        int minutosTotales = horas.multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_UP).intValue();
        return formatearDuracionMinutos(minutosTotales);
    }

    private boolean esJustificacionAprobada(Asistencia asistencia) {
        return asistencia != null
                && asistencia.getObservacion() != null
                && asistencia.getObservacion().toLowerCase(Locale.ROOT).contains("justificación aprobada");
    }

    public String getMesAnioTexto() {
        String mes = Month.of(mesFiltro).getDisplayName(TextStyle.FULL, new Locale("es", "PE"));
        return mes.substring(0, 1).toUpperCase(Locale.ROOT) + mes.substring(1) + " de " + anioFiltro;
    }

    public long getTotalAsistencias() {
        return asistencias.size();
    }

    public long getTotalTardanzas() {
        return asistencias.stream().filter(a -> "TARDANZA".equals(obtenerEstadoCodigo(a))).count();
    }

    public long getTotalFaltas() {
        return asistencias.stream().filter(a -> "FALTA".equals(obtenerEstadoCodigo(a))).count();
    }

    public List<Empleado> getEmpleados() { return empleados; }
    public List<Asistencia> getAsistencias() { return asistencias; }
    public Long getFiltroEmpleadoId() { return filtroEmpleadoId; }
    public void setFiltroEmpleadoId(Long filtroEmpleadoId) { this.filtroEmpleadoId = filtroEmpleadoId; }
    public int getMesFiltro() { return mesFiltro; }
    public void setMesFiltro(int mesFiltro) { this.mesFiltro = mesFiltro; }
    public int getAnioFiltro() { return anioFiltro; }
    public void setAnioFiltro(int anioFiltro) { this.anioFiltro = anioFiltro; }
}
