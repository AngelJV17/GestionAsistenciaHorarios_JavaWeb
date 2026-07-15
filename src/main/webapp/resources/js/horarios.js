/* =========================================================
 horarios.js - Funcionalidad del módulo de horarios
 ========================================================= */

(function () {
    'use strict';

    function initPanelCambioHorario() {
        var toggle = document.getElementById('toggleScheduleChange');
        var cancel = document.getElementById('cancelScheduleChange');
        var panel = document.getElementById('scheduleChangePanel');

        if (toggle && panel) {
            toggle.addEventListener('click', function () {
                panel.classList.toggle('is-open');
            });
        }

        if (cancel && panel) {
            cancel.addEventListener('click', function () {
                panel.classList.remove('is-open');
            });
        }
    }

    function obtenerColorEstado(clases) {
        if (!clases) {
            return {
                background: '#cce5ff',
                color: '#004085'
            };
        }

        if (clases.indexOf('is-confirmed') >= 0) {
            return {
                background: '#d4edda',
                color: '#155724'
            };
        }

        if (clases.indexOf('is-pending') >= 0) {
            return {
                background: '#fff3cd',
                color: '#856404'
            };
        }

        if (clases.indexOf('is-rejected') >= 0) {
            return {
                background: '#f8d7da',
                color: '#721c24'
            };
        }

        if (clases.indexOf('is-free') >= 0) {
            return {
                background: 'transparent',
                color: '#111827'
            };
        }

        return {
            background: '#cce5ff',
            color: '#004085'
        };
    }

    function construirTablaParaPdf(tablaOriginal) {
        var tablaClonada = tablaOriginal.cloneNode(true);

        tablaClonada.querySelectorAll('.schedule-status').forEach(function (estado) {
            var colores = obtenerColorEstado(estado.className || '');

            estado.style.display = 'inline-block';
            estado.style.padding = estado.className.indexOf('is-free') >= 0 ? '0' : '6px 12px';
            estado.style.borderRadius = estado.className.indexOf('is-free') >= 0 ? '0' : '999px';
            estado.style.background = colores.background;
            estado.style.color = colores.color;
            estado.style.fontWeight = '800';
            estado.style.whiteSpace = 'nowrap';
        });

        return tablaClonada.outerHTML;
    }

    function exportarHorarioPdf() {
        var tabla = document.querySelector('.weekly-schedule');

        if (!tabla) {
            return;
        }

        var ventana = window.open('', '_blank', 'width=900,height=700');

        if (!ventana) {
            window.print();
            return;
        }

        ventana.document.open();

        ventana.document.write(
                '<!doctype html>' +
                '<html>' +
                '<head>' +
                '<meta charset="UTF-8"/>' +
                '<title>Horario semanal</title>' +
                '<style>' +
                'body{' +
                'font-family:Arial,sans-serif;' +
                'color:#0f172a;' +
                'padding:24px;' +
                '}' +
                'h1{' +
                'font-size:22px;' +
                'margin:0 0 6px;' +
                '}' +
                'p{' +
                'margin:0 0 18px;' +
                'color:#475569;' +
                '}' +
                'table{' +
                'width:100%;' +
                'border-collapse:collapse;' +
                '}' +
                'th{' +
                'background:#1e6bbf;' +
                'color:#fff;' +
                'text-align:left;' +
                '}' +
                'th,td{' +
                'padding:12px;' +
                'border:1px solid #dbe7f6;' +
                'vertical-align:middle;' +
                '}' +
                'strong{' +
                'font-weight:800;' +
                '}' +
                '.schedule-day{' +
                'display:grid;' +
                'font-weight:800;' +
                '}' +
                '.schedule-day small{' +
                'color:#64748b;' +
                'font-size:12px;' +
                '}' +
                '@media print{' +
                '@page{' +
                'size:A4;' +
                'margin:16mm;' +
                '}' +
                '}' +
                '</style>' +
                '</head>' +
                '<body>' +
                '<h1>Horario semanal</h1>' +
                '<p>Turnos y descansos programados de lunes a domingo.</p>' +
                construirTablaParaPdf(tabla) +
                '</body>' +
                '</html>'
                );

        ventana.document.close();
        ventana.focus();

        setTimeout(function () {
            ventana.print();
        }, 250);
    }

    window.exportarHorarioPdf = exportarHorarioPdf;

    document.addEventListener('DOMContentLoaded', function () {
        initPanelCambioHorario();
    });
})();