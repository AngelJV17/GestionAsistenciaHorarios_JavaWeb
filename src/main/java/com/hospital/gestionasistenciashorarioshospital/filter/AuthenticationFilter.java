package com.hospital.gestionasistenciashorarioshospital.filter;

import com.hospital.gestionasistenciashorarioshospital.dao.RolPermisoDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Rol;
import com.hospital.gestionasistenciashorarioshospital.modelo.Usuario;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebFilter("/views/*")
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI()
                .substring(req.getContextPath().length());

        boolean esLogin = path.startsWith("/views/login/");
        boolean esRecursoJSF = path.contains("/jakarta.faces.resource/");
        boolean esRecursoPublico = path.startsWith("/resources/");

        if (esLogin || esRecursoJSF || esRecursoPublico) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);

        boolean logueado = session != null
                && session.getAttribute("usuarioLogueado") != null;

        if (!logueado) {
            res.sendRedirect(req.getContextPath() + "/views/login/login.xhtml");
            return;
        }

        boolean rutaAdmin = path.startsWith("/views/admin/");
        boolean esAdmin = resolverEsAdmin(session);

        if (rutaAdmin && !esAdmin) {
            res.sendRedirect(req.getContextPath() + "/views/medico/inicio.xhtml");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean resolverEsAdmin(HttpSession session) {
        Object usuarioSesion = session.getAttribute("usuarioLogueado");

        if (!(usuarioSesion instanceof Usuario)) {
            session.setAttribute("esAdmin", false);
            return false;
        }

        Usuario usuario = (Usuario) usuarioSesion;
        RolPermisoDAO rolPermisoDAO = new RolPermisoDAO();
        List<Rol> roles = rolPermisoDAO.listarRolesPorUsuario(usuario.getId());

        boolean esAdmin = roles.stream()
                .anyMatch(rol -> rol.getCodigo() != null
                && ("ADMIN".equalsIgnoreCase(rol.getCodigo())
                || "RRHH".equalsIgnoreCase(rol.getCodigo())
                || "JEFE_AREA".equalsIgnoreCase(rol.getCodigo())));

        session.setAttribute("rolesUsuario", roles);
        session.setAttribute("esAdmin", esAdmin);

        return esAdmin;
    }
}
