package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.RolPermisoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.UsuarioDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Rol;
import com.hospital.gestionasistenciashorarioshospital.modelo.Usuario;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;

@Named
@ViewScoped
public class UsuariosController implements Serializable {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final RolPermisoDAO rolPermisoDAO = new RolPermisoDAO();
    private List<Usuario> usuarios = new ArrayList<>();
    private List<Rol> roles = new ArrayList<>();
    private Usuario usuarioSeleccionado = new Usuario();
    private String passwordTemporal;
    private String codigoRolSeleccionado;
    private String filtroEstado = "todos";
    private String busqueda;
    private int pagina = 1;
    private final int tamanioPagina = 5;
    private long totalUsuarios;
    private boolean mostrarModal;

    @PostConstruct
    public void init() {
        roles = rolPermisoDAO.listarRoles();
        cargarUsuarios();
        nuevoUsuario(false);
    }

    public void cargarUsuarios() {
        totalUsuarios = usuarioDAO.contarFiltrado(filtroEstado, busqueda);
        if (pagina > getTotalPaginas()) {
            pagina = getTotalPaginas();
        }
        usuarios = usuarioDAO.listarFiltrado(Math.max(0, (pagina - 1) * tamanioPagina), tamanioPagina, filtroEstado, busqueda);
    }

    public void filtrar() {
        pagina = 1;
        cargarUsuarios();
    }

    public void nuevoUsuario() {
        nuevoUsuario(true);
    }

    private void nuevoUsuario(boolean abrirModal) {
        usuarioSeleccionado = new Usuario();
        usuarioSeleccionado.setEstadoId(1L);
        passwordTemporal = "";
        codigoRolSeleccionado = "";
        mostrarModal = abrirModal;
    }

    public void editarUsuario(Long id) {
        usuarioSeleccionado = usuarioDAO.buscarPorId(id);
        passwordTemporal = "";
        codigoRolSeleccionado = "";
        mostrarModal = true;
    }

    public void guardarUsuario() {
        if (!validarUsuario()) {
            return;
        }

        if (passwordTemporal != null && !passwordTemporal.isBlank()) {
            usuarioSeleccionado.setPasswordHash(BCrypt.hashpw(passwordTemporal, BCrypt.gensalt()));
        }

        usuarioDAO.guardar(usuarioSeleccionado);

        if (codigoRolSeleccionado != null && !codigoRolSeleccionado.isBlank()) {
            Usuario usuarioGuardado = usuarioDAO.buscarPorNombreUsuario(usuarioSeleccionado.getNombreUsuario());
            if (usuarioGuardado != null) {
                rolPermisoDAO.asignarRolAUsuarioSiNoExiste(usuarioGuardado.getId(), codigoRolSeleccionado);
            }
        }

        cargarUsuarios();
        nuevoUsuario(false);
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Usuario guardado", "Los datos se guardaron correctamente."));
    }

    private boolean validarUsuario() {
        if (usuarioSeleccionado.getNombreUsuario() == null || usuarioSeleccionado.getNombreUsuario().isBlank()) {
            agregarError("Usuario requerido", "Ingrese un nombre de usuario.");
            return false;
        }

        usuarioSeleccionado.setNombreUsuario(usuarioSeleccionado.getNombreUsuario().trim().toLowerCase());

        if (usuarioSeleccionado.getNombreUsuario().length() < 4) {
            agregarError("Usuario inválido", "El usuario debe tener al menos 4 caracteres.");
            return false;
        }

        if (usuarioSeleccionado.getId() == null && usuarioDAO.existeNombreUsuario(usuarioSeleccionado.getNombreUsuario())) {
            agregarError("Usuario duplicado", "Ya existe una cuenta con ese usuario.");
            return false;
        }

        if (usuarioSeleccionado.getId() == null && (passwordTemporal == null || passwordTemporal.isBlank())) {
            agregarError("Contraseña requerida", "Ingrese una contraseña para el nuevo usuario.");
            return false;
        }

        if (passwordTemporal != null && !passwordTemporal.isBlank() && passwordTemporal.length() < 6) {
            agregarError("Contraseña inválida", "La contraseña debe tener al menos 6 caracteres.");
            return false;
        }

        mostrarModal = true;
        return true;
    }

    private void agregarError(String resumen, String detalle) {
        mostrarModal = true;
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, resumen, detalle));
    }

    public void cambiarEstado(Long id) {
        Usuario usuario = usuarioDAO.buscarPorId(id);
        if (usuario != null) {
            usuario.setEstadoId(usuario.getEstadoId() != null && usuario.getEstadoId().equals(1L) ? 2L : 1L);
            usuarioDAO.guardar(usuario);
            cargarUsuarios();
        }
    }

    public String obtenerRolesUsuario(Long usuarioId) {
        List<Rol> rolesUsuario = rolPermisoDAO.listarRolesPorUsuario(usuarioId);
        if (rolesUsuario.isEmpty()) {
            return "Sin rol asignado";
        }
        StringBuilder rolesTexto = new StringBuilder();
        for (Rol rol : rolesUsuario) {
            if (rolesTexto.length() > 0) {
                rolesTexto.append(", ");
            }
            rolesTexto.append(rol.getNombre());
        }
        return rolesTexto.toString();
    }

    public void paginaAnterior() {
        if (pagina > 1) {
            pagina--;
            cargarUsuarios();
        }
    }

    public void paginaSiguiente() {
        if (pagina < getTotalPaginas()) {
            pagina++;
            cargarUsuarios();
        }
    }

    public void irPagina(int pagina) {
        if (pagina >= 1 && pagina <= getTotalPaginas()) {
            this.pagina = pagina;
            cargarUsuarios();
        }
    }

    public int getTotalPaginas() {
        return Math.max(1, (int) Math.ceil((double) totalUsuarios / tamanioPagina));
    }

    public List<Integer> getPaginas() {
        List<Integer> paginas = new ArrayList<>();
        for (int i = 1; i <= getTotalPaginas(); i++) {
            paginas.add(i);
        }
        return paginas;
    }

    public List<Usuario> getUsuarios() { return usuarios; }
    public List<Rol> getRoles() { return roles; }
    public Usuario getUsuarioSeleccionado() { return usuarioSeleccionado; }
    public void setUsuarioSeleccionado(Usuario usuarioSeleccionado) { this.usuarioSeleccionado = usuarioSeleccionado; }
    public String getPasswordTemporal() { return passwordTemporal; }
    public void setPasswordTemporal(String passwordTemporal) { this.passwordTemporal = passwordTemporal; }
    public String getCodigoRolSeleccionado() { return codigoRolSeleccionado; }
    public void setCodigoRolSeleccionado(String codigoRolSeleccionado) { this.codigoRolSeleccionado = codigoRolSeleccionado; }
    public String getFiltroEstado() { return filtroEstado; }
    public void setFiltroEstado(String filtroEstado) { this.filtroEstado = filtroEstado; }
    public String getBusqueda() { return busqueda; }
    public void setBusqueda(String busqueda) { this.busqueda = busqueda; }
    public int getPagina() { return pagina; }
    public long getTotalUsuarios() { return totalUsuarios; }
    public boolean getMostrarModal() { return mostrarModal; }
}
