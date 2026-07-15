(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var toggle = document.getElementById('togglePassword');
        var passwordInput = document.querySelector("input[id$='password']");
        var icon = document.getElementById('toggleIcon');
        var loginForm = document.querySelector("form[id$='loginForm']");
        var submitButton = loginForm ? loginForm.querySelector('.login-submit') : null;

        if (loginForm && submitButton) {
            loginForm.addEventListener('submit', function () {
                if (loginForm.dataset.submitted === 'true') {
                    return false;
                }

                loginForm.dataset.submitted = 'true';
                submitButton.setAttribute('aria-disabled', 'true');
                submitButton.classList.add('disabled');
                submitButton.value = submitButton.dataset.loadingText || 'Ingresando...';
                return true;
            });
        }

        if (!toggle || !passwordInput || !icon) {
            return;
        }

        toggle.addEventListener('click', function () {
            var isHidden = passwordInput.type === 'password';
            passwordInput.type = isHidden ? 'text' : 'password';
            icon.classList.toggle('fa-eye', !isHidden);
            icon.classList.toggle('fa-eye-slash', isHidden);
            toggle.setAttribute('aria-label', isHidden ? 'Ocultar contraseña' : 'Mostrar contraseña');
        });
    });
})();
