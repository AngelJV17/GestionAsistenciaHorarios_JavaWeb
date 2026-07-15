package com.hospital.gestionasistenciashorarioshospital.controlador;

import com.hospital.gestionasistenciashorarioshospital.dao.CargoDAO;
import com.hospital.gestionasistenciashorarioshospital.dao.SimpleEntityDAO;
import com.hospital.gestionasistenciashorarioshospital.modelo.Area;
import com.hospital.gestionasistenciashorarioshospital.modelo.Cargo;
import com.hospital.gestionasistenciashorarioshospital.modelo.Especialidad;
import com.hospital.gestionasistenciashorarioshospital.modelo.TipoDocumento;
import com.hospital.gestionasistenciashorarioshospital.modelo.TipoSolicitud;
import com.hospital.gestionasistenciashorarioshospital.util.CodigoUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class CatalogosController implements Serializable {

    private CargoDAO cargoDAO;
    private SimpleEntityDAO<Area> areaDAO;
    private SimpleEntityDAO<Especialidad> especialidadDAO;
    private SimpleEntityDAO<TipoSolicitud> tipoSolicitudDAO;
    private SimpleEntityDAO<TipoDocumento> tipoDocumentoDAO;

    private List<Cargo> listaCargos = new ArrayList<>();
    private List<Area> listaAreas = new ArrayList<>();
    private List<Especialidad> listaEspecialidades = new ArrayList<>();
    private List<TipoSolicitud> listaTiposSolicitud = new ArrayList<>();
    private List<TipoDocumento> listaTiposDocumento = new ArrayList<>();

    private Cargo cargoSeleccionado;
    private Area areaSeleccionada;
    private Especialidad especialidadSeleccionada;
    private TipoSolicitud tipoSolicitudSeleccionado;
    private TipoDocumento tipoDocumentoSeleccionado;

    @PostConstruct
    public void init() {
        cargoDAO = new CargoDAO();
        areaDAO = new SimpleEntityDAO<>(Area.class);
        especialidadDAO = new SimpleEntityDAO<>(Especialidad.class);
        tipoSolicitudDAO = new SimpleEntityDAO<>(TipoSolicitud.class);
        tipoDocumentoDAO = new SimpleEntityDAO<>(TipoDocumento.class);
        cargarCatalogos();
        nuevoCargo();
        nuevaArea();
        nuevaEspecialidad();
        nuevoTipoSolicitud();
        nuevoTipoDocumento();
    }

    public void cargarCatalogos() {
        listaCargos = cargoDAO.listarTodos();
        listaAreas = areaDAO.listarTodos();
        listaEspecialidades = especialidadDAO.listarTodos();
        listaTiposSolicitud = tipoSolicitudDAO.listarTodos();
        listaTiposDocumento = tipoDocumentoDAO.listarTodos();
    }

    public void nuevoCargo() {
        cargoSeleccionado = new Cargo();
        cargoSeleccionado.setActivo(true);
    }

    public void guardarCargo() {
        if (!validar(cargoSeleccionado.getNombre(), "Ingrese el nombre del cargo.")) {
            return;
        }
        generarCodigoCargo();
        cargoDAO.guardar(cargoSeleccionado);
        cargarCatalogos();
        nuevoCargo();
        mensaje("Cargo guardado");
    }

    public void editarCargo(Long id) { cargoSeleccionado = cargoDAO.buscarPorId(id); }
    public void cambiarEstadoCargo(Long id) {
        Cargo cargo = cargoDAO.buscarPorId(id);
        if (cargo != null) {
            cargo.setActivo(!Boolean.TRUE.equals(cargo.getActivo()));
            cargoDAO.guardar(cargo);
            cargarCatalogos();
        }
    }

    public void nuevaArea() {
        areaSeleccionada = new Area();
        areaSeleccionada.setActivo(true);
    }

    public void guardarArea() {
        if (!validar(areaSeleccionada.getNombre(), "Ingrese el nombre del área.")) {
            return;
        }
        generarCodigoArea();
        areaDAO.guardar(areaSeleccionada);
        cargarCatalogos();
        nuevaArea();
        mensaje("Área guardada");
    }

    public void editarArea(Long id) { areaSeleccionada = areaDAO.buscarPorId(id); }
    public void cambiarEstadoArea(Long id) {
        Area area = areaDAO.buscarPorId(id);
        if (area != null) {
            area.setActivo(!Boolean.TRUE.equals(area.getActivo()));
            areaDAO.guardar(area);
            cargarCatalogos();
        }
    }

    public void nuevaEspecialidad() {
        especialidadSeleccionada = new Especialidad();
        especialidadSeleccionada.setActivo(true);
    }

    public void guardarEspecialidad() {
        if (!validar(especialidadSeleccionada.getNombre(), "Ingrese el nombre de la especialidad.")) {
            return;
        }
        generarCodigoEspecialidad();
        especialidadDAO.guardar(especialidadSeleccionada);
        cargarCatalogos();
        nuevaEspecialidad();
        mensaje("Especialidad guardada");
    }

    public void editarEspecialidad(Long id) { especialidadSeleccionada = especialidadDAO.buscarPorId(id); }
    public void cambiarEstadoEspecialidad(Long id) {
        Especialidad especialidad = especialidadDAO.buscarPorId(id);
        if (especialidad != null) {
            especialidad.setActivo(!Boolean.TRUE.equals(especialidad.getActivo()));
            especialidadDAO.guardar(especialidad);
            cargarCatalogos();
        }
    }

    public void nuevoTipoSolicitud() {
        tipoSolicitudSeleccionado = new TipoSolicitud();
        tipoSolicitudSeleccionado.setActivo(true);
        tipoSolicitudSeleccionado.setRequiereAprobacion(true);
    }

    public void guardarTipoSolicitud() {
        if (!validar(tipoSolicitudSeleccionado.getNombre(), "Ingrese el nombre del tipo de solicitud.")) {
            return;
        }
        generarCodigoTipoSolicitud();
        tipoSolicitudDAO.guardar(tipoSolicitudSeleccionado);
        cargarCatalogos();
        nuevoTipoSolicitud();
        mensaje("Tipo de solicitud guardado");
    }

    public void editarTipoSolicitud(Long id) { tipoSolicitudSeleccionado = tipoSolicitudDAO.buscarPorId(id); }
    public void cambiarEstadoTipoSolicitud(Long id) {
        TipoSolicitud tipo = tipoSolicitudDAO.buscarPorId(id);
        if (tipo != null) {
            tipo.setActivo(!Boolean.TRUE.equals(tipo.getActivo()));
            tipoSolicitudDAO.guardar(tipo);
            cargarCatalogos();
        }
    }

    public void nuevoTipoDocumento() {
        tipoDocumentoSeleccionado = new TipoDocumento();
        tipoDocumentoSeleccionado.setActivo(true);
    }

    public void guardarTipoDocumento() {
        if (!validar(tipoDocumentoSeleccionado.getNombre(), "Ingrese el nombre del tipo de documento.")) {
            return;
        }
        generarCodigoTipoDocumento();
        tipoDocumentoDAO.guardar(tipoDocumentoSeleccionado);
        cargarCatalogos();
        nuevoTipoDocumento();
        mensaje("Tipo de documento guardado");
    }

    public void editarTipoDocumento(Long id) { tipoDocumentoSeleccionado = tipoDocumentoDAO.buscarPorId(id); }
    public void cambiarEstadoTipoDocumento(Long id) {
        TipoDocumento tipo = tipoDocumentoDAO.buscarPorId(id);
        if (tipo != null) {
            tipo.setActivo(!Boolean.TRUE.equals(tipo.getActivo()));
            tipoDocumentoDAO.guardar(tipo);
            cargarCatalogos();
        }
    }

    private void generarCodigoCargo() {
        if (cargoSeleccionado.getCodigo() == null || cargoSeleccionado.getCodigo().isBlank()) {
            cargoSeleccionado.setCodigo(CodigoUtil.normalizarCodigo(cargoSeleccionado.getNombre()));
        }
    }

    private void generarCodigoArea() {
        if (areaSeleccionada.getCodigo() == null || areaSeleccionada.getCodigo().isBlank()) {
            areaSeleccionada.setCodigo(CodigoUtil.normalizarCodigo(areaSeleccionada.getNombre()));
        }
    }

    private void generarCodigoEspecialidad() {
        if (especialidadSeleccionada.getCodigo() == null || especialidadSeleccionada.getCodigo().isBlank()) {
            especialidadSeleccionada.setCodigo(CodigoUtil.normalizarCodigo(especialidadSeleccionada.getNombre()));
        }
    }

    private void generarCodigoTipoSolicitud() {
        if (tipoSolicitudSeleccionado.getCodigo() == null || tipoSolicitudSeleccionado.getCodigo().isBlank()) {
            tipoSolicitudSeleccionado.setCodigo(CodigoUtil.normalizarCodigo(tipoSolicitudSeleccionado.getNombre()));
        }
    }

    private void generarCodigoTipoDocumento() {
        if (tipoDocumentoSeleccionado.getCodigo() == null || tipoDocumentoSeleccionado.getCodigo().isBlank()) {
            tipoDocumentoSeleccionado.setCodigo(CodigoUtil.normalizarCodigo(tipoDocumentoSeleccionado.getNombre()));
        }
    }

    private boolean validar(String nombre, String mensaje) {
        if (nombre == null || nombre.isBlank()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dato requerido", mensaje));
            return false;
        }
        return true;
    }

    private void mensaje(String resumen) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, resumen, "Los datos se guardaron correctamente."));
    }

    public List<Cargo> getListaCargos() { return listaCargos; }
    public List<Area> getListaAreas() { return listaAreas; }
    public List<Especialidad> getListaEspecialidades() { return listaEspecialidades; }
    public List<TipoSolicitud> getListaTiposSolicitud() { return listaTiposSolicitud; }
    public List<TipoDocumento> getListaTiposDocumento() { return listaTiposDocumento; }
    public Cargo getCargoSeleccionado() { return cargoSeleccionado; }
    public void setCargoSeleccionado(Cargo cargoSeleccionado) { this.cargoSeleccionado = cargoSeleccionado; }
    public Area getAreaSeleccionada() { return areaSeleccionada; }
    public void setAreaSeleccionada(Area areaSeleccionada) { this.areaSeleccionada = areaSeleccionada; }
    public Especialidad getEspecialidadSeleccionada() { return especialidadSeleccionada; }
    public void setEspecialidadSeleccionada(Especialidad especialidadSeleccionada) { this.especialidadSeleccionada = especialidadSeleccionada; }
    public TipoSolicitud getTipoSolicitudSeleccionado() { return tipoSolicitudSeleccionado; }
    public void setTipoSolicitudSeleccionado(TipoSolicitud tipoSolicitudSeleccionado) { this.tipoSolicitudSeleccionado = tipoSolicitudSeleccionado; }
    public TipoDocumento getTipoDocumentoSeleccionado() { return tipoDocumentoSeleccionado; }
    public void setTipoDocumentoSeleccionado(TipoDocumento tipoDocumentoSeleccionado) { this.tipoDocumentoSeleccionado = tipoDocumentoSeleccionado; }
}
