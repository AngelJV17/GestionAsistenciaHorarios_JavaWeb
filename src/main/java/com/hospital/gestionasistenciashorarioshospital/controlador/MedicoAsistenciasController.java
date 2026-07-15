package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.AsistenciaDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.EmpleadoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.HorarioDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.MarcacionDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.SolicitudDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.VariableGlobalDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Asistencia;
import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import com.hospital.gestionasistenciashorarioshospital.modelo.Horario;
import com.hospital.gestionasistenciashorarioshospital.modelo.Marcacion;
import com.hospital.gestionasistenciashorarioshospital.modelo.Solicitud;
import com.hospital.gestionasistenciashorarioshospital.modelo.Usuario;
import com.hospital.gestionasistenciashorarioshospital.modelo.VariableGlobal;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Named
@ViewScoped
public class MedicoAsistenciasController implements Serializable {

    private static final Long TIPO_ENTRADA_ID = 8L;
    private static final Long TIPO_SALIDA_ID = 9L;
    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final HorarioDAO horarioDAO = new HorarioDAO();
    private final AsistenciaDAO asistenciaDAO = new AsistenciaDAO();
    private final MarcacionDAO marcacionDAO = new MarcacionDAO();
    private final SolicitudDAO solicitudDAO = new SolicitudDAO();
    private final VariableGlobalDAO variableGlobalDAO = new VariableGlobalDAO();

    private Empleado empleado;
    private Horario horarioActivo;
    private Asistencia asistenciaHoy;
    private List<Asistencia> asistencias = new ArrayList<>();
    private int mesFiltro;
    private int anioFiltro;
    private boolean yaRegistroEntrada;
    private boolean yaRegistroSalida;

    @PostConstruct
    public void init() {
        mesFiltro = LocalDate.now().getMonthValue();
        anioFiltro = LocalDate.now().getYear();
        Usuario usuario = getUsuarioSesion();
        if (usuario != null && usuario.getId() != null) {
            empleado = empleadoDAO.buscarPorUsuarioId(usuario.getId());
        }
        cargarDatos();
    }

    public void cargarDatos() {
        if (empleado == null || empleado.getId() == null) {
            horarioActivo = null;
            asistenciaHoy = null;
            asistencias = new ArrayList<>();
            yaRegistroEntrada = false;
            yaRegistroSalida = false;
            return;
        }
        horarioActivo = horarioDAO.buscarHorarioActivo(empleado.getId());
        asistenciaHoy = asistenciaDAO.buscarHoy(empleado.getId());
        asistencias = asistenciaDAO.listarPorMes(empleado.getId(), mesFiltro, anioFiltro);
        yaRegistroEntrada = marcacionDAO.existeMarcacionHoy(empleado.getId(), TIPO_ENTRADA_ID)
                || (asistenciaHoy != null && asistenciaHoy.getFechaHoraEntrada() != null);
        yaRegistroSalida = marcacionDAO.existeMarcacionHoy(empleado.getId(), TIPO_SALIDA_ID)
                || (asistenciaHoy != null && asistenciaHoy.getFechaHoraSalida() != null);
    }

    public void registrarEntrada() {
        if (!validarMarcacionDisponible(true)) {
            return;
        }
        if (yaRegistroEntrada) {
            mensaje(FacesMessage.SEVERITY_WARN, "Entrada ya registrada", "Ya existe una entrada para el día de hoy.");
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        int minutosTardanza = calcularTardanza(ahora.toLocalTime());
        VariableGlobal estado = variableGlobalDAO.buscarPorCodigo(minutosTardanza > 0 ? "TARDANZA" : "ASISTIO");
        if (estado == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Catálogo incompleto", "No existe el estado de asistencia requerido.");
            return;
        }

        Marcacion marcacion = nuevaMarcacion(TIPO_ENTRADA_ID, ahora);
        marcacionDAO.guardar(marcacion);

        Asistencia asistencia = asistenciaHoy == null ? new Asistencia() : asistenciaHoy;
        if (asistencia.getId() == null) {
            asistencia.setEmpleado(empleado);
            asistencia.setHorarioId(horarioActivo.getId());
            asistencia.setFechaAsistencia(LocalDate.now());
        }
        asistencia.setFechaHoraEntrada(ahora);
        asistencia.setMinutosTardanza(minutosTardanza);
        asistencia.setEstadoAsistencia(estado);
        asistenciaDAO.guardar(asistencia);

        cargarDatos();
        mensaje(FacesMessage.SEVERITY_INFO, "Entrada registrada",
                minutosTardanza > 0
                        ? "Entrada registrada con " + formatearDuracionMinutos(minutosTardanza) + " de tardanza."
                        : "Entrada registrada correctamente.");
    }

    public void registrarSalida() {
        if (!validarMarcacionDisponible(false)) {
            return;
        }
        if (!yaRegistroEntrada || asistenciaHoy == null || asistenciaHoy.getFechaHoraEntrada() == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Entrada requerida", "Primero registre su entrada.");
            return;
        }
        if (yaRegistroSalida) {
            mensaje(FacesMessage.SEVERITY_WARN, "Salida ya registrada", "Ya existe una salida para el día de hoy.");
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        marcacionDAO.guardar(nuevaMarcacion(TIPO_SALIDA_ID, ahora));

        long minutos = Duration.between(asistenciaHoy.getFechaHoraEntrada(), ahora).toMinutes();
        asistenciaHoy.setFechaHoraSalida(ahora);
        asistenciaHoy.setHorasTrabajadas(BigDecimal.valueOf(minutos / 60.0).setScale(2, RoundingMode.HALF_UP));
        asistenciaDAO.guardar(asistenciaHoy);

        cargarDatos();
        mensaje(FacesMessage.SEVERITY_INFO, "Salida registrada", "Salida registrada correctamente.");
    }

    public void buscar() {
        cargarDatos();
    }

    public String obtenerEstadoTexto(Asistencia asistencia) {
        if (asistencia == null || asistencia.getEstadoAsistencia() == null) {
            return "Sin registro";
        }
        if (esJustificacionAprobada(asistencia)) {
            return "Justificado";
        }
        return asistencia.getEstadoAsistencia().getNombre();
    }

    public String obtenerEstadoCodigo(Asistencia asistencia) {
        if (asistencia == null || asistencia.getEstadoAsistencia() == null
                || asistencia.getEstadoAsistencia().getCodigo() == null) {
            return "SIN_REGISTRO";
        }
        return asistencia.getEstadoAsistencia().getCodigo().toUpperCase(Locale.ROOT);
    }

    public String obtenerEstadoClase(Asistencia asistencia) {
        String codigo = asistencia == null || asistencia.getEstadoAsistencia() == null
                ? "" : asistencia.getEstadoAsistencia().getCodigo();
        switch (codigo == null ? "" : codigo.toUpperCase(Locale.ROOT)) {
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
            case "VACACIONES":
                return "badge bg-primary";
            default:
                return "badge bg-secondary";
        }
    }

    public boolean puedeSolicitarJustificacion(Asistencia asistencia) {
        if (asistencia == null || asistencia.getFechaAsistencia() == null || empleado == null) {
            return false;
        }
        String codigo = obtenerEstadoCodigo(asistencia);
        boolean incidencia = "TARDANZA".equals(codigo) || "FALTA".equals(codigo);
        return incidencia && !solicitudDAO.existeJustificacionPorEmpleadoYFecha(empleado.getId(), asistencia.getFechaAsistencia());
    }

    public String obtenerTextoJustificacion(Asistencia asistencia) {
        if (asistencia == null || asistencia.getFechaAsistencia() == null || empleado == null) {
            return "";
        }
        Solicitud solicitud = solicitudDAO.buscarJustificacionPorEmpleadoYFecha(empleado.getId(), asistencia.getFechaAsistencia());
        if (solicitud == null || solicitud.getEstado() == null) {
            return "";
        }
        String codigo = solicitud.getEstado().getCodigo() == null ? "" : solicitud.getEstado().getCodigo();
        if ("APROBADA".equalsIgnoreCase(codigo)) {
            return "Justificado";
        }
        if ("RECHAZADA".equalsIgnoreCase(codigo)) {
            return "Justificación rechazada";
        }
        return "Justificación solicitada";
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

    public String getHoraEntradaHoy() {
        return asistenciaHoy == null ? "--:--" : formatearHora(asistenciaHoy.getFechaHoraEntrada());
    }

    public String getHoraSalidaHoy() {
        return asistenciaHoy == null ? "--:--" : formatearHora(asistenciaHoy.getFechaHoraSalida());
    }

    public long getTotalAsistencias() {
        return asistencias.stream().filter(a -> codigoEstado(a, "ASISTIO") || codigoEstado(a, "ASISTIÓ")).count();
    }

    public long getTotalTardanzas() {
        return asistencias.stream().filter(a -> codigoEstado(a, "TARDANZA")).count();
    }

    public long getTotalFaltas() {
        return asistencias.stream().filter(a -> codigoEstado(a, "FALTA")).count();
    }

    public long getTotalJustificados() {
        return asistencias.stream().filter(a -> codigoEstado(a, "PERMISO") || codigoEstado(a, "LICENCIA")).count();
    }

    public String getTurnoActivoNombre() {
        return horarioActivo == null || horarioActivo.getTurno() == null ? "Sin turno activo" : horarioActivo.getTurno().getNombre();
    }

    public String getTurnoActivoHorario() {
        if (horarioActivo == null || horarioActivo.getTurno() == null) {
            return "Solicite la programación del horario.";
        }
        return horarioActivo.getTurno().getHoraInicio().format(HORA_FORMATTER)
                + " - "
                + horarioActivo.getTurno().getHoraFin().format(HORA_FORMATTER);
    }

    public String getMesAnioTexto() {
        String mes = Month.of(mesFiltro).getDisplayName(TextStyle.FULL, new Locale("es", "PE"));
        return mes.substring(0, 1).toUpperCase(Locale.ROOT) + mes.substring(1) + " de " + anioFiltro;
    }

    public int getDiasHabilesRegistrados() {
        return asistencias == null ? 0 : asistencias.size();
    }

    private boolean validarMarcacionDisponible(boolean validarVentanaEntrada) {
        if (empleado == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Perfil no disponible", "No hay un empleado vinculado al usuario actual.");
            return false;
        }
        if (horarioActivo == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Horario no disponible", "No hay un horario activo para registrar asistencia.");
            return false;
        }
        if (esDiaDescanso(horarioActivo, LocalDate.now())) {
            mensaje(FacesMessage.SEVERITY_INFO, "Día de descanso", "Hoy figura como día de descanso en su horario asignado.");
            return false;
        }
        if (horarioActivo.getTurno() == null || horarioActivo.getTurno().getHoraInicio() == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Turno incompleto", "El turno activo no tiene hora de inicio configurada.");
            return false;
        }
        if (!validarVentanaEntrada) {
            return true;
        }
        LocalTime horaInicio = horarioActivo.getTurno().getHoraInicio();
        LocalTime horaFin = horarioActivo.getTurno().getHoraFin();
        if (horaFin == null) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Turno incompleto", "El turno activo no tiene hora de fin configurada.");
            return false;
        }
        LocalTime ahora = LocalTime.now();
        if (!estaDentroDeVentanaTurno(ahora, horaInicio, horaFin)) {
            if (esAntesDelInicio(ahora, horaInicio, horaFin)) {
                mensaje(FacesMessage.SEVERITY_INFO, "Marcación anticipada",
                        "Podrá registrar asistencia desde las " + horaInicio.format(HORA_FORMATTER) + ".");
            } else {
                mensaje(FacesMessage.SEVERITY_WARN, "Turno finalizado",
                        "Ya pasó el horario permitido para registrar la entrada de este turno.");
            }
            return false;
        }
        return true;
    }

    private boolean estaDentroDeVentanaTurno(LocalTime hora, LocalTime inicio, LocalTime fin) {
        if (inicio.equals(fin)) {
            return true;
        }
        if (fin.isAfter(inicio)) {
            return !hora.isBefore(inicio) && !hora.isAfter(fin);
        }
        return !hora.isBefore(inicio) || !hora.isAfter(fin);
    }

    private boolean esAntesDelInicio(LocalTime hora, LocalTime inicio, LocalTime fin) {
        if (fin.isAfter(inicio)) {
            return hora.isBefore(inicio);
        }
        return hora.isBefore(inicio) && hora.isAfter(fin);
    }
    private boolean esDiaDescanso(Horario horario, LocalDate fecha) {
        if (horario == null || horario.getDiasDescanso() == null || horario.getDiasDescanso().isBlank()) {
            return false;
        }
        String codigoHoy = codigoDia(fecha);
        return Arrays.stream(horario.getDiasDescanso().split(","))
                .map(String::trim)
                .anyMatch(codigo -> codigo.equalsIgnoreCase(codigoHoy));
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

    private int calcularTardanza(LocalTime horaReal) {
        if (horarioActivo == null || horarioActivo.getTurno() == null || horarioActivo.getTurno().getHoraInicio() == null) {
            return 0;
        }
        int tolerancia = horarioActivo.getTurno().getToleranciaMinutos() == null ? 15 : horarioActivo.getTurno().getToleranciaMinutos();
        LocalTime limite = horarioActivo.getTurno().getHoraInicio().plusMinutes(tolerancia);
        if (!horaReal.isAfter(limite)) {
            return 0;
        }
        return (int) Duration.between(horarioActivo.getTurno().getHoraInicio(), horaReal).toMinutes();
    }

    private Marcacion nuevaMarcacion(Long tipoMarcacionId, LocalDateTime fechaHora) {
        Marcacion marcacion = new Marcacion();
        marcacion.setEmpleadoId(empleado.getId());
        marcacion.setTipoMarcacionId(tipoMarcacionId);
        marcacion.setFechaHora(fechaHora);
        marcacion.setOrigen("SISTEMA");
        return marcacion;
    }

    private boolean codigoEstado(Asistencia asistencia, String codigo) {
        return asistencia != null && asistencia.getEstadoAsistencia() != null
                && asistencia.getEstadoAsistencia().getCodigo() != null
                && asistencia.getEstadoAsistencia().getCodigo().equalsIgnoreCase(codigo);
    }

    private boolean esJustificacionAprobada(Asistencia asistencia) {
        return asistencia != null
                && asistencia.getObservacion() != null
                && asistencia.getObservacion().toLowerCase(Locale.ROOT).contains("justificación aprobada");
    }

    private Usuario getUsuarioSesion() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        Object usuarioSesion = externalContext.getSessionMap().get("usuarioLogueado");
        return usuarioSesion instanceof Usuario ? (Usuario) usuarioSesion : null;
    }

    private void mensaje(FacesMessage.Severity severity, String resumen, String detalle) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, resumen, detalle));
    }

    public Empleado getEmpleado() { return empleado; }
    public Horario getHorarioActivo() { return horarioActivo; }
    public Asistencia getAsistenciaHoy() { return asistenciaHoy; }
    public List<Asistencia> getAsistencias() { return asistencias; }
    public int getMesFiltro() { return mesFiltro; }
    public void setMesFiltro(int mesFiltro) { this.mesFiltro = mesFiltro; }
    public int getAnioFiltro() { return anioFiltro; }
    public void setAnioFiltro(int anioFiltro) { this.anioFiltro = anioFiltro; }
    public boolean isYaRegistroEntrada() { return yaRegistroEntrada; }
    public boolean isYaRegistroSalida() { return yaRegistroSalida; }
}
