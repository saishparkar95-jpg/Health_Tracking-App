/**
 * HealthTrack AI - Client-Side Script
 * Step 3: User Authentication, Form Validation, and Password Visibility Helpers
 */

/**
 * Toggles password input visibility between 'password' and 'text'.
 */
function togglePasswordVisibility(inputId, btnElement) {
    const input = document.getElementById(inputId);
    if (!input) return;

    if (input.type === 'password') {
        input.type = 'text';
        btnElement.textContent = '🔒';
        btnElement.setAttribute('title', 'Hide password');
    } else {
        input.type = 'password';
        btnElement.textContent = '👁️';
        btnElement.setAttribute('title', 'Show password');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    console.log("⚡ HealthTrack AI (Step 3: Auth & UI) initialized.");

    // =========================================================================
    // 1. Auto-dismiss Flash Alerts
    // =========================================================================
    const flashAlerts = document.querySelectorAll('.alert');
    flashAlerts.forEach(alert => {
        setTimeout(() => {
            alert.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
            alert.style.opacity = '0';
            alert.style.transform = 'translateY(-10px)';
            setTimeout(() => alert.remove(), 500);
        }, 6000);
    });

    // =========================================================================
    // 2. Registration Form Frontend Validation
    // =========================================================================
    const registerForm = document.getElementById('register-form');
    if (registerForm) {
        const fullNameInput = document.getElementById('full_name');
        const emailInput = document.getElementById('email');
        const passwordInput = document.getElementById('password');
        const confirmPasswordInput = document.getElementById('confirm_password');
        const ageInput = document.getElementById('age');
        const genderInput = document.getElementById('gender');
        const heightInput = document.getElementById('height');
        const weightInput = document.getElementById('weight');
        const errorBanner = document.getElementById('client-error-banner');
        const errorText = document.getElementById('client-error-text');
        const pwMatchHint = document.getElementById('pw-match-hint');

        // Helper to display error
        function showClientError(msg, targetInput) {
            if (errorBanner && errorText) {
                errorText.textContent = msg;
                errorBanner.style.display = 'flex';
                errorBanner.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }
            if (targetInput) {
                targetInput.classList.add('input-error');
                targetInput.focus();
            }
        }

        function clearClientError() {
            if (errorBanner) errorBanner.style.display = 'none';
            const errorInputs = registerForm.querySelectorAll('.input-error');
            errorInputs.forEach(el => el.classList.remove('input-error'));
        }

        // Live password match check
        function checkPasswordMatch() {
            if (!passwordInput || !confirmPasswordInput || !pwMatchHint) return;
            if (!confirmPasswordInput.value) {
                pwMatchHint.textContent = "Must match password";
                pwMatchHint.style.color = "var(--text-muted)";
                return;
            }
            if (passwordInput.value === confirmPasswordInput.value) {
                pwMatchHint.textContent = "✓ Passwords match";
                pwMatchHint.style.color = "var(--primary-light)";
            } else {
                pwMatchHint.textContent = "✗ Passwords do not match";
                pwMatchHint.style.color = "var(--danger)";
            }
        }

        if (passwordInput && confirmPasswordInput) {
            passwordInput.addEventListener('input', checkPasswordMatch);
            confirmPasswordInput.addEventListener('input', checkPasswordMatch);
        }

        // Validate on Submit
        registerForm.addEventListener('submit', (e) => {
            clearClientError();

            // Name
            const nameVal = fullNameInput ? fullNameInput.value.trim() : '';
            if (!nameVal || nameVal.length < 2) {
                e.preventDefault();
                showClientError("Please enter your full name (minimum 2 characters).", fullNameInput);
                return;
            }

            // Email
            const emailVal = emailInput ? emailInput.value.trim() : '';
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailVal || !emailRegex.test(emailVal)) {
                e.preventDefault();
                showClientError("Please enter a valid email address.", emailInput);
                return;
            }

            // Password
            const pwVal = passwordInput ? passwordInput.value : '';
            if (!pwVal || pwVal.length < 6) {
                e.preventDefault();
                showClientError("Password must be at least 6 characters long.", passwordInput);
                return;
            }

            // Confirm Password
            const confirmVal = confirmPasswordInput ? confirmPasswordInput.value : '';
            if (pwVal !== confirmVal) {
                e.preventDefault();
                showClientError("Passwords do not match.", confirmPasswordInput);
                return;
            }

            // Age
            const ageVal = ageInput ? parseInt(ageInput.value, 10) : NaN;
            if (isNaN(ageVal) || ageVal < 10 || ageVal > 120) {
                e.preventDefault();
                showClientError("Please enter a valid age between 10 and 120.", ageInput);
                return;
            }

            // Gender
            const genderVal = genderInput ? genderInput.value : '';
            if (!genderVal) {
                e.preventDefault();
                showClientError("Please select your gender.", genderInput);
                return;
            }

            // Height
            const heightVal = heightInput ? parseFloat(heightInput.value) : NaN;
            if (isNaN(heightVal) || heightVal < 50 || heightVal > 260) {
                e.preventDefault();
                showClientError("Please enter a realistic height between 50 and 260 cm.", heightInput);
                return;
            }

            // Weight
            const weightVal = weightInput ? parseFloat(weightInput.value) : NaN;
            if (isNaN(weightVal) || weightVal < 20 || weightVal > 350) {
                e.preventDefault();
                showClientError("Please enter a realistic weight between 20 and 350 kg.", weightInput);
                return;
            }
        });
    }

    // =========================================================================
    // 3. Login Form Frontend Validation
    // =========================================================================
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        const identifierInput = document.getElementById('identifier');
        const loginPasswordInput = document.getElementById('login_password');
        const loginErrorBanner = document.getElementById('login-error-banner');
        const loginErrorText = document.getElementById('login-error-text');

        loginForm.addEventListener('submit', (e) => {
            const identVal = identifierInput ? identifierInput.value.trim() : '';
            const passVal = loginPasswordInput ? loginPasswordInput.value : '';

            if (loginErrorBanner) loginErrorBanner.style.display = 'none';

            if (!identVal) {
                e.preventDefault();
                if (loginErrorBanner && loginErrorText) {
                    loginErrorText.textContent = "Please enter your email or username.";
                    loginErrorBanner.style.display = 'flex';
                }
                identifierInput.focus();
                return;
            }

            if (!passVal) {
                e.preventDefault();
                if (loginErrorBanner && loginErrorText) {
                    loginErrorText.textContent = "Please enter your password.";
                    loginErrorBanner.style.display = 'flex';
                }
                loginPasswordInput.focus();
                return;
            }
        });
    }
});
