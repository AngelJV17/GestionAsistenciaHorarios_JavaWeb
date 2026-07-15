/* =========================================================
 main.js - Funcionalidad común del sistema hospitalario
 ========================================================= */
(function () {
    'use strict';

    function mostrarFechaActual() {
        var el = document.getElementById('currentDate');

        if (!el) {
            return;
        }

        var fecha = new Date().toLocaleDateString('es-PE', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });

        el.textContent = fecha.charAt(0).toUpperCase() + fecha.slice(1);
    }

    function initResponsiveMenu() {
        var toggle = document.querySelector('.hfms-nav__toggle');
        var menu = document.querySelector('.hfms-nav__menu');

        if (!toggle || !menu) {
            return;
        }

        var icon = toggle.querySelector('.hfms-nav__toggle-icon');

        var closeMenu = function () {
            menu.classList.remove('active');
            toggle.classList.remove('active');
            toggle.setAttribute('aria-expanded', 'false');
            toggle.setAttribute('aria-label', 'Abrir menú de navegación');

            if (icon) {
                icon.classList.add('fa-bars');
                icon.classList.remove('fa-times');
            }
        };

        var openMenu = function () {
            menu.classList.add('active');
            menu.scrollTop = 0;
            toggle.classList.add('active');
            toggle.setAttribute('aria-expanded', 'true');
            toggle.setAttribute('aria-label', 'Cerrar menú de navegación');

            if (icon) {
                icon.classList.remove('fa-bars');
                icon.classList.add('fa-times');
            }
        };

        toggle.setAttribute('aria-controls', menu.id || 'mainMenu');
        toggle.setAttribute('aria-expanded', 'false');

        if (!menu.id) {
            menu.id = 'mainMenu';
        }

        toggle.addEventListener('click', function (e) {
            e.stopPropagation();
            menu.classList.contains('active') ? closeMenu() : openMenu();
        });

        menu.querySelectorAll('.hfms-nav__link').forEach(function (link) {
            link.addEventListener('click', function () {
                if (window.innerWidth <= 768) {
                    closeMenu();
                }
            });
        });

        document.addEventListener('click', function (e) {
            if (!e.target.closest('.hfms-nav__container')) {
                closeMenu();
            }
        });

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                closeMenu();
                toggle.focus();
            }
        });

        window.addEventListener('resize', function () {
            if (window.innerWidth > 768) {
                closeMenu();
            }
        });
    }

    function initPageLoader() {
        var loader = document.getElementById('pageLoader');

        if (!loader) {
            return;
        }

        window.addEventListener('load', function () {
            setTimeout(function () {
                loader.style.transition = 'opacity 0.3s ease';
                loader.style.opacity = '0';
                loader.setAttribute('aria-hidden', 'true');

                setTimeout(function () {
                    loader.remove();
                }, 300);
            }, 250);
        });
    }

    function markFieldsWithErrors() {
        document.querySelectorAll('.field-error').forEach(function (error) {
            var text = (error.textContent || '').trim();

            if (!text) {
                return;
            }

            var wrapper = error.previousElementSibling;

            if (!wrapper) {
                return;
            }

            var input = wrapper.querySelector('.form-control, .form-select');

            if (input) {
                input.classList.add('is-invalid');
            }
        });
    }

    function initSweetAlertsFromFacesMessages() {
        if (!window.Swal) {
            return;
        }

        var messages = [];

        document.querySelectorAll('.jsf-alert-source li, .jsf-alert-source td, .jsf-alert-source span').forEach(function (node) {
            var text = (node.textContent || '').replace(/\s+/g, ' ').trim();

            if (text && messages.indexOf(text) === -1) {
                messages.push(text);
            }
        });

        if (!messages.length) {
            return;
        }

        var joined = messages.join('<br/>');
        var lower = joined.toLowerCase();

        var esError = lower.indexOf('error') >= 0 ||
                lower.indexOf('inválido') >= 0 ||
                lower.indexOf('inválido') >= 0 ||
                lower.indexOf('requerido') >= 0 ||
                lower.indexOf('no se pudo') >= 0 ||
                lower.indexOf('exception') >= 0;
        var esInfo = lower.indexOf('anticipada') >= 0 ||
                lower.indexOf('día de descanso') >= 0 ||
                lower.indexOf('día de descanso') >= 0 ||
                lower.indexOf('podrá registrar') >= 0 ||
                lower.indexOf('podrá registrar') >= 0;
        var icon = esError ? 'error' : (esInfo ? 'info' : 'success');

        Swal.fire({
            icon: icon,
            title: icon === 'error' ? 'Revise la información' : (icon === 'info' ? 'Información' : 'Operación realizada'),
            html: joined,
            confirmButtonText: 'Entendido',
            confirmButtonColor: '#1e88e5'
        });
    }

    function confirmarEliminacionRol(boton) {
        var formulario = boton.closest('form');
        var botonEliminar = formulario ? formulario.querySelector('.js-eliminar-rol-submit') : null;
        var nombreRol = boton.getAttribute('data-rol') || 'este rol';

        if (!botonEliminar) {
            return;
        }

        if (!window.Swal) {
            console.warn('SweetAlert2 no está disponible para confirmar la eliminación del rol.');
            return;
        }

        Swal.fire({
            title: '¿Eliminar rol?',
            html: 'Se eliminará el rol <strong>' + nombreRol + '</strong> y también sus relaciones con permisos y usuarios.',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Sí, eliminar',
            cancelButtonText: 'Cancelar',
            confirmButtonColor: '#dc3545',
            cancelButtonColor: '#6c757d',
            reverseButtons: true
        }).then(function (resultado) {
            if (resultado.isConfirmed) {
                botonEliminar.click();
            }
        });
    }

    function confirmarEliminacionDocumento(boton) {
        var nombreDocumento = boton.getAttribute('data-document-name') || 'este documento';

        if (boton.getAttribute('data-confirmed') === 'true') {
            boton.removeAttribute('data-confirmed');
            return true;
        }

        if (!window.Swal) {
            console.warn('SweetAlert2 no está disponible para confirmar la eliminación del documento.');
            return false;
        }

        Swal.fire({
            title: '¿Eliminar documento?',
            html: 'Se eliminará <strong>' + nombreDocumento + '</strong> y también el archivo físico del storage.',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Sí, eliminar',
            cancelButtonText: 'Cancelar',
            confirmButtonColor: '#dc3545',
            cancelButtonColor: '#6c757d',
            reverseButtons: true,
            focusCancel: true
        }).then(function (resultado) {
            if (resultado.isConfirmed) {
                boton.setAttribute('data-confirmed', 'true');
                boton.click();
            }
        });

        return false;
    }

    function confirmarAccionEliminacion(boton) {
        var titulo = boton.getAttribute('data-confirm-title') || '¿Eliminar registro?';
        var texto = boton.getAttribute('data-confirm-text') || 'Esta acción no se puede deshacer.';

        if (boton.getAttribute('data-confirmed') === 'true') {
            boton.removeAttribute('data-confirmed');
            return true;
        }

        if (!window.Swal) {
            console.warn('SweetAlert2 no está disponible para confirmar la eliminación.');
            return false;
        }

        Swal.fire({
            title: titulo,
            text: texto,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Sí, eliminar',
            cancelButtonText: 'Cancelar',
            confirmButtonColor: '#dc3545',
            cancelButtonColor: '#6c757d',
            reverseButtons: true,
            focusCancel: true
        }).then(function (resultado) {
            if (resultado.isConfirmed) {
                boton.setAttribute('data-confirmed', 'true');
                boton.click();
            }
        });

        return false;
    }

    function abrirEnNuevaPestana(boton) {
        var formulario = boton.closest('form');
        if (!formulario) {
            return true;
        }
        formulario.setAttribute('target', '_blank');
        window.setTimeout(function () {
            formulario.removeAttribute('target');
        }, 1000);
        return true;
    }

    function organizarPermisosComoGrid() {
        var tablaPermisos = document.querySelector("[id$='permisosRol']");

        if (!tablaPermisos) {
            return;
        }

        tablaPermisos.style.width = '100%';
        tablaPermisos.style.display = 'grid';
        tablaPermisos.style.gridTemplateColumns = 'repeat(auto-fit, minmax(260px, 1fr))';
        tablaPermisos.style.gap = '10px';

        tablaPermisos.querySelectorAll('tbody, tr').forEach(function (elemento) {
            elemento.style.display = 'contents';
        });

        tablaPermisos.querySelectorAll('td').forEach(function (celda) {
            celda.style.marginBottom = '0';
            celda.style.minHeight = '58px';
        });
    }

    function prepararTablasResponsivas() {
        document.querySelectorAll('.system-table').forEach(function (tabla) {
            var headers = Array.prototype.map.call(tabla.querySelectorAll('thead th'), function (th) {
                return (th.textContent || '').replace(/\s+/g, ' ').trim();
            });

            tabla.querySelectorAll('tbody tr').forEach(function (fila) {
                Array.prototype.forEach.call(fila.children, function (celda, index) {
                    if (!celda.getAttribute('data-label') && headers[index]) {
                        celda.setAttribute('data-label', headers[index]);
                    }
                });
            });
        });
    }

    /*
     * Se expone esta función al objeto window porque el botón eliminar
     * de roles.xhtml la llama directamente con onclick="confirmarEliminacionRol(this)".
     */
    window.confirmarEliminacionRol = confirmarEliminacionRol;
    window.confirmarEliminacionDocumento = confirmarEliminacionDocumento;
    window.confirmarAccionEliminacion = confirmarAccionEliminacion;
    window.abrirEnNuevaPestana = abrirEnNuevaPestana;

    document.addEventListener('DOMContentLoaded', function () {
        mostrarFechaActual();
        initResponsiveMenu();
        initPageLoader();
        markFieldsWithErrors();
        initSweetAlertsFromFacesMessages();
        organizarPermisosComoGrid();
        prepararTablasResponsivas();
    });
})();
