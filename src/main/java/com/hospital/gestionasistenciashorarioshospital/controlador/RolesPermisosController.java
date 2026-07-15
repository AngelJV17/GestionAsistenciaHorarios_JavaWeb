package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.RolPermisoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.UsuarioDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Permiso;
import com.hospital.gestionasistenciashorarioshospital.modelo.Rol;
import com.hospital.gestionasistenciashorarioshospital.modelo.RolPermiso;
import com.hospital.gestionasistenciashorarioshospital.modelo.Usuario;
import com.hospital.gestionasistenciashorarioshospital.modelo.UsuarioRol;
import com.hospital.gestionasistenciashorarioshospital.util.CodigoUtil;
import com.hospital.gestionasistenciashorarioshospital.util.JPAUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Named
@ViewScoped
public class RolesPermisosController implements Serializable {

    private RolPermisoDAO rolPermisoDAO;
    private UsuarioDAO usuarioDAO;

    private List<Rol> roles = new ArrayList<>();
    private List<Rol> rolesTodos = new ArrayList<>();
    private List<Permiso> permisos = new ArrayList<>();
    private List<RolPermiso> asignaciones = new ArrayList<>();
    private List<UsuarioRol> usuarioRoles = new ArrayList<>();
    private List<Usuario> usuarios = new ArrayList<>();

    private long totalRolesActivos;
    private long totalPermisos;
    private long totalModulos;
    private long totalAsignaciones;

    private Rol rolSeleccionado = new Rol();

    private Long rolPermisosId;
    private Long[] permisoIdsSeleccionados = new Long[0];

    private Long usuarioAsignacionId;
    private Long rolAsignacionUsuarioId;

    private int paginaRoles = 1;
    private final int tamanioPaginaRoles = 5;
    private long totalRoles;

    private boolean mostrarModalRol;

    @PostConstruct
    public void init() {
        rolPermisoDAO = new RolPermisoDAO();
        usuarioDAO = new UsuarioDAO();
        cargarDatos();
    }

    public void cargarDatos() {
        cargarRolesPaginados();

        rolesTodos = rolPermisoDAO.listarRoles();
        permisos = rolPermisoDAO.listarPermisos();
        asignaciones = rolPermisoDAO.listarRolPermisos();
        usuarioRoles = rolPermisoDAO.listarUsuarioRoles();
        usuarios = usuarioDAO.listarFiltrado(0, 1000, "todos", null);

        totalRolesActivos = rolPermisoDAO.contarRolesActivos();
        totalPermisos = rolPermisoDAO.contarPermisos();
        totalModulos = rolPermisoDAO.contarModulosConPermisos();
        totalAsignaciones = rolPermisoDAO.contarAsignacionesRolPermiso();

        if (rolPermisosId != null && !existeRolEnLista(rolPermisosId, rolesTodos)) {
            rolPermisosId = null;
            permisoIdsSeleccionados = new Long[0];
        }

        if (rolPermisosId == null && !rolesTodos.isEmpty()) {
            rolPermisosId = rolesTodos.get(0).getId();
            cargarPermisosDelRol();
        }
    }

    public void cargarRolesPaginados() {
        totalRoles = rolPermisoDAO.contarRoles();

        if (paginaRoles > getTotalPaginasRoles()) {
            paginaRoles = getTotalPaginasRoles();
        }

        int inicio = Math.max(0, (paginaRoles - 1) * tamanioPaginaRoles);
        roles = rolPermisoDAO.listarRolesPaginado(inicio, tamanioPaginaRoles);
    }

    public void cargarPermisosDelRol() {
        permisoIdsSeleccionados = rolPermisosId == null
                ? new Long[0]
                : rolPermisoDAO.listarPermisoIdsPorRol(rolPermisosId).toArray(new Long[0]);
    }

    public void guardarPermisosRol() {
        if (rolPermisosId == null) {
            mensajeError("Seleccione un rol", "Debe elegir un rol para asignar permisos.");
            return;
        }

        rolPermisoDAO.guardarPermisosRol(
                rolPermisosId,
                permisoIdsSeleccionados == null
                        ? new ArrayList<>()
                        : Arrays.asList(permisoIdsSeleccionados)
        );

        cargarDatos();
        mensajeInfo("Permisos guardados", "Los permisos del rol se actualizaron correctamente.");
    }

    public void asignarRolUsuario() {
        if (usuarioAsignacionId == null || rolAsignacionUsuarioId == null) {
            mensajeError("Datos requeridos", "Seleccione un usuario y un rol.");
            return;
        }

        rolPermisoDAO.asignarRolAUsuario(usuarioAsignacionId, rolAsignacionUsuarioId);
        cargarDatos();

        usuarioAsignacionId = null;
        rolAsignacionUsuarioId = null;

        mensajeInfo("Rol asignado", "El rol fue asignado al usuario correctamente.");
    }

    public String obtenerRolesUsuario(Long usuarioId) {
        List<Rol> rolesUsuario = rolPermisoDAO.listarRolesPorUsuario(usuarioId);

        if (rolesUsuario.isEmpty()) {
            return "Sin rol asignado";
        }

        return rolesUsuario.stream()
                .map(Rol::getNombre)
                .collect(Collectors.joining(", "));
    }

    public void paginaAnteriorRoles() {
        if (paginaRoles > 1) {
            paginaRoles--;
            cargarRolesPaginados();
        }
    }

    public void paginaSiguienteRoles() {
        if (paginaRoles < getTotalPaginasRoles()) {
            paginaRoles++;
            cargarRolesPaginados();
        }
    }

    public void irPaginaRoles(int pagina) {
        if (pagina >= 1 && pagina <= getTotalPaginasRoles()) {
            paginaRoles = pagina;
            cargarRolesPaginados();
        }
    }

    public int getTotalPaginasRoles() {
        return Math.max(1, (int) Math.ceil((double) totalRoles / tamanioPaginaRoles));
    }

    public List<Integer> getPaginasRoles() {
        List<Integer> paginas = new ArrayList<>();

        for (int i = 1; i <= getTotalPaginasRoles(); i++) {
            paginas.add(i);
        }

        return paginas;
    }

    public void nuevoRol() {
        rolSeleccionado = new Rol();
        rolSeleccionado.setActivo(true);
        mostrarModalRol = true;
    }

    public void editarRol(Long id) {
        Rol rol = rolPermisoDAO.buscarRolPorId(id);

        if (rol != null) {
            rolSeleccionado = rol;
            mostrarModalRol = true;
        } else {
            mensajeError("Error", "No se encontró el rol seleccionado.");
        }
    }

    public void guardarRol() {
        if (rolSeleccionado.getNombre() == null || rolSeleccionado.getNombre().isBlank()) {
            mensajeError("Dato requerido", "Ingrese el nombre del rol.");
            mostrarModalRol = true;
            return;
        }

        if (rolSeleccionado.getCodigo() == null || rolSeleccionado.getCodigo().isBlank()) {
            rolSeleccionado.setCodigo(CodigoUtil.normalizarCodigo(rolSeleccionado.getNombre()));
        }

        try {
            rolPermisoDAO.guardarRol(rolSeleccionado);
            cargarDatos();

            mostrarModalRol = false;

            mensajeInfo("Correcto", "Rol guardado correctamente.");

        } catch (Exception e) {
            mensajeError("Error", extraerMensajeError(e));
            mostrarModalRol = true;
        }
    }

    public void eliminarRol(Long rolId) {
        try {
            eliminarRolConRelaciones(rolId);

            if (rolPermisosId != null && rolPermisosId.equals(rolId)) {
                rolPermisosId = null;
                permisoIdsSeleccionados = new Long[0];
            }

            if (rolSeleccionado != null
                    && rolSeleccionado.getId() != null
                    && rolSeleccionado.getId().equals(rolId)) {
                rolSeleccionado = new Rol();
            }

            cargarDatos();

            mostrarModalRol = false;

            mensajeInfo(
                    "Rol eliminado",
                    "El rol y sus relaciones con permisos y usuarios fueron eliminados correctamente."
            );

        } catch (Exception e) {
            mensajeError(
                    "No se pudo eliminar el rol",
                    extraerMensajeError(e)
            );
        }
    }

    private void eliminarRolConRelaciones(Long rolId) {
        if (rolId == null) {
            throw new IllegalArgumentException("Seleccione un rol válido para eliminar.");
        }

        EntityManager em = JPAUtil.getEntityManager();

        try {
            em.getTransaction().begin();

            Object codigoResultado = em.createNativeQuery(
                    "SELECT codigo FROM roles WHERE id = :rolId"
            )
                    .setParameter("rolId", rolId)
                    .getSingleResult();

            String codigoRol = codigoResultado == null
                    ? ""
                    : codigoResultado.toString();

            if ("ADMIN".equalsIgnoreCase(codigoRol)
                    || "ADMINISTRADOR".equalsIgnoreCase(codigoRol)) {
                throw new IllegalStateException("No se puede eliminar el rol administrador por seguridad del sistema.");
            }

            em.createNativeQuery("DELETE FROM rol_permisos WHERE rol_id = :rolId")
                    .setParameter("rolId", rolId)
                    .executeUpdate();

            em.createNativeQuery("DELETE FROM usuario_roles WHERE rol_id = :rolId")
                    .setParameter("rolId", rolId)
                    .executeUpdate();

            int rolesEliminados = em.createNativeQuery("DELETE FROM roles WHERE id = :rolId")
                    .setParameter("rolId", rolId)
                    .executeUpdate();

            if (rolesEliminados == 0) {
                throw new IllegalArgumentException("No se encontró el rol seleccionado.");
            }

            em.getTransaction().commit();

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            throw e;

        } finally {
            em.close();
        }
    }

    private boolean existeRolEnLista(Long rolId, List<Rol> listaRoles) {
        if (rolId == null || listaRoles == null || listaRoles.isEmpty()) {
            return false;
        }

        return listaRoles.stream()
                .anyMatch(rol -> rol.getId() != null && rol.getId().equals(rolId));
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

        if (mensaje == null || mensaje.isBlank()) {
            return "Revise si el rol tiene relaciones activas o si ya fue eliminado.";
        }

        if (mensaje.toLowerCase().contains("duplicate")) {
            return "Ya existe un rol con esos datos.";
        }

        if (mensaje.toLowerCase().contains("constraint")) {
            return "No se pudo completar la operación por una restricción de datos.";
        }

        if (mensaje.toLowerCase().contains("no entity")) {
            return "No se encontró el rol seleccionado.";
        }

        return mensaje;
    }

    private void mensajeInfo(String resumen, String detalle) {
        FacesContext.getCurrentInstance().addMessage(
                null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, resumen, detalle)
        );
    }

    private void mensajeError(String resumen, String detalle) {
        FacesContext.getCurrentInstance().addMessage(
                null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, resumen, detalle)
        );
    }

    public List<Rol> getRoles() {
        return roles;
    }

    public List<Rol> getRolesTodos() {
        return rolesTodos;
    }

    public List<Permiso> getPermisos() {
        return permisos;
    }

    public List<RolPermiso> getAsignaciones() {
        return asignaciones;
    }

    public List<UsuarioRol> getUsuarioRoles() {
        return usuarioRoles;
    }

    public List<Usuario> getUsuarios() {
        return usuarios;
    }

    public long getTotalRolesActivos() {
        return totalRolesActivos;
    }

    public long getTotalPermisos() {
        return totalPermisos;
    }

    public long getTotalModulos() {
        return totalModulos;
    }

    public long getTotalAsignaciones() {
        return totalAsignaciones;
    }

    public Rol getRolSeleccionado() {
        return rolSeleccionado;
    }

    public void setRolSeleccionado(Rol rolSeleccionado) {
        this.rolSeleccionado = rolSeleccionado;
    }

    public Long getRolPermisosId() {
        return rolPermisosId;
    }

    public void setRolPermisosId(Long rolPermisosId) {
        this.rolPermisosId = rolPermisosId;
    }

    public Long[] getPermisoIdsSeleccionados() {
        return permisoIdsSeleccionados;
    }

    public void setPermisoIdsSeleccionados(Long[] permisoIdsSeleccionados) {
        this.permisoIdsSeleccionados = permisoIdsSeleccionados;
    }

    public Long getUsuarioAsignacionId() {
        return usuarioAsignacionId;
    }

    public void setUsuarioAsignacionId(Long usuarioAsignacionId) {
        this.usuarioAsignacionId = usuarioAsignacionId;
    }

    public Long getRolAsignacionUsuarioId() {
        return rolAsignacionUsuarioId;
    }

    public void setRolAsignacionUsuarioId(Long rolAsignacionUsuarioId) {
        this.rolAsignacionUsuarioId = rolAsignacionUsuarioId;
    }

    public int getPaginaRoles() {
        return paginaRoles;
    }

    public int getTamanioPaginaRoles() {
        return tamanioPaginaRoles;
    }

    public long getTotalRoles() {
        return totalRoles;
    }

    public boolean isMostrarModalRol() {
        return mostrarModalRol;
    }

    public boolean getMostrarModalRol() {
        return mostrarModalRol;
    }
}