/* =========================================================
 settings.js - Navegación lateral reutilizable para módulos de configuración.
 ========================================================= */
(function () {
    'use strict';

    function all(selector, context) {
        return Array.from((context || document).querySelectorAll(selector));
    }

    function showSection(container, sectionName) {
        if (!container || !sectionName) {
            return;
        }

        all('.settings-item', container).forEach(function (item) {
            var active = item.dataset.section === sectionName;
            item.classList.toggle('active', active);
            item.setAttribute('aria-selected', String(active));
        });

        all('.settings-section', container).forEach(function (section) {
            var active = section.id === sectionName + '-section';
            section.classList.toggle('active', active);
            section.removeAttribute('hidden');
        });
    }

    function initSettingsContainer(container) {
        var items = all('.settings-item[data-section]', container);
        if (!items.length) {
            return;
        }

        items.forEach(function (item) {
            item.setAttribute('role', 'tab');
            item.setAttribute('tabindex', '0');

            item.addEventListener('click', function () {
                showSection(container, item.dataset.section);
            });

            item.addEventListener('keydown', function (event) {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    showSection(container, item.dataset.section);
                }
            });
        });

        var activeItem = container.querySelector('.settings-item.active[data-section]') || items[0];
        showSection(container, activeItem.dataset.section);
    }

    document.addEventListener('DOMContentLoaded', function () {
        all('.settings-container').forEach(initSettingsContainer);
    });
})();
