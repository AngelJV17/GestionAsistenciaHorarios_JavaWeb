package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.EmpleadoDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import com.hospital.gestionasistenciashorarioshospital.modelo.Rol;
import com.hospital.gestionasistenciashorarioshospital.modelo.Usuario;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@RequestScoped
public class UsuarioActualController implements Serializable {

    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private Empleado empleado;
    private boolean empleadoCargado;

    public String getNombreMostrado() {
        Empleado empleadoActual = getEmpleadoActual();
        if (empleadoActual != null && empleadoActual.getNombreCompleto() != null
                && !empleadoActual.getNombreCompleto().isBlank()) {
            return empleadoActual.getNombreCompleto();
        }

        Usuario usuario = getUsuarioSesion();
        return usuario == null || usuario.getNombreUsuario() == null
                ? "Usuario"
                : usuario.getNombreUsuario();
    }

    public String getRolMostrado() {
        Empleado empleadoActual = getEmpleadoActual();
        if (empleadoActual != null) {
            if (empleadoActual.getEspecialidad() != null && empleadoActual.getEspecialidad().getNombre() != null) {
                return empleadoActual.getEspecialidad().getNombre();
            }
            if (empleadoActual.getCargo() != null && empleadoActual.getCargo().getNombre() != null) {
                return empleadoActual.getCargo().getNombre();
            }
        }

        List<Rol> roles = getRolesSesion();
        if (!roles.isEmpty() && roles.get(0).getNombre() != null) {
            return roles.get(0).getNombre();
        }

        return Boolean.TRUE.equals(getSessionValue("esAdmin")) ? "Administrador" : "Usuario";
    }

    public String getIniciales() {
        String nombre = getNombreMostrado();
        if (nombre == null || nombre.isBlank()) {
            return "US";
        }

        String[] partes = nombre.trim().split("\\s+");
        String primera = partes[0].substring(0, 1);
        String segunda = partes.length > 1 ? partes[1].substring(0, 1) : "";
        return (primera + segunda).toUpperCase();
    }

    public String getInicioOutcome() {
        return Boolean.TRUE.equals(getSessionValue("esAdmin"))
                ? "/views/admin/dashboard.xhtml"
                : "/views/medico/inicio.xhtml";
    }

    public String getPerfilOutcome() {
        return Boolean.TRUE.equals(getSessionValue("esAdmin"))
                ? "/views/admin/dashboard.xhtml"
                : "/views/medico/configuraciones.xhtml";
    }

    private Empleado getEmpleadoActual() {
        if (empleadoCargado) {
            return empleado;
        }

        empleadoCargado = true;
        Usuario usuario = getUsuarioSesion();
        if (usuario != null && usuario.getId() != null) {
            empleado = empleadoDAO.buscarPorUsuarioId(usuario.getId());
        }
        return empleado;
    }

    private Usuario getUsuarioSesion() {
        Object usuarioSesion = getSessionValue("usuarioLogueado");
        return usuarioSesion instanceof Usuario ? (Usuario) usuarioSesion : null;
    }

    private List<Rol> getRolesSesion() {
        Object rolesSesion = getSessionValue("rolesUsuario");
        if (!(rolesSesion instanceof List<?>)) {
            return new ArrayList<>();
        }

        List<Rol> roles = new ArrayList<>();
        for (Object item : (List<?>) rolesSesion) {
            if (item instanceof Rol) {
                roles.add((Rol) item);
            }
        }
        return roles;
    }

    private Object getSessionValue(String key) {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        return externalContext.getSessionMap().get(key);
    }
}
