/* =========================================================
 asistencias.js - Funcionalidad específica del módulo
 ========================================================= */

(function () {
    'use strict';

    var calendarioRenderizado = false;

    function actualizarReloj() {
        var reloj = document.getElementById('relojActual');
        var fecha = document.getElementById('fechaActual');

        if (!reloj && !fecha) {
            return;
        }

        var ahora = new Date();

        if (reloj) {
            reloj.textContent = ahora.toLocaleTimeString('es-PE', {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                hour12: false
            });
        }

        if (fecha) {
            fecha.textContent = ahora.toLocaleDateString('es-PE', {
                weekday: 'long',
                day: '2-digit',
                month: 'long',
                year: 'numeric'
            }).toUpperCase();
        }
    }

    function normalizarFecha(fechaTexto) {
        if (!fechaTexto) {
            return '';
        }

        return String(fechaTexto).trim().substring(0, 10);
    }

    function obtenerAsistenciasDesdeTabla() {
        var filas = document.querySelectorAll('#tablaAsistenciasMedico tbody tr.fila-asistencia');
        var asistencias = [];

        filas.forEach(function (fila) {
            var enlaceJustificacion = fila.querySelector('.attachment-open-btn');
            asistencias.push({
                fecha: normalizarFecha(fila.getAttribute('data-fecha')),
                estado: fila.getAttribute('data-estado') || 'DEFAULT',
                estadoTexto: fila.getAttribute('data-estado-texto') || 'Registro',
                puedeJustificar: fila.getAttribute('data-puede-justificar') === 'true',
                justificacionTexto: fila.getAttribute('data-justificacion-texto') || '',
                enlaceJustificacion: enlaceJustificacion ? enlaceJustificacion.getAttribute('href') : '',
                fila: fila
            });
        });

        return asistencias;
    }

    function obtenerClaseEvento(estado, justificacionTexto) {
        var texto = ((estado || '') + ' ' + (justificacionTexto || ''))
                .toString()
                .toLowerCase()
                .normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '');

        /*
         * Prioridad visual:
         * Si ya está justificado, debe pintar como Justificado,
         * aunque el estado original haya sido Tardanza o Falta.
         */
        if (texto.indexOf('justificado') >= 0
                || texto.indexOf('justificacion aprobada') >= 0
                || texto.indexOf('permiso') >= 0
                || texto.indexOf('licencia') >= 0) {
            return 'justificado';
        }

        if (texto.indexOf('vacacion') >= 0 || texto.indexOf('vacaciones') >= 0) {
            return 'vacaciones';
        }

        if (texto.indexOf('asist') >= 0 || texto.indexOf('puntual') >= 0) {
            return 'asistio';
        }

        if (texto.indexOf('tard') >= 0) {
            return 'tardanza';
        }

        if (texto.indexOf('falt') >= 0) {
            return 'falta';
        }

        return 'default';
    }

    function obtenerTextoEstadoCalendario(item) {
        var texto = (item.justificacionTexto || '')
                .toString()
                .toLowerCase()
                .normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '');

        if (texto.indexOf('justificado') >= 0
                || texto.indexOf('justificacion aprobada') >= 0) {
            return 'Justificado';
        }

        return item.estadoTexto || 'Registro';
    }

    function obtenerTextoHoraCalendario(item) {
        var fila = item.fila;

        if (!fila) {
            return '';
        }

        var celdas = fila.querySelectorAll('td');

        if (celdas.length < 3) {
            return '';
        }

        var entrada = (celdas[1].textContent || '').replace(/\s+/g, ' ').trim();
        var salida = (celdas[2].textContent || '').replace(/\s+/g, ' ').trim();

        if (!entrada || entrada === '--:--') {
            return '';
        }

        if (!salida || salida === '--:--') {
            return entrada;
        }

        return entrada + ' - ' + salida;
    }

    function fechaISO(anio, mes, dia) {
        var mesTexto = String(mes).padStart(2, '0');
        var diaTexto = String(dia).padStart(2, '0');

        return anio + '-' + mesTexto + '-' + diaTexto;
    }

    function renderizarCalendario() {
        var contenedor = document.getElementById('attendanceCalendar');

        if (!contenedor) {
            return;
        }

        var mes = parseInt(contenedor.getAttribute('data-month'), 10);
        var anio = parseInt(contenedor.getAttribute('data-year'), 10);

        if (!mes || !anio) {
            return;
        }

        var asistencias = obtenerAsistenciasDesdeTabla();
        var asistenciasPorFecha = {};

        asistencias.forEach(function (item) {
            if (!asistenciasPorFecha[item.fecha]) {
                asistenciasPorFecha[item.fecha] = [];
            }

            asistenciasPorFecha[item.fecha].push(item);
        });

        //var diasSemana = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb'];
        var diasSemana = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
        var primerDia = new Date(anio, mes - 1, 1);
        var totalDias = new Date(anio, mes, 0).getDate();
        var inicioSemana = primerDia.getDay();

        var hoy = new Date();
        var hoyISO = fechaISO(hoy.getFullYear(), hoy.getMonth() + 1, hoy.getDate());

        contenedor.innerHTML = '';

        diasSemana.forEach(function (dia) {
            var cabecera = document.createElement('div');
            cabecera.className = 'attendance-calendar-head';
            cabecera.textContent = dia;
            contenedor.appendChild(cabecera);
        });

        for (var vacio = 0; vacio < inicioSemana; vacio++) {
            var celdaVacia = document.createElement('div');
            celdaVacia.className = 'attendance-calendar-day is-muted';
            contenedor.appendChild(celdaVacia);
        }

        for (var diaMes = 1; diaMes <= totalDias; diaMes++) {
            var fecha = fechaISO(anio, mes, diaMes);
            var celda = document.createElement('div');

            celda.className = 'attendance-calendar-day';

            if (fecha === hoyISO) {
                celda.classList.add('is-today');
            }

            var numero = document.createElement('span');
            numero.className = 'calendar-day-number';
            numero.textContent = diaMes;
            celda.appendChild(numero);

            if (asistenciasPorFecha[fecha]) {
                asistenciasPorFecha[fecha].forEach(function (item) {
                    var claseEstado = obtenerClaseEvento(
                            item.estado + ' ' + item.estadoTexto,
                            item.justificacionTexto
                            );

                    var textoEstado = obtenerTextoEstadoCalendario(item);
                    var hora = obtenerTextoHoraCalendario(item);

                    celda.classList.add('status-' + claseEstado);

                    if (hora) {
                        var horario = document.createElement('span');
                        horario.className = 'calendar-event-time';
                        horario.textContent = hora;
                        celda.appendChild(horario);
                    }

                    var evento = document.createElement('span');
                    evento.className = 'calendar-event ' + claseEstado;
                    evento.textContent = textoEstado;
                    evento.title = textoEstado;

                    celda.appendChild(evento);

                    if (claseEstado !== 'justificado' && item.puedeJustificar && item.enlaceJustificacion) {
                        var justificar = document.createElement('a');
                        justificar.className = 'calendar-justify-link';
                        justificar.href = item.enlaceJustificacion;
                        justificar.title = 'Justificar incidencia';
                        justificar.innerHTML = '<i class="fas fa-file-signature"></i><span>Justificar</span>';

                        celda.appendChild(justificar);
                    }
                });
            } else {
                celda.classList.add('status-vacio');

                var eventoVacio = document.createElement('span');
                eventoVacio.className = 'calendar-event default';
                eventoVacio.textContent = 'Sin registro';
                eventoVacio.title = 'Sin registro';

                celda.appendChild(eventoVacio);
            }

            contenedor.appendChild(celda);
        }

        calendarioRenderizado = true;
    }

    function renderizarListaCalendario() {
        var lista = document.getElementById('attendanceCalendarList');

        if (!lista) {
            return;
        }

        var asistencias = obtenerAsistenciasDesdeTabla();

        lista.innerHTML = '';

        if (!asistencias.length) {
            var vacio = document.createElement('div');
            vacio.className = 'attendance-calendar-list-item';
            vacio.textContent = 'No hay registros para mostrar.';
            lista.appendChild(vacio);
            return;
        }

        asistencias.forEach(function (item) {
            var clase = obtenerClaseEvento(
                    item.estado + ' ' + item.estadoTexto,
                    item.justificacionTexto
                    );

            var textoEstado = obtenerTextoEstadoCalendario(item);

            var fila = document.createElement('div');
            fila.className = 'attendance-calendar-list-item status-' + clase;

            var accionesHtml = '';

            if (clase !== 'justificado' && item.puedeJustificar && item.enlaceJustificacion) {
                accionesHtml =
                        '<a class="calendar-justify-link" href="' + item.enlaceJustificacion + '" title="Justificar incidencia">' +
                        '<i class="fas fa-file-signature"></i>' +
                        '<span>Justificar</span>' +
                        '</a>';
            }

            fila.innerHTML =
                    '<div class="attendance-calendar-list-head">' +
                    '<span>' + item.fecha + '</span>' +
                    '<span><i class="status-dot ' + clase + '"></i> ' + textoEstado + '</span>' +
                    '</div>' +
                    '<div class="attendance-calendar-list-body">' +
                    '<span>' + (clase === 'justificado' ? 'Justificación aprobada' : 'Registro de asistencia') + '</span>' +
                    accionesHtml +
                    '</div>';

            lista.appendChild(fila);
        });
    }

    function iniciarTabsBootstrap() {
        var tabCalendario = document.getElementById('tabCalendario');

        if (!tabCalendario) {
            return;
        }

        tabCalendario.addEventListener('shown.bs.tab', function () {
            renderizarCalendario();
            renderizarListaCalendario();
        });
    }

    function iniciarModoCalendario() {
        var botonMes = document.getElementById('calendarMonthMode');
        var botonLista = document.getElementById('calendarListMode');
        var calendario = document.getElementById('attendanceCalendar');
        var lista = document.getElementById('attendanceCalendarList');

        if (!botonMes || !botonLista || !calendario || !lista) {
            return;
        }

        botonMes.addEventListener('click', function () {
            calendario.classList.remove('d-none');
            lista.classList.add('d-none');

            botonMes.classList.add('btn-secondary');
            botonMes.classList.remove('btn-outline-secondary');

            botonLista.classList.remove('btn-secondary');
            botonLista.classList.add('btn-outline-secondary');

            renderizarCalendario();
        });

        botonLista.addEventListener('click', function () {
            calendario.classList.add('d-none');
            lista.classList.remove('d-none');

            botonLista.classList.add('btn-secondary');
            botonLista.classList.remove('btn-outline-secondary');

            botonMes.classList.remove('btn-secondary');
            botonMes.classList.add('btn-outline-secondary');

            renderizarListaCalendario();
        });
    }

    function iniciarFiltrosHistorial() {
        var botones = document.querySelectorAll('[data-filter]');
        var filas = document.querySelectorAll('#tablaAsistenciasMedico tbody tr.fila-asistencia');

        if (!botones.length || !filas.length) {
            return;
        }

        botones.forEach(function (boton) {
            boton.addEventListener('click', function () {
                var filtro = boton.getAttribute('data-filter') || 'TODOS';

                botones.forEach(function (item) {
                    item.classList.remove('btn-secondary');
                    item.classList.add('btn-outline-primary');
                });

                boton.classList.remove('btn-outline-primary');
                boton.classList.add('btn-secondary');

                filas.forEach(function (fila) {
                    var estado = fila.getAttribute('data-estado') || '';

                    if (filtro === 'TODOS' || estado === filtro) {
                        fila.classList.remove('d-none');
                    } else {
                        fila.classList.add('d-none');
                    }
                });
            });
        });
    }

    function confirmarMarcacion(boton) {
        var mensaje = boton.getAttribute('data-confirm-message') || '¿Desea registrar esta marcación?';
        var destino = boton.getAttribute('data-submit-target');
        var submit = destino ? document.querySelector(destino) : null;

        if (!submit) {
            return;
        }

        if (!window.Swal) {
            if (confirm(mensaje)) {
                submit.click();
            }

            return;
        }

        Swal.fire({
            title: 'Confirmar marcación',
            text: mensaje,
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: 'Sí, registrar',
            cancelButtonText: 'Cancelar',
            confirmButtonColor: '#1e88e5',
            cancelButtonColor: '#6c757d',
            reverseButtons: true
        }).then(function (resultado) {
            if (resultado.isConfirmed) {
                submit.click();
            }
        });
    }

    function iniciarConfirmaciones() {
        var botones = document.querySelectorAll('[data-attendance-confirm]');

        botones.forEach(function (boton) {
            boton.addEventListener('click', function (event) {
                event.preventDefault();
                confirmarMarcacion(boton);
            });
        });
    }

    function iniciarModuloAsistencias() {
        actualizarReloj();

        if (document.getElementById('relojActual') || document.getElementById('fechaActual')) {
            setInterval(actualizarReloj, 1000);
        }

        iniciarTabsBootstrap();
        iniciarModoCalendario();
        iniciarFiltrosHistorial();
        iniciarConfirmaciones();

        if (document.getElementById('panelCalendario')
                && document.getElementById('panelCalendario').classList.contains('show')) {
            renderizarCalendario();
            renderizarListaCalendario();
        }
    }

    window.actualizarRelojAsistencias = actualizarReloj;
    window.confirmarMarcacionAsistencia = confirmarMarcacion;
    window.renderizarCalendarioAsistencias = renderizarCalendario;

    document.addEventListener('DOMContentLoaded', iniciarModuloAsistencias);
})();