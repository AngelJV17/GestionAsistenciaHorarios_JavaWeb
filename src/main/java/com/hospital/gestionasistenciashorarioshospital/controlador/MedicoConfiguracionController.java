package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.EmpleadoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.UsuarioDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import com.hospital.gestionasistenciashorarioshospital.modelo.Usuario;
import com.hospital.gestionasistenciashorarioshospital.util.PasswordUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@ViewScoped
public class MedicoConfiguracionController implements Serializable {

    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    private Empleado empleado;
    private Usuario usuario;
    private String passwordActual;
    private String passwordNueva;
    private String passwordConfirmacion;

    @PostConstruct
    public void init() {
        usuario = getUsuarioSesion();
        if (usuario != null) {
            empleado = empleadoDAO.buscarPorUsuarioId(usuario.getId());
            usuario = usuarioDAO.buscarPorId(usuario.getId());
        }
    }

    public void guardarPerfil() {
        if (empleado == null) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "Perfil no disponible",
                    "No se encontro un empleado vinculado al usuario actual.");
            return;
        }

        normalizarPerfil();
        empleadoDAO.guardar(empleado);
        agregarMensaje(FacesMessage.SEVERITY_INFO, "Perfil actualizado",
                "Los datos profesionales se guardaron correctamente.");
    }

    public void guardarCuenta() {
        if (usuario == null) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "Cuenta no disponible",
                    "No se encontro la cuenta del usuario actual.");
            return;
        }

        if (usuario.getCorreoRecuperacion() != null) {
            usuario.setCorreoRecuperacion(usuario.getCorreoRecuperacion().trim().toLowerCase());
        }

        usuarioDAO.guardar(usuario);
        agregarMensaje(FacesMessage.SEVERITY_INFO, "Cuenta actualizada",
                "La información de recuperación fue guardada.");
    }

    public void cambiarPassword() {
        if (usuario == null) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "Cuenta no disponible",
                    "No se encontro la cuenta del usuario actual.");
            return;
        }

        if (passwordActual == null || passwordActual.isBlank()
                || passwordNueva == null || passwordNueva.isBlank()
                || passwordConfirmacion == null || passwordConfirmacion.isBlank()) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "Datos incompletos",
                    "Complete la contraseña actual, la nueva y su confirmación.");
            return;
        }

        if (!PasswordUtil.verifyPassword(passwordActual, usuario.getPasswordHash())) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "Contraseña incorrecta",
                    "La contraseña actual no coincide con la registrada.");
            return;
        }

        if (passwordNueva.length() < 6) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "Contraseña inválida",
                    "La nueva contraseña debe tener al menos 6 caracteres.");
            return;
        }

        if (!passwordNueva.equals(passwordConfirmacion)) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "Confirmacion inválida",
                    "La confirmación no coincide con la nueva contraseña.");
            return;
        }

        usuario.setPasswordHash(PasswordUtil.hashPassword(passwordNueva));
        usuarioDAO.guardar(usuario);
        limpiarPassword();
        agregarMensaje(FacesMessage.SEVERITY_INFO, "Contraseña actualizada",
                "Use la nueva contraseña en su próximo inicio de sesión.");
    }

    public String getIniciales() {
        if (empleado == null || empleado.getNombreCompleto() == null || empleado.getNombreCompleto().isBlank()) {
            return "MD";
        }
        String[] partes = empleado.getNombreCompleto().trim().split("\\s+");
        String primera = partes[0].substring(0, 1);
        String segunda = partes.length > 1 ? partes[1].substring(0, 1) : "";
        return (primera + segunda).toUpperCase();
    }

    public String getResumenProfesional() {
        if (empleado == null) {
            return "Perfil médico";
        }
        String cargo = empleado.getCargo() == null ? "Medico" : empleado.getCargo().getNombre();
        String area = empleado.getArea() == null ? "Hospital" : empleado.getArea().getNombre();
        return cargo + " - " + area;
    }

    private void normalizarPerfil() {
        empleado.setNombres(limpiar(empleado.getNombres()));
        empleado.setApellidoPaterno(limpiar(empleado.getApellidoPaterno()));
        empleado.setApellidoMaterno(limpiar(empleado.getApellidoMaterno()));
        empleado.setCorreo(limpiarMinuscula(empleado.getCorreo()));
        empleado.setTelefono(limpiar(empleado.getTelefono()));
        empleado.setDireccion(limpiar(empleado.getDireccion()));
        empleado.setBiografia(limpiar(empleado.getBiografia()));
        empleado.setNumeroColegiatura(limpiar(empleado.getNumeroColegiatura()));
    }

    private String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String limpiarMinuscula(String valor) {
        return valor == null ? null : valor.trim().toLowerCase();
    }

    private Usuario getUsuarioSesion() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        Object usuarioSesion = externalContext.getSessionMap().get("usuarioLogueado");
        return usuarioSesion instanceof Usuario ? (Usuario) usuarioSesion : null;
    }

    private void limpiarPassword() {
        passwordActual = null;
        passwordNueva = null;
        passwordConfirmacion = null;
    }

    private void agregarMensaje(FacesMessage.Severity severity, String resumen, String detalle) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, resumen, detalle));
    }

    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getPasswordActual() { return passwordActual; }
    public void setPasswordActual(String passwordActual) { this.passwordActual = passwordActual; }
    public String getPasswordNueva() { return passwordNueva; }
    public void setPasswordNueva(String passwordNueva) { this.passwordNueva = passwordNueva; }
    public String getPasswordConfirmacion() { return passwordConfirmacion; }
    public void setPasswordConfirmacion(String passwordConfirmacion) { this.passwordConfirmacion = passwordConfirmacion; }
}
