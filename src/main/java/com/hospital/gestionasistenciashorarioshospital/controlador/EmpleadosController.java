package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.EmpleadoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.RolPermisoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.SimpleEntityDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.UsuarioDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Area;
import com.hospital.gestionasistenciashorarioshospital.modelo.Cargo;
import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import com.hospital.gestionasistenciashorarioshospital.modelo.Especialidad;
import com.hospital.gestionasistenciashorarioshospital.modelo.Rol;
import com.hospital.gestionasistenciashorarioshospital.modelo.Sede;
import com.hospital.gestionasistenciashorarioshospital.modelo.Usuario;
import com.hospital.gestionasistenciashorarioshospital.util.CodigoUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;

@Named
@ViewScoped
public class EmpleadosController implements Serializable {

    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final RolPermisoDAO rolPermisoDAO = new RolPermisoDAO();

    private final SimpleEntityDAO<Sede> sedeDAO = new SimpleEntityDAO<>(Sede.class);
    private final SimpleEntityDAO<Area> areaDAO = new SimpleEntityDAO<>(Area.class);
    private final SimpleEntityDAO<Cargo> cargoDAO = new SimpleEntityDAO<>(Cargo.class);
    private final SimpleEntityDAO<Especialidad> especialidadDAO = new SimpleEntityDAO<>(Especialidad.class);

    private List<Empleado> empleados = new ArrayList<>();
    private List<Sede> sedes = new ArrayList<>();
    private List<Area> areas = new ArrayList<>();
    private List<Cargo> cargos = new ArrayList<>();
    private List<Especialidad> especialidades = new ArrayList<>();
    private List<Rol> rolesAcceso = new ArrayList<>();

    private Empleado empleadoSeleccionado = new Empleado();

    private Long sedeId;
    private Long areaId;
    private Long cargoId;
    private Long especialidadId;

    private int pagina = 1;
    private final int tamanioPagina = 5;
    private long totalEmpleados;

    private boolean mostrarModal;
    private boolean crearAccesoSistema;

    private String codigoRolAcceso;
    private Long filtroCargoId;
    private Long filtroAreaId;
    private String filtroEstado = "todos";
    private String busqueda;

    @PostConstruct
    public void init() {
        cargarCatalogos();
        cargarEmpleados();
        nuevoEmpleado(false);
    }

    private void cargarCatalogos() {
        sedes = sedeDAO.listarTodos();
        areas = areaDAO.listarTodos();
        cargos = cargoDAO.listarTodos();
        especialidades = especialidadDAO.listarTodos();
        rolesAcceso = rolPermisoDAO.listarRoles();
    }

    public void cargarEmpleados() {
        totalEmpleados = empleadoDAO.contarFiltrado(
                filtroCargoId,
                filtroAreaId,
                filtroEstado,
                busqueda
        );

        if (pagina > getTotalPaginas()) {
            pagina = getTotalPaginas();
        }

        empleados = empleadoDAO.listarFiltrado(
                Math.max(0, (pagina - 1) * tamanioPagina),
                tamanioPagina,
                filtroCargoId,
                filtroAreaId,
                filtroEstado,
                busqueda
        );
    }

    public void nuevoEmpleado() {
        nuevoEmpleado(true);
    }

    private void nuevoEmpleado(boolean abrirModal) {
        empleadoSeleccionado = new Empleado();

        empleadoSeleccionado.setCodigoEmpleado(
                CodigoUtil.generarCodigoSecuencial(
                        "EMP",
                        empleadoDAO.obtenerUltimoNumeroCodigo("EMP"),
                        6
                )
        );

        empleadoSeleccionado.setActivo(true);
        empleadoSeleccionado.setFechaIngreso(LocalDate.now());

        sedeId = null;
        areaId = null;
        cargoId = null;
        especialidadId = null;

        crearAccesoSistema = false;
        codigoRolAcceso = "";

        mostrarModal = abrirModal;
    }

    public void editarEmpleado(Long id) {
        empleadoSeleccionado = empleadoDAO.buscarPorId(id);

        if (empleadoSeleccionado == null) {
            mostrarModal = false;
            mensaje(
                    FacesMessage.SEVERITY_WARN,
                    "Empleado no encontrado",
                    "No se encontró el empleado seleccionado."
            );
            return;
        }

        sedeId = empleadoSeleccionado.getSede() != null
                ? empleadoSeleccionado.getSede().getId()
                : null;

        areaId = empleadoSeleccionado.getArea() != null
                ? empleadoSeleccionado.getArea().getId()
                : null;

        cargoId = empleadoSeleccionado.getCargo() != null
                ? empleadoSeleccionado.getCargo().getId()
                : null;

        especialidadId = empleadoSeleccionado.getEspecialidad() != null
                ? empleadoSeleccionado.getEspecialidad().getId()
                : null;

        crearAccesoSistema = empleadoSeleccionado.getUsuario() != null;
        codigoRolAcceso = "";

        mostrarModal = true;
    }

    public void guardarEmpleado() {
        try {
            if (!validarEmpleado()) {
                return;
            }

            if (empleadoSeleccionado.getCodigoEmpleado() == null
                    || empleadoSeleccionado.getCodigoEmpleado().isBlank()) {

                empleadoSeleccionado.setCodigoEmpleado(
                        CodigoUtil.generarCodigoSecuencial(
                                "EMP",
                                empleadoDAO.obtenerUltimoNumeroCodigo("EMP"),
                                6
                        )
                );
            }

            empleadoSeleccionado.setSede(sedeDAO.buscarPorId(sedeId));
            empleadoSeleccionado.setArea(areaDAO.buscarPorId(areaId));
            empleadoSeleccionado.setCargo(cargoDAO.buscarPorId(cargoId));

            empleadoSeleccionado.setEspecialidad(
                    especialidadId != null
                            ? especialidadDAO.buscarPorId(especialidadId)
                            : null
            );

            Usuario usuarioCreado = null;

            if (crearAccesoSistema && empleadoSeleccionado.getUsuario() == null) {
                usuarioCreado = crearUsuarioParaEmpleado();
                empleadoSeleccionado.setUsuario(usuarioCreado);
            }

            empleadoDAO.guardar(empleadoSeleccionado);

            cargarEmpleados();
            nuevoEmpleado(false);

            String detalle = usuarioCreado == null
                    ? "Los datos se guardaron correctamente."
                    : "Empleado guardado. Usuario creado: "
                    + usuarioCreado.getNombreUsuario()
                    + ". Clave temporal: DNI o código de empleado.";

            mensaje(
                    FacesMessage.SEVERITY_INFO,
                    "Empleado guardado",
                    detalle
            );

        } catch (Exception e) {
            agregarError(
                    "No se pudo guardar el empleado",
                    extraerMensajeError(e)
            );
        }
    }

    private boolean validarEmpleado() {
        if (empleadoSeleccionado.getNombres() == null
                || empleadoSeleccionado.getNombres().isBlank()
                || empleadoSeleccionado.getApellidoPaterno() == null
                || empleadoSeleccionado.getApellidoPaterno().isBlank()) {

            agregarError(
                    "Datos requeridos",
                    "Ingrese nombres y apellido paterno."
            );
            return false;
        }

        if (empleadoSeleccionado.getDni() == null
                || !empleadoSeleccionado.getDni().matches("\\d{8}")) {

            agregarError(
                    "DNI inválido",
                    "El DNI debe tener 8 dígitos."
            );
            return false;
        }

        if (sedeId == null || areaId == null || cargoId == null) {
            agregarError(
                    "Datos laborales requeridos",
                    "Seleccione sede, área y cargo."
            );
            return false;
        }

        if (crearAccesoSistema
                && (codigoRolAcceso == null || codigoRolAcceso.isBlank())) {

            agregarError(
                    "Rol requerido",
                    "Seleccione el rol que tendrá el usuario del empleado."
            );
            return false;
        }

        return true;
    }

    private void agregarError(String resumen, String detalle) {
        mostrarModal = true;

        mensaje(
                FacesMessage.SEVERITY_ERROR,
                resumen,
                detalle
        );
    }

    private void mensaje(FacesMessage.Severity severity, String resumen, String detalle) {
        FacesContext.getCurrentInstance().addMessage(
                null,
                new FacesMessage(severity, resumen, detalle)
        );
    }

    private String extraerMensajeError(Exception e) {
        String mensaje = e.getMessage();
        Throwable causa = e.getCause();

        while (causa != null) {
            if (causa.getMessage() != null) {
                mensaje = causa.getMessage();
            }

            causa = causa.getCause();
        }

        if (mensaje != null && mensaje.toLowerCase().contains("duplicate")) {
            return "Revise el DNI o código de empleado; ya existe un registro con esos datos.";
        }

        return mensaje == null
                ? "Revise los datos obligatorios e intente nuevamente."
                : mensaje;
    }

    public void cambiarEstado(Long id) {
        try {
            Empleado empleado = empleadoDAO.buscarPorId(id);

            if (empleado != null) {
                empleado.setActivo(!Boolean.TRUE.equals(empleado.getActivo()));
                empleadoDAO.guardar(empleado);
                cargarEmpleados();

                mensaje(
                        FacesMessage.SEVERITY_INFO,
                        "Empleado actualizado",
                        "El estado del empleado se actualizó correctamente."
                );
            }

            mostrarModal = false;

        } catch (Exception e) {
            mostrarModal = false;

            mensaje(
                    FacesMessage.SEVERITY_ERROR,
                    "No se pudo actualizar",
                    extraerMensajeError(e)
            );
        }
    }

    public void filtrar() {
        pagina = 1;
        mostrarModal = false;
        cargarEmpleados();
    }

    private Usuario crearUsuarioParaEmpleado() {
        Usuario usuario = new Usuario();

        usuario.setNombreUsuario(generarNombreUsuario());
        usuario.setCorreoRecuperacion(empleadoSeleccionado.getCorreo());
        usuario.setEstadoId(1L);

        String claveTemporal = empleadoSeleccionado.getDni() != null
                && !empleadoSeleccionado.getDni().isBlank()
                        ? empleadoSeleccionado.getDni()
                        : empleadoSeleccionado.getCodigoEmpleado();

        usuario.setPasswordHash(
                BCrypt.hashpw(claveTemporal, BCrypt.gensalt())
        );

        usuarioDAO.guardar(usuario);

        Usuario guardado = usuarioDAO.buscarPorNombreUsuario(
                usuario.getNombreUsuario()
        );

        if (guardado != null
                && codigoRolAcceso != null
                && !codigoRolAcceso.isBlank()) {

            rolPermisoDAO.asignarRolAUsuarioSiNoExiste(
                    guardado.getId(),
                    codigoRolAcceso
            );
        }

        return guardado != null ? guardado : usuario;
    }

    private String generarNombreUsuario() {
        String inicial = empleadoSeleccionado.getNombres() != null
                && !empleadoSeleccionado.getNombres().isBlank()
                        ? empleadoSeleccionado.getNombres().trim().substring(0, 1)
                        : "";

        String apellido = empleadoSeleccionado.getApellidoPaterno() == null
                ? ""
                : empleadoSeleccionado.getApellidoPaterno().trim();

        String base = CodigoUtil.normalizarUsuario(inicial + apellido);

        if (base.isBlank()) {
            base = CodigoUtil.normalizarUsuario(
                    empleadoSeleccionado.getCodigoEmpleado()
            );
        }

        String candidato = base;
        int contador = 1;

        while (usuarioDAO.existeNombreUsuario(candidato)) {
            contador++;
            candidato = base + contador;
        }

        return candidato;
    }

    public int getTotalPaginas() {
        return Math.max(
                1,
                (int) Math.ceil((double) totalEmpleados / tamanioPagina)
        );
    }

    public List<Empleado> getEmpleados() {
        return empleados;
    }

    public List<Sede> getSedes() {
        return sedes;
    }

    public List<Area> getAreas() {
        return areas;
    }

    public List<Cargo> getCargos() {
        return cargos;
    }

    public List<Especialidad> getEspecialidades() {
        return especialidades;
    }

    public List<Rol> getRolesAcceso() {
        return rolesAcceso;
    }

    public Empleado getEmpleadoSeleccionado() {
        return empleadoSeleccionado;
    }

    public void setEmpleadoSeleccionado(Empleado empleadoSeleccionado) {
        this.empleadoSeleccionado = empleadoSeleccionado;
    }

    public Long getSedeId() {
        return sedeId;
    }

    public void setSedeId(Long sedeId) {
        this.sedeId = sedeId;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public Long getCargoId() {
        return cargoId;
    }

    public void setCargoId(Long cargoId) {
        this.cargoId = cargoId;
    }

    public Long getEspecialidadId() {
        return especialidadId;
    }

    public void setEspecialidadId(Long especialidadId) {
        this.especialidadId = especialidadId;
    }

    public int getPagina() {
        return pagina;
    }

    public long getTotalEmpleados() {
        return totalEmpleados;
    }

    public long getTotalActivos() {
        return empleadoDAO.contarActivos();
    }

    public boolean getMostrarModal() {
        return mostrarModal;
    }

    public boolean isMostrarModal() {
        return mostrarModal;
    }

    public void setMostrarModal(boolean mostrarModal) {
        this.mostrarModal = mostrarModal;
    }

    public boolean isCrearAccesoSistema() {
        return crearAccesoSistema;
    }

    public boolean getCrearAccesoSistema() {
        return crearAccesoSistema;
    }

    public void setCrearAccesoSistema(boolean crearAccesoSistema) {
        this.crearAccesoSistema = crearAccesoSistema;
    }

    public String getCodigoRolAcceso() {
        return codigoRolAcceso;
    }

    public void setCodigoRolAcceso(String codigoRolAcceso) {
        this.codigoRolAcceso = codigoRolAcceso;
    }

    public Long getFiltroCargoId() {
        return filtroCargoId;
    }

    public void setFiltroCargoId(Long filtroCargoId) {
        this.filtroCargoId = filtroCargoId;
    }

    public Long getFiltroAreaId() {
        return filtroAreaId;
    }

    public void setFiltroAreaId(Long filtroAreaId) {
        this.filtroAreaId = filtroAreaId;
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