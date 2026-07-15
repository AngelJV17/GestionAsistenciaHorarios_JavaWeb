package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.EmpleadoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.HorarioDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.TurnoDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import com.hospital.gestionasistenciashorarioshospital.modelo.Horario;
import com.hospital.gestionasistenciashorarioshospital.modelo.Turno;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Named
@ViewScoped
public class AdminHorariosController implements Serializable {

    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Map<String, String> DIAS_SEMANA = new LinkedHashMap<>();

    static {
        DIAS_SEMANA.put("LUN", "Lunes");
        DIAS_SEMANA.put("MAR", "Martes");
        DIAS_SEMANA.put("MIE", "Miércoles");
        DIAS_SEMANA.put("JUE", "Jueves");
        DIAS_SEMANA.put("VIE", "Viernes");
        DIAS_SEMANA.put("SAB", "Sábado");
        DIAS_SEMANA.put("DOM", "Domingo");
    }

    private final HorarioDAO horarioDAO = new HorarioDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final TurnoDAO turnoDAO = new TurnoDAO();

    private List<Horario> horarios = new ArrayList<>();
    private List<Empleado> empleados = new ArrayList<>();
    private List<Turno> turnos = new ArrayList<>();
    private Long empleadoId;
    private Long turnoId;
    private String fechaInicio;
    private String fechaFin;
    private String observacion;
    private String[] diasDescansoSeleccionados = new String[0];
    private Long horarioEditandoId;
    private Long filtroEmpleadoId;
    private Long filtroTurnoId;
    private String busqueda;
    private int pagina = 1;
    private final int tamanioPagina = 8;
    private long totalHorarios;

    @PostConstruct
    public void init() {
        empleados = empleadoDAO.listarFiltrado(0, 500, null, null, "activo", null);
        turnos = turnoDAO.listarFiltrado(0, 500, "activo", null);
        cargarHorarios();
    }

    public void guardarHorario() {
        if (!validar()) {
            return;
        }
        Turno turno = buscarTurnoSeleccionado();
        if (turno == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Turno inválido", "Seleccione un turno activo.");
            return;
        }
        LocalDate inicio = parseFecha(fechaInicio);
        LocalDate fin = parseFecha(fechaFin);
        if (horarioDAO.existeTurnoSolapado(empleadoId, turnoId, inicio, fin, horarioEditandoId)) {
            mensaje(FacesMessage.SEVERITY_WARN, "Horario duplicado",
                    "El empleado ya tiene este mismo turno asignado en un rango de fechas que se cruza.");
            return;
        }

        Horario horario = horarioEditandoId == null ? new Horario() : horarioDAO.buscarPorId(horarioEditandoId);
        if (horario == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Horario no encontrado", "Recargue la pantalla e intente nuevamente.");
            return;
        }
        horario.setEmpleadoId(empleadoId);
        horario.setTurno(turno);
        horario.setFechaInicio(inicio);
        horario.setFechaFin(fin);
        horario.setObservacion(limpiar(observacion));
        horario.setDiasDescanso(serializarDiasDescanso());
        horarioDAO.guardar(horario);

        mensaje(FacesMessage.SEVERITY_INFO, "Horario guardado",
                "El turno quedó asignado al empleado seleccionado.");
        limpiarFormulario();
        limpiarFiltrosSinRecargar();
        cargarHorarios();
    }

    public void editarHorario(Long id) {
        Horario horario = horarioDAO.buscarPorId(id);
        if (horario == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Horario no encontrado", null);
            return;
        }
        horarioEditandoId = horario.getId();
        empleadoId = horario.getEmpleadoId();
        turnoId = horario.getTurno() == null ? null : horario.getTurno().getId();
        fechaInicio = horario.getFechaInicio() == null ? null : horario.getFechaInicio().toString();
        fechaFin = horario.getFechaFin() == null ? null : horario.getFechaFin().toString();
        observacion = horario.getObservacion();
        diasDescansoSeleccionados = deserializarDiasDescanso(horario.getDiasDescanso());
    }

    public void eliminarHorario(Long id) {
        try {
            horarioDAO.eliminar(id);
            mensaje(FacesMessage.SEVERITY_INFO, "Horario eliminado", "La asignación fue retirada.");
            cargarHorarios();
        } catch (Exception e) {
            mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo eliminar el horario",
                    "Revise si el horario tiene asistencias o registros relacionados.");
        }
    }

    public void cancelarEdicion() {
        limpiarFormulario();
    }

    public void cargarHorarios() {
        totalHorarios = horarioDAO.contarFiltrado(filtroEmpleadoId, filtroTurnoId, busqueda);
        if (pagina > getTotalPaginas()) {
            pagina = getTotalPaginas();
        }
        horarios = horarioDAO.listarFiltrado(Math.max(0, (pagina - 1) * tamanioPagina),
                tamanioPagina, filtroEmpleadoId, filtroTurnoId, busqueda);
    }

    public void filtrar() {
        pagina = 1;
        cargarHorarios();
    }

    public void limpiarFiltros() {
        limpiarFiltrosSinRecargar();
        cargarHorarios();
    }

    public void irPagina(int pagina) {
        if (pagina >= 1 && pagina <= getTotalPaginas()) {
            this.pagina = pagina;
            cargarHorarios();
        }
    }

    public int getTotalPaginas() {
        return Math.max(1, (int) Math.ceil((double) totalHorarios / tamanioPagina));
    }

    public List<Integer> getPaginas() {
        List<Integer> paginas = new ArrayList<>();
        for (int i = 1; i <= getTotalPaginas(); i++) {
            paginas.add(i);
        }
        return paginas;
    }

    public String obtenerEmpleadoNombre(Long id) {
        return empleados.stream()
                .filter(empleado -> empleado.getId() != null && empleado.getId().equals(id))
                .map(Empleado::getNombreCompleto)
                .findFirst()
                .orElse("Empleado no disponible");
    }

    public boolean isModoEdicion() {
        return horarioEditandoId != null;
    }

    public String formatearFecha(LocalDate fecha) {
        return fecha == null ? "--/--/----" : fecha.format(FECHA_FORMATTER);
    }

    public String formatearRango(Horario horario) {
        if (horario == null || horario.getFechaInicio() == null || horario.getFechaFin() == null) {
            return "Sin rango";
        }
        return formatearFecha(horario.getFechaInicio()) + " al " + formatearFecha(horario.getFechaFin());
    }

    public String formatearDiasDescanso(Horario horario) {
        if (horario == null || horario.getDiasDescanso() == null || horario.getDiasDescanso().isBlank()) {
            return "Sin descanso asignado";
        }
        return Arrays.stream(horario.getDiasDescanso().split(","))
                .map(String::trim)
                .filter(codigo -> !codigo.isBlank())
                .map(codigo -> DIAS_SEMANA.getOrDefault(codigo, codigo))
                .collect(Collectors.joining(", "));
    }

    public List<SelectItem> getOpcionesDiasDescanso() {
        return DIAS_SEMANA.entrySet().stream()
                .map(entry -> new SelectItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private boolean validar() {
        if (empleadoId == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Empleado requerido", "Seleccione el empleado.");
            return false;
        }
        if (turnoId == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Turno requerido", "Seleccione el turno a asignar.");
            return false;
        }
        LocalDate inicio = parseFecha(fechaInicio);
        LocalDate fin = parseFecha(fechaFin);
        if (inicio == null || fin == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Fechas requeridas", "Ingrese fecha inicio y fecha fin.");
            return false;
        }
        if (fin.isBefore(inicio)) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Rango inválido", "La fecha fin no puede ser anterior a la fecha inicio.");
            return false;
        }
        return true;
    }

    private LocalDate parseFecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Fecha inválida", "Use el formato yyyy-MM-dd.");
            return null;
        }
    }

    private Turno buscarTurnoSeleccionado() {
        return turnos.stream()
                .filter(turno -> turno.getId() != null && turno.getId().equals(turnoId))
                .findFirst()
                .orElse(null);
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String serializarDiasDescanso() {
        if (diasDescansoSeleccionados == null || diasDescansoSeleccionados.length == 0) {
            return null;
        }
        return Arrays.stream(diasDescansoSeleccionados)
                .filter(valor -> valor != null && DIAS_SEMANA.containsKey(valor))
                .distinct()
                .collect(Collectors.joining(","));
    }

    private String[] deserializarDiasDescanso(String valor) {
        if (valor == null || valor.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(valor.split(","))
                .map(String::trim)
                .filter(codigo -> DIAS_SEMANA.containsKey(codigo))
                .toArray(String[]::new);
    }

    private void limpiarFormulario() {
        horarioEditandoId = null;
        empleadoId = null;
        turnoId = null;
        fechaInicio = null;
        fechaFin = null;
        observacion = null;
        diasDescansoSeleccionados = new String[0];
    }

    private void limpiarFiltrosSinRecargar() {
        filtroEmpleadoId = null;
        filtroTurnoId = null;
        busqueda = null;
        pagina = 1;
    }

    private void mensaje(FacesMessage.Severity severity, String resumen, String detalle) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, resumen, detalle));
    }

    public List<Horario> getHorarios() { return horarios; }
    public List<Empleado> getEmpleados() { return empleados; }
    public List<Turno> getTurnos() { return turnos; }
    public Long getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(Long empleadoId) { this.empleadoId = empleadoId; }
    public Long getTurnoId() { return turnoId; }
    public void setTurnoId(Long turnoId) { this.turnoId = turnoId; }
    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }
    public String getFechaFin() { return fechaFin; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String[] getDiasDescansoSeleccionados() { return diasDescansoSeleccionados; }
    public void setDiasDescansoSeleccionados(String[] diasDescansoSeleccionados) { this.diasDescansoSeleccionados = diasDescansoSeleccionados; }
    public Long getFiltroEmpleadoId() { return filtroEmpleadoId; }
    public void setFiltroEmpleadoId(Long filtroEmpleadoId) { this.filtroEmpleadoId = filtroEmpleadoId; }
    public Long getFiltroTurnoId() { return filtroTurnoId; }
    public void setFiltroTurnoId(Long filtroTurnoId) { this.filtroTurnoId = filtroTurnoId; }
    public String getBusqueda() { return busqueda; }
    public void setBusqueda(String busqueda) { this.busqueda = busqueda; }
    public int getPagina() { return pagina; }
    public long getTotalHorarios() { return totalHorarios; }
}
