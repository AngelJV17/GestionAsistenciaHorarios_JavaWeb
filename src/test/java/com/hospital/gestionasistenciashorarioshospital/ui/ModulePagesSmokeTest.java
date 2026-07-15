package com.hospital.gestionasistenciashorarioshospital.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ModulePagesSmokeTest {

    private static final List<String> ADMIN_PAGES = List.of(
            "auditoria", "catalogos", "correcciones", "dashboard", "documentos",
            "empleados", "feriados", "horarios", "reportes", "roles", "sedes", "solicitudes",
            "turnos", "usuarios", "variables");

    @Test
    void existenTodasLasPaginasAdminPrincipales() {
        for (String page : ADMIN_PAGES) {
            assertTrue(Files.exists(Path.of("src/main/webapp/views/admin/" + page + ".xhtml")),
                    "Debe existir la pagina admin " + page);
        }
    }

    @Test
    void documentosAdminTieneFormularioAgrupadoSubidaEdicionYEliminacion() throws Exception {
        String page = Files.readString(Path.of("src/main/webapp/views/admin/documentos.xhtml"));

        assertTrue(page.contains("enctype=\"multipart/form-data\""));
        assertTrue(page.contains("adminDocumentosController.guardarDocumento"));
        assertTrue(page.contains("document-upload-group"));
        assertTrue(page.contains("Propietario del documento"));
        assertTrue(page.contains("Clasificación y vigencia"));
        assertTrue(page.contains("Archivo y detalle"));
        assertTrue(page.contains("adminDocumentosController.editarDocumento"));
        assertTrue(page.contains("adminDocumentosController.eliminarDocumento"));
        assertTrue(page.contains("adminDocumentosController.cancelarEdicion"));
    }

    @Test
    void documentosMedicoFiltraPorEmpleadoYPermiteDescarga() throws Exception {
        String controller = Files.readString(Path.of(
                "src/main/java/com/hospital/gestionasistenciashorarioshospital/controlador/MedicoDocumentosController.java"));
        String page = Files.readString(Path.of("src/main/webapp/views/medico/documentos.xhtml"));

        assertTrue(controller.contains("empleadoDAO.buscarPorUsuarioId"));
        assertTrue(controller.contains("documentoDAO.listarPorEmpleado"));
        assertTrue(controller.contains("Files.copy"));
        assertTrue(page.contains("medicoDocumentosController.descargar"));
    }

    @Test
    void configuracionMedicoTienePerfilCuentaYSeguridad() throws Exception {
        String page = Files.readString(Path.of("src/main/webapp/views/medico/configuraciones.xhtml"));
        String controller = Files.readString(Path.of(
                "src/main/java/com/hospital/gestionasistenciashorarioshospital/controlador/MedicoConfiguracionController.java"));

        assertTrue(page.contains("profile-section"));
        assertTrue(page.contains("account-section"));
        assertTrue(page.contains("security-section"));
        assertTrue(controller.contains("guardarPerfil"));
        assertTrue(controller.contains("guardarCuenta"));
        assertTrue(controller.contains("cambiarPassword"));
    }

    @Test
    void solicitudesMedicoUsaControllerRealYVistaOrdenada() throws Exception {
        String page = Files.readString(Path.of("src/main/webapp/views/medico/solicitudes.xhtml"));
        String controller = Files.readString(Path.of(
                "src/main/java/com/hospital/gestionasistenciashorarioshospital/controlador/MedicoSolicitudesController.java"));
        String dao = Files.readString(Path.of(
                "src/main/java/com/hospital/gestionasistenciashorarioshospital/dao/SolicitudDAO.java"));

        assertTrue(page.contains("medicoSolicitudesController.registrarSolicitud"));
        assertTrue(page.contains("medicoSolicitudesController.solicitudes"));
        assertTrue(page.contains("enctype=\"multipart/form-data\""));
        assertTrue(page.contains("Adjunto opcional"));
        assertTrue(page.contains("medical-workspace"));
        assertTrue(page.contains("request-history-list"));
        assertTrue(controller.contains("empleadoDAO.buscarPorUsuarioId"));
        assertTrue(controller.contains("solicitudDAO.guardar"));
        assertTrue(controller.contains("setNombreArchivo"));
        assertTrue(dao.contains("public void guardar(Solicitud solicitud)"));
    }

    @Test
    void horariosYAsistenciasMedicoUsanDatosRealesYTablaUnificada() throws Exception {
        String horariosPage = Files.readString(Path.of("src/main/webapp/views/medico/horarios.xhtml"));
        String asistenciasPage = Files.readString(Path.of("src/main/webapp/views/medico/asistencias.xhtml"));
        String horariosController = Files.readString(Path.of(
                "src/main/java/com/hospital/gestionasistenciashorarioshospital/controlador/MedicoHorariosController.java"));
        String asistenciasController = Files.readString(Path.of(
                "src/main/java/com/hospital/gestionasistenciashorarioshospital/controlador/MedicoAsistenciasController.java"));

        assertTrue(horariosPage.contains("medicoHorariosController.horarioSemanal"));
        assertTrue(horariosPage.contains("weekly-schedule"));
        assertTrue(horariosPage.contains("schedule-dashboard"));
        assertTrue(horariosPage.contains("schedule-side-card"));
        assertTrue(horariosPage.contains("Resumen Mensual"));
        assertTrue(horariosPage.contains("medicoHorariosController.solicitarCambioTurno"));
        assertTrue(horariosPage.contains("exportarHorarioPdf"));
        assertTrue(horariosController.contains("horarioDAO.listarPorEmpleado"));
        assertTrue(horariosController.contains("horarioDAO.buscarHorarioActivo"));
        assertTrue(horariosController.contains("getHorarioSemanal"));
        assertTrue(horariosController.contains("getTurnosMananaMes"));

        assertTrue(asistenciasPage.contains("medicoAsistenciasController.asistencias"));
        assertTrue(asistenciasPage.contains("medicoAsistenciasController.registrarEntrada"));
        assertTrue(asistenciasPage.contains("medicoAsistenciasController.registrarSalida"));
        assertTrue(asistenciasPage.contains("attendanceCalendar"));
        assertTrue(asistenciasPage.contains("tabCalendario"));
        assertTrue(asistenciasPage.contains("system-table"));
        assertFalse(asistenciasPage.contains("attendance.js"));
        assertTrue(asistenciasController.contains("asistenciaDAO.listarPorMes"));
        assertTrue(asistenciasController.contains("marcacionDAO.guardar"));
    }

    @Test
    void adminHorariosAsignaTurnosAEmpleados() throws Exception {
        String page = Files.readString(Path.of("src/main/webapp/views/admin/horarios.xhtml"));
        String controller = Files.readString(Path.of(
                "src/main/java/com/hospital/gestionasistenciashorarioshospital/controlador/AdminHorariosController.java"));
        String dao = Files.readString(Path.of(
                "src/main/java/com/hospital/gestionasistenciashorarioshospital/dao/HorarioDAO.java"));
        String navbar = Files.readString(Path.of("src/main/webapp/WEB-INF/templates/navbar-admin.xhtml"));

        assertTrue(page.contains("adminHorariosController.guardarHorario"));
        assertTrue(page.contains("adminHorariosController.empleados"));
        assertTrue(page.contains("adminHorariosController.turnos"));
        assertTrue(page.contains("system-table"));
        assertTrue(controller.contains("empleadoDAO.listarFiltrado"));
        assertTrue(controller.contains("turnoDAO.listarFiltrado"));
        assertTrue(controller.contains("existeTurnoSolapado"));
        assertTrue(controller.contains("No se pudo eliminar el horario"));
        assertTrue(controller.contains("horarioDAO.guardar"));
        assertTrue(dao.contains("public void guardar(Horario horario)"));
        assertTrue(dao.contains("public boolean existeTurnoSolapado"));
        assertTrue(navbar.contains("/views/admin/horarios.xhtml"));
    }

    @Test
    void accionesDeTablasUsanIconosYClasesUnificadas() throws Exception {
        String adminHorarios = Files.readString(Path.of("src/main/webapp/views/admin/horarios.xhtml"));
        String adminDocumentos = Files.readString(Path.of("src/main/webapp/views/admin/documentos.xhtml"));

        assertTrue(adminHorarios.contains("table-actions"));
        assertTrue(adminHorarios.contains("table-action-btn table-action-btn--primary"));
        assertTrue(adminHorarios.contains("table-action-btn table-action-btn--danger"));
        assertTrue(adminHorarios.contains("fa-pen-to-square"));
        assertFalse(adminHorarios.contains("table-action-btn--edit"));
        assertFalse(adminHorarios.contains("table-action-btn--delete"));

        assertTrue(adminDocumentos.contains("table-actions"));
        assertTrue(adminDocumentos.contains("table-action-btn table-action-btn--primary"));
        assertTrue(adminDocumentos.contains("table-action-btn table-action-btn--danger"));
        assertFalse(adminDocumentos.contains("document-row-actions"));
        assertFalse(adminDocumentos.contains("document-action-btn--edit"));
    }

    @Test
    void headerOcultaPerfilParaAdminYColoresDeAsistenciaSonConsistentes() throws Exception {
        String header = Files.readString(Path.of("src/main/webapp/WEB-INF/templates/header.xhtml"));
        String inicioMedico = Files.readString(Path.of("src/main/webapp/views/medico/inicio.xhtml"));
        String style = Files.readString(Path.of("src/main/webapp/resources/css/style.css"));

        assertTrue(header.contains("rendered=\"#{sessionScope.esAdmin ne true}\""));
        assertTrue(inicioMedico.contains("stat-card stat-card-success\">\n                                                <div class=\"fs-5 fw-bold text-success\">#{inicioController.totalAsistencias}"));
        assertTrue(inicioMedico.contains("stat-card stat-card-warning"));
        assertTrue(inicioMedico.contains("stat-card stat-card-danger\">\n                                                <div class=\"fs-5 fw-bold text-danger\">#{inicioController.totalFaltas}"));
        assertTrue(style.contains(".stat-card-success .stat-value"));
        assertTrue(style.contains(".stat-card-warning .stat-value"));
        assertTrue(style.contains(".stat-card-danger .stat-value"));
    }

    @Test
    void solicitudesPermitenAbrirAdjuntosEnAdminYMedico() throws Exception {
        String adminPage = Files.readString(Path.of("src/main/webapp/views/admin/solicitudes.xhtml"));
        String medicoPage = Files.readString(Path.of("src/main/webapp/views/medico/solicitudes.xhtml"));
        String adminController = Files.readString(Path.of(
                "src/main/java/com/hospital/gestionasistenciashorarioshospital/controlador/SolicitudesController.java"));
        String medicoController = Files.readString(Path.of(
                "src/main/java/com/hospital/gestionasistenciashorarioshospital/controlador/MedicoSolicitudesController.java"));

        assertTrue(adminPage.contains("solicitudesController.abrirAdjunto"));
        assertTrue(medicoPage.contains("medicoSolicitudesController.abrirAdjunto"));
        assertTrue(adminPage.contains("abrirEnNuevaPestana"));
        assertTrue(medicoPage.contains("abrirEnNuevaPestana"));
        assertTrue(adminController.contains("inline; filename="));
        assertTrue(medicoController.contains("Acceso denegado"));
        assertTrue(medicoController.contains("inline; filename="));
    }

    @Test
    void tablasBootstrapUsanPatronVisualUnificado() throws Exception {
        List<Path> pages = Files.walk(Path.of("src/main/webapp"))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".xhtml"))
                .collect(Collectors.toList());

        for (Path page : pages) {
            String content = Files.readString(page);
            if (content.contains("table table-hover") && !content.contains("system-table")) {
                throw new AssertionError("La tabla debe usar system-table: " + page);
            }
        }
    }

    @Test
    void listasDeEmpleadosNoUsanDaoGenerico() throws Exception {
        String controllers = Files.walk(Path.of("src/main/java/com/hospital/gestionasistenciashorarioshospital/controlador"))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                })
                .reduce("", String::concat);

        assertFalse(controllers.contains("SimpleEntityDAO<Empleado>"));
        assertFalse(controllers.contains("new SimpleEntityDAO<>(Empleado.class)"));
    }

    @Test
    void noQuedanDatosFijosDelDoctorDeMaqueta() throws Exception {
        String webapp = Files.walk(Path.of("src/main/webapp"))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".xhtml") || path.toString().endsWith(".html"))
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                })
                .reduce("", String::concat);

        assertFalse(webapp.contains("Miguel Jimenez"));
        assertFalse(webapp.contains("Miguel Jim\u00c3\u00a9nez"));
        assertFalse(webapp.contains(">MJ<"));
    }
}
