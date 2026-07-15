/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.UsuarioDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.RolPermisoDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Rol;
import com.hospital.gestionasistenciashorarioshospital.modelo.Usuario;
import com.hospital.gestionasistenciashorarioshospital.util.PasswordUtil;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("loginController")
@SessionScoped
public class LoginController implements Serializable {

    private String username;
    private String password;
    private boolean rememberMe;

    private Usuario usuarioLogueado;
    private List<Rol> rolesUsuario = new ArrayList<>();
    private boolean admin;

    public String autenticar() {
        UsuarioDAO usuarioDAO = new UsuarioDAO();

        Usuario usuario = usuarioDAO.buscarPorNombreUsuario(username);

        if (usuario == null || !PasswordUtil.verifyPassword(password, usuario.getPasswordHash())) {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            "Error",
                            "Usuario o contraseña incorrectos"
                    )
            );

            password = null;
            return null;
        }

        usuarioDAO.actualizarUltimoAcceso(usuario.getId());

        RolPermisoDAO rolPermisoDAO = new RolPermisoDAO();
        try {
            rolesUsuario = rolPermisoDAO.listarRolesPorUsuario(usuario.getId());

            if ("admin".equalsIgnoreCase(usuario.getNombreUsuario()) && !contieneRol(rolesUsuario, "ADMIN")) {
                rolPermisoDAO.asignarRolAUsuarioSiNoExiste(usuario.getId(), "ADMIN");
                rolPermisoDAO.asignarTodosLosPermisosARolSiNoExisten("ADMIN");
                rolesUsuario = rolPermisoDAO.listarRolesPorUsuario(usuario.getId());
            }

            admin = tieneRol("ADMIN") || tieneRol("RRHH") || tieneRol("JEFE_AREA");
        } catch (RuntimeException e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            "Error",
                            "No se pudieron cargar los roles del usuario."
                    )
            );
            password = null;
            return null;
        }

        usuarioLogueado = usuario;

        HttpSession session = obtenerSesion(true);
        session.setAttribute("usuarioLogueado", usuario);
        session.setAttribute("rolesUsuario", rolesUsuario);
        session.setAttribute("esAdmin", admin);

        password = null;

        return admin
                ? "/views/admin/dashboard.xhtml?faces-redirect=true"
                : "/views/medico/inicio.xhtml?faces-redirect=true";
    }

    public void verificarSesion() throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        HttpSession session = obtenerSesion(false);

        boolean logueado = session != null
                && session.getAttribute("usuarioLogueado") != null;

        if (logueado) {
            Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
            String destino = Boolean.TRUE.equals(esAdmin)
                    ? "/views/admin/dashboard.xhtml"
                    : "/views/medico/inicio.xhtml";

            facesContext.getExternalContext().redirect(
                    facesContext.getExternalContext().getRequestContextPath()
                    + destino
            );
            facesContext.responseComplete();
        }
    }

    public String cerrarSesion() {
        usuarioLogueado = null;
        rolesUsuario = new ArrayList<>();
        admin = false;
        username = null;
        password = null;
        rememberMe = false;

        FacesContext facesContext = FacesContext.getCurrentInstance();
        try {
            facesContext.getExternalContext().invalidateSession();
            facesContext.getExternalContext().redirect(
                    facesContext.getExternalContext().getRequestContextPath()
                    + "/views/login/login.xhtml"
            );
            facesContext.responseComplete();
        } catch (IOException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "No se pudo cerrar sesión", e.getMessage()));
        }
        return null;
    }

    public boolean isLogueado() {
        return usuarioLogueado != null;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean getAdmin() {
        return admin;
    }

    public boolean isMedico() {
        return !admin;
    }

    public boolean getMedico() {
        return !admin;
    }

    public boolean tieneRol(String codigoRol) {
        if (codigoRol == null || rolesUsuario == null) {
            return false;
        }

        return contieneRol(rolesUsuario, codigoRol);
    }

    private boolean contieneRol(List<Rol> roles, String codigoRol) {
        if (codigoRol == null || roles == null) {
            return false;
        }

        return roles.stream()
                .anyMatch(rol -> rol.getCodigo() != null
                && rol.getCodigo().equalsIgnoreCase(codigoRol));
    }

    private HttpSession obtenerSesion(boolean crear) {
        return (HttpSession) FacesContext
                .getCurrentInstance()
                .getExternalContext()
                .getSession(crear);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public Usuario getUsuarioLogueado() {
        return usuarioLogueado;
    }

    public void setUsuarioLogueado(Usuario usuarioLogueado) {
        this.usuarioLogueado = usuarioLogueado;
    }

    public List<Rol> getRolesUsuario() {
        return rolesUsuario;
    }

}
