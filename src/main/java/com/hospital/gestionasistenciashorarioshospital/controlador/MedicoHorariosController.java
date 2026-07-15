package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.EmpleadoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.HorarioDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.SimpleEntityDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.SolicitudDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.TurnoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.VariableGlobalDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import com.hospital.gestionasistenciashorarioshospital.modelo.Horario;
import com.hospital.gestionasistenciashorarioshospital.modelo.Solicitud;
import com.hospital.gestionasistenciashorarioshospital.modelo.TipoSolicitud;
import com.hospital.gestionasistenciashorarioshospital.modelo.Turno;
import com.hospital.gestionasistenciashorarioshospital.modelo.Usuario;
import com.hospital.gestionasistenciashorarioshospital.modelo.VariableGlobal;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Named
@ViewScoped
public class MedicoHorariosController implements Serializable {

    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
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

    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final HorarioDAO horarioDAO = new HorarioDAO();
    private final TurnoDAO turnoDAO = new TurnoDAO();
    private final SolicitudDAO solicitudDAO = new SolicitudDAO();
    private final VariableGlobalDAO variableGlobalDAO = new VariableGlobalDAO();
    private final SimpleEntityDAO<TipoSolicitud> tipoSolicitudDAO = new SimpleEntityDAO<>(TipoSolicitud.class);

    private Empleado empleado;
    private Horario horarioActivo;
    private List<Horario> horarios = new ArrayList<>();
    private List<Solicitud> solicitudesRecientes = new ArrayList<>();
    private List<Turno> turnosActivos = new ArrayList<>();
    private Long turnoSolicitadoId;
    private LocalDate fechaCambio;
    private String motivoCambio;

    @PostConstruct
    public void init() {
        Usuario usuario = getUsuarioSesion();
        if (usuario != null && usuario.getId() != null) {
            empleado = empleadoDAO.buscarPorUsuarioId(usuario.getId());
        }
        turnosActivos = turnoDAO.listarFiltrado(0, 500, "activo", null);
        cargarHorarios();
    }

    public void cargarHorarios() {
        if (empleado == null || empleado.getId() == null) {
            horarios = new ArrayList<>();
            horarioActivo = null;
            solicitudesRecientes = new ArrayList<>();
            return;
        }
        horarios = horarioDAO.listarPorEmpleado(empleado.getId());
        horarioActivo = horarioDAO.buscarHorarioActivo(empleado.getId());
        solicitudesRecientes = solicitudDAO.listarPorEmpleado(0, 3, empleado.getId(), null);
    }

    public void solicitarCambioTurno() {
        if (empleado == null || empleado.getId() == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Perfil no disponible",
                    "No se encontró un empleado vinculado al usuario actual.");
            return;
        }
        if (turnoSolicitadoId == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Turno requerido", "Seleccione el turno al que desea cambiar.");
            return;
        }
        if (fechaCambio == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Fecha requerida", "Seleccione desde cuándo aplicaría el cambio.");
            return;
        }
        if (motivoCambio == null || motivoCambio.trim().length() < 10) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Motivo requerido", "Explique el motivo con al menos 10 caracteres.");
            return;
        }

        Turno turno = turnoDAO.buscarPorId(turnoSolicitadoId);
        TipoSolicitud tipoCambio = buscarTipoCambioTurno();
        VariableGlobal estadoPendiente = variableGlobalDAO.buscarPorCodigo("PENDIENTE");
        if (turno == null || tipoCambio == null || estadoPendiente == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Catálogo incompleto",
                    "Revise que exista el turno, el tipo CAMBIO_TURNO y el estado PENDIENTE.");
            return;
        }

        Solicitud solicitud = new Solicitud();
        solicitud.setEmpleado(empleado);
        solicitud.setTipoSolicitud(tipoCambio);
        solicitud.setEstado(estadoPendiente);
        solicitud.setFechaInicio(fechaCambio);
        solicitud.setFechaFin(fechaCambio);
        solicitud.setMotivo("Cambio solicitado al turno " + turno.getNombre() + ". Motivo: "
                + motivoCambio.trim() + " [[TURNO_ID:" + turno.getId() + "]]");
        solicitudDAO.guardar(solicitud);

        turnoSolicitadoId = null;
        fechaCambio = null;
        motivoCambio = null;
        mensaje(FacesMessage.SEVERITY_INFO, "Solicitud registrada",
                "El cambio de turno fue enviado para aprobación administrativa.");
    }

    public String formatearHora(LocalTime hora) {
        return hora == null ? "--:--" : hora.format(HORA_FORMATTER);
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

    public String obtenerClaseTurno(Horario horario) {
        String nombre = horario == null || horario.getTurno() == null
                ? ""
                : horario.getTurno().getNombre();

        String texto = nombre == null
                ? ""
                : nombre.toLowerCase(Locale.ROOT);

        if (texto.contains("mañana") || texto.contains("manana")) {
            return "shift-morning";
        }

        if (texto.contains("tarde")) {
            return "shift-afternoon";
        }

        if (texto.contains("noche") || texto.contains("nocturno")) {
            return "shift-night";
        }

        return "";
    }

    public String obtenerEstadoTexto(Horario horario) {
        if (esDiaDescansoHoy(horario)) {
            return "Descanso";
        }
        return horario != null && horario.estaActivo() ? "Activo" : "Programado";
    }

    public String obtenerEstadoClase(Horario horario) {
        if (esDiaDescansoHoy(horario)) {
            return "badge bg-secondary";
        }
        return horario != null && horario.estaActivo() ? "badge bg-success" : "badge bg-primary";
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

    public boolean esDiaDescansoHoy(Horario horario) {
        if (horario == null || horario.getDiasDescanso() == null || horario.getDiasDescanso().isBlank()) {
            return false;
        }
        String codigoHoy = codigoDia(LocalDate.now());
        return Arrays.stream(horario.getDiasDescanso().split(","))
                .map(String::trim)
                .anyMatch(codigo -> codigo.equalsIgnoreCase(codigoHoy));
    }

    public String getDescansoActivoTexto() {
        if (horarioActivo == null) {
            return "Sin horario activo";
        }
        return esDiaDescansoHoy(horarioActivo)
                ? "Hoy es día de descanso"
                : formatearDiasDescanso(horarioActivo);
    }

    public long contarTurnosPorNombre(String nombreTurno) {
        if (nombreTurno == null) {
            return 0;
        }
        String esperado = nombreTurno.toLowerCase(Locale.ROOT);
        return horarios.stream()
                .filter(horario -> horario.getTurno() != null && horario.getTurno().getNombre() != null)
                .filter(horario -> horario.getTurno().getNombre().toLowerCase(Locale.ROOT).contains(esperado))
                .count();
    }

    public List<Map<String, String>> getProximosHorarios() {
        List<Map<String, String>> proximos = new ArrayList<>();
        for (Horario horario : horarios) {
            if (proximos.size() >= 3) {
                break;
            }
            Map<String, String> item = new LinkedHashMap<>();
            Turno turno = horario.getTurno();
            item.put("periodo", formatearRango(horario));
            item.put("turno", turno == null ? "Sin turno" : turno.getNombre());
            item.put("horario", turno == null ? "--:-- - --:--" : formatearHora(turno.getHoraInicio()) + " - " + formatearHora(turno.getHoraFin()));
            item.put("descanso", formatearDiasDescanso(horario));
            proximos.add(item);
        }
        return proximos;
    }

    public List<Map<String, Object>> getHorarioSemanal() {
        List<Map<String, Object>> semana = new ArrayList<>();
        LocalDate lunes = LocalDate.now().with(DayOfWeek.MONDAY);
        for (int i = 0; i < 7; i++) {
            LocalDate fecha = lunes.plusDays(i);
            Horario horario = buscarHorarioParaFecha(fecha);
            boolean libre = horario == null || esDiaDescanso(horario, fecha);
            Turno turno = libre || horario.getTurno() == null ? null : horario.getTurno();

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("dia", nombreDia(fecha));
            fila.put("fecha", formatearFecha(fecha));
            fila.put("turno", turno == null ? "-" : limpiarNombreTurno(turno.getNombre()));
            fila.put("inicio", turno == null ? "-" : formatearHora(turno.getHoraInicio()));
            fila.put("fin", turno == null ? "-" : formatearHora(turno.getHoraFin()));
            fila.put("estado", libre ? "Libre" : "Confirmado");
            fila.put("clase", libre ? "schedule-free" : obtenerClaseTurno(horario));
            fila.put("libre", libre);
            semana.add(fila);
        }
        return semana;
    }

    public String getTurnoActivoNombre() {
        return horarioActivo == null || horarioActivo.getTurno() == null ? "Sin turno activo" : horarioActivo.getTurno().getNombre();
    }

    public String getTurnoActivoHorario() {
        return horarioActivo == null || horarioActivo.getTurno() == null
                ? "--:-- - --:--"
                : formatearHora(horarioActivo.getTurno().getHoraInicio()) + " - " + formatearHora(horarioActivo.getTurno().getHoraFin());
    }

    public String getTurnoHoyNombre() {
        Horario horario = buscarHorarioParaFecha(LocalDate.now());
        if (horario == null || esDiaDescanso(horario, LocalDate.now()) || horario.getTurno() == null) {
            return "Día libre";
        }
        return limpiarNombreTurno(horario.getTurno().getNombre());
    }

    public String getTurnoHoyHorario() {
        Horario horario = buscarHorarioParaFecha(LocalDate.now());
        if (horario == null || esDiaDescanso(horario, LocalDate.now()) || horario.getTurno() == null) {
            return "-";
        }
        return formatearHora(horario.getTurno().getHoraInicio()) + " - " + formatearHora(horario.getTurno().getHoraFin());
    }

    public long getTurnosMananaMes() {
        return contarDiasTurnoMes("mañana", "manana");
    }

    public long getTurnosTardeMes() {
        return contarDiasTurnoMes("tarde");
    }

    public long getTurnosNocheMes() {
        return contarDiasTurnoMes("noche", "nocturno");
    }

    public long getDiasLibresMes() {
        YearMonth mes = YearMonth.now();
        long total = 0;
        for (int dia = 1; dia <= mes.lengthOfMonth(); dia++) {
            LocalDate fecha = mes.atDay(dia);
            Horario horario = buscarHorarioParaFecha(fecha);
            if (horario == null || esDiaDescanso(horario, fecha)) {
                total++;
            }
        }
        return total;
    }

    public boolean isTieneSolicitudesRecientes() {
        return solicitudesRecientes != null && !solicitudesRecientes.isEmpty();
    }

    public String resumirSolicitud(Solicitud solicitud) {
        if (solicitud == null) {
            return "Solicitud";
        }
        String tipo = solicitud.getTipoSolicitud() == null ? "Solicitud" : solicitud.getTipoSolicitud().getNombre();
        String fecha = solicitud.getFechaInicio() == null ? "" : " " + formatearFecha(solicitud.getFechaInicio());
        return tipo + fecha;
    }

    public String obtenerEstadoSolicitud(Solicitud solicitud) {
        return solicitud == null || solicitud.getEstado() == null ? "Pendiente" : solicitud.getEstado().getNombre();
    }

    public String obtenerClaseEstadoSolicitud(Solicitud solicitud) {
        String codigo = solicitud == null
                || solicitud.getEstado() == null
                || solicitud.getEstado().getCodigo() == null
                ? ""
                : solicitud.getEstado().getCodigo().trim().toUpperCase(Locale.ROOT);

        String nombre = solicitud == null
                || solicitud.getEstado() == null
                || solicitud.getEstado().getNombre() == null
                ? ""
                : solicitud.getEstado().getNombre().trim().toLowerCase(Locale.ROOT);

        if ("APROBADO".equals(codigo)
                || "APROBADA".equals(codigo)
                || nombre.contains("aprob")) {
            return "badge approved";
        }

        if ("RECHAZADO".equals(codigo)
                || "RECHAZADA".equals(codigo)
                || nombre.contains("rechaz")) {
            return "badge rejected";
        }

        if ("PENDIENTE".equals(codigo)
                || nombre.contains("pend")) {
            return "badge pending";
        }

        if ("EN_REVISION".equals(codigo)
                || "REVISION".equals(codigo)
                || nombre.contains("revisión")
                || nombre.contains("revision")) {
            return "badge review";
        }

        return "badge processing";
    }

    private long contarDiasTurnoMes(String... nombres) {
        YearMonth mes = YearMonth.now();
        long total = 0;
        for (int dia = 1; dia <= mes.lengthOfMonth(); dia++) {
            LocalDate fecha = mes.atDay(dia);
            Horario horario = buscarHorarioParaFecha(fecha);
            if (horario == null || esDiaDescanso(horario, fecha) || horario.getTurno() == null
                    || horario.getTurno().getNombre() == null) {
                continue;
            }
            String nombre = horario.getTurno().getNombre().toLowerCase(Locale.ROOT);
            if (Arrays.stream(nombres).anyMatch(nombre::contains)) {
                total++;
            }
        }
        return total;
    }

    private TipoSolicitud buscarTipoCambioTurno() {
        return tipoSolicitudDAO.listarTodos().stream()
                .filter(tipo -> tipo.getCodigo() != null && "CAMBIO_TURNO".equalsIgnoreCase(tipo.getCodigo()))
                .findFirst()
                .orElse(null);
    }

    private Horario buscarHorarioParaFecha(LocalDate fecha) {
        if (fecha == null || horarios == null) {
            return null;
        }
        return horarios.stream()
                .filter(horario -> horario.getFechaInicio() != null && horario.getFechaFin() != null)
                .filter(horario -> !fecha.isBefore(horario.getFechaInicio()) && !fecha.isAfter(horario.getFechaFin()))
                .findFirst()
                .orElse(null);
    }

    private boolean esDiaDescanso(Horario horario, LocalDate fecha) {
        if (horario == null || horario.getDiasDescanso() == null || horario.getDiasDescanso().isBlank()) {
            return false;
        }
        String codigo = codigoDia(fecha);
        return Arrays.stream(horario.getDiasDescanso().split(","))
                .map(String::trim)
                .anyMatch(dia -> dia.equalsIgnoreCase(codigo));
    }

    private String nombreDia(LocalDate fecha) {
        switch (fecha.getDayOfWeek()) {
            case MONDAY:
                return "Lunes";
            case TUESDAY:
                return "Martes";
            case WEDNESDAY:
                return "Miércoles";
            case THURSDAY:
                return "Jueves";
            case FRIDAY:
                return "Viernes";
            case SATURDAY:
                return "Sábado";
            case SUNDAY:
                return "Domingo";
            default:
                return "";
        }
    }

    private String limpiarNombreTurno(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "Turno";
        }
        return nombre.replaceFirst("(?i)^turno\\s+", "").trim();
    }

    private String codigoDia(LocalDate fecha) {
        switch (fecha.getDayOfWeek()) {
            case MONDAY:
                return "LUN";
            case TUESDAY:
                return "MAR";
            case WEDNESDAY:
                return "MIE";
            case THURSDAY:
                return "JUE";
            case FRIDAY:
                return "VIE";
            case SATURDAY:
                return "SAB";
            case SUNDAY:
                return "DOM";
            default:
                return "";
        }
    }

    private void mensaje(FacesMessage.Severity severity, String resumen, String detalle) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, resumen, detalle));
    }

    private Usuario getUsuarioSesion() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        Object usuarioSesion = externalContext.getSessionMap().get("usuarioLogueado");
        return usuarioSesion instanceof Usuario ? (Usuario) usuarioSesion : null;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public Horario getHorarioActivo() {
        return horarioActivo;
    }

    public List<Horario> getHorarios() {
        return horarios;
    }

    public List<Solicitud> getSolicitudesRecientes() {
        return solicitudesRecientes;
    }

    public int getTotalHorarios() {
        return horarios == null ? 0 : horarios.size();
    }

    public List<Turno> getTurnosActivos() {
        return turnosActivos;
    }

    public Long getTurnoSolicitadoId() {
        return turnoSolicitadoId;
    }

    public void setTurnoSolicitadoId(Long turnoSolicitadoId) {
        this.turnoSolicitadoId = turnoSolicitadoId;
    }

    public LocalDate getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(LocalDate fechaCambio) {
        this.fechaCambio = fechaCambio;
    }

    public String getMotivoCambio() {
        return motivoCambio;
    }

    public void setMotivoCambio(String motivoCambio) {
        this.motivoCambio = motivoCambio;
    }
}
