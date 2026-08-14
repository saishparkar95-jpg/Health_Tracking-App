/**
 * HealthTrack AI - Client-Side JavaScript
 * Step 4: Chart.js Visualizations, Quick Log Modal, and Auth Form Helpers
 */

/* =============================================================================
   1. PASSWORD VISIBILITY TOGGLE HELPER
   ============================================================================= */
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

/* =============================================================================
   2. QUICK LOG MODAL CONTROLLER
   ============================================================================= */
function openQuickLogModal(initialTab = 'steps') {
    const modal = document.getElementById('quickLogModal');
    if (!modal) return;
    modal.style.display = 'flex';
    switchModalTab(initialTab);
}

function closeQuickLogModal() {
    const modal = document.getElementById('quickLogModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

function switchModalTab(tabName) {
    // Tab Mapping
    const tabMap = {
        'steps': 'form-log-steps',
        'water': 'form-log-water',
        'sleep': 'form-log-sleep',
        'weight': 'form-log-weight',
        'heart_rate': 'form-log-heart'
    };

    // Hide all forms
    document.querySelectorAll('.modal-tab-pane').forEach(pane => {
        pane.style.display = 'none';
    });

    // Deactivate all tab buttons
    document.querySelectorAll('.modal-tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });

    // Show target form
    const targetFormId = tabMap[tabName] || 'form-log-steps';
    const targetForm = document.getElementById(targetFormId);
    if (targetForm) {
        targetForm.style.display = 'block';
    }

    // Activate tab button
    const buttons = document.querySelectorAll('.modal-tab-btn');
    buttons.forEach(btn => {
        if (btn.getAttribute('onclick') && btn.getAttribute('onclick').includes(`'${tabName}'`)) {
            btn.classList.add('active');
        }
    });
}

// Close modal when clicking outside of modal-card
window.addEventListener('click', (e) => {
    const modal = document.getElementById('quickLogModal');
    if (modal && e.target === modal) {
        closeQuickLogModal();
    }
});


/* =============================================================================
   3. CHART.JS INITIALIZATION FOR REAL DATABASE METRICS
   ============================================================================= */
document.addEventListener('DOMContentLoaded', () => {
    console.log("⚡ HealthTrack AI (Step 4 Dashboard & Chart.js) active.");

    // Auto-dismiss Flash Alerts
    const flashAlerts = document.querySelectorAll('.alert');
    flashAlerts.forEach(alert => {
        setTimeout(() => {
            alert.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
            alert.style.opacity = '0';
            alert.style.transform = 'translateY(-10px)';
            setTimeout(() => alert.remove(), 500);
        }, 6000);
    });

    // Read real chart data injected from backend SQLite
    const chartDataEl = document.getElementById('chart-data');
    if (!chartDataEl) return;

    let chartData = null;
    try {
        chartData = JSON.parse(chartDataEl.textContent);
    } catch (e) {
        console.error("Failed to parse Chart JSON data:", e);
        return;
    }

    if (!chartData) return;

    const labels = chartData.short_labels || ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

    // Common Chart.js Theme Defaults
    Chart.defaults.color = '#9ca3af';
    Chart.defaults.font.family = "'Plus Jakarta Sans', sans-serif";
    Chart.defaults.font.size = 12;

    const commonGridOptions = {
        color: 'rgba(255, 255, 255, 0.06)',
        drawBorder: false
    };

    // -------------------------------------------------------------------------
    // Chart 1: Weekly Steps Chart (Bar Chart)
    // -------------------------------------------------------------------------
    const stepsCanvas = document.getElementById('weeklyStepsChart');
    if (stepsCanvas) {
        const ctx = stepsCanvas.getContext('2d');
        const emeraldGradient = ctx.createLinearGradient(0, 0, 0, 260);
        emeraldGradient.addColorStop(0, 'rgba(16, 185, 129, 0.9)');
        emeraldGradient.addColorStop(1, 'rgba(16, 185, 129, 0.25)');

        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Steps Walked',
                    data: chartData.steps || [0, 0, 0, 0, 0, 0, 0],
                    backgroundColor: emeraldGradient,
                    borderColor: '#10b981',
                    borderWidth: 1.5,
                    borderRadius: 8,
                    borderSkipped: false,
                    barPercentage: 0.55
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: '#111827',
                        titleColor: '#f9fafb',
                        bodyColor: '#34d399',
                        borderColor: 'rgba(16, 185, 129, 0.3)',
                        borderWidth: 1,
                        padding: 10,
                        callbacks: {
                            label: (ctx) => `Steps: ${ctx.parsed.y.toLocaleString()} / Goal: ${chartData.step_goal.toLocaleString()}`
                        }
                    }
                },
                scales: {
                    x: { grid: { display: false } },
                    y: {
                        grid: commonGridOptions,
                        suggestedMin: 0,
                        suggestedMax: Math.max(...(chartData.steps || [10000]), chartData.step_goal) * 1.15,
                        ticks: {
                            callback: (v) => v >= 1000 ? (v / 1000) + 'k' : v
                        }
                    }
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Chart 2: Weekly Water Chart (Bar Chart)
    // -------------------------------------------------------------------------
    const waterCanvas = document.getElementById('weeklyWaterChart');
    if (waterCanvas) {
        const ctx = waterCanvas.getContext('2d');
        const cyanGradient = ctx.createLinearGradient(0, 0, 0, 260);
        cyanGradient.addColorStop(0, 'rgba(6, 182, 212, 0.9)');
        cyanGradient.addColorStop(1, 'rgba(6, 182, 212, 0.25)');

        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Water Intake (ml)',
                    data: chartData.water || [0, 0, 0, 0, 0, 0, 0],
                    backgroundColor: cyanGradient,
                    borderColor: '#06b6d4',
                    borderWidth: 1.5,
                    borderRadius: 8,
                    borderSkipped: false,
                    barPercentage: 0.55
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: '#111827',
                        titleColor: '#f9fafb',
                        bodyColor: '#38bdf8',
                        borderColor: 'rgba(6, 182, 212, 0.3)',
                        borderWidth: 1,
                        padding: 10,
                        callbacks: {
                            label: (ctx) => `Intake: ${ctx.parsed.y.toLocaleString()} ml / Goal: ${chartData.water_goal.toLocaleString()} ml`
                        }
                    }
                },
                scales: {
                    x: { grid: { display: false } },
                    y: {
                        grid: commonGridOptions,
                        suggestedMin: 0,
                        suggestedMax: Math.max(...(chartData.water || [2500]), chartData.water_goal) * 1.15,
                        ticks: {
                            callback: (v) => v + ' ml'
                        }
                    }
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Chart 3: Weekly Sleep Chart (Smooth Area + Line Chart)
    // -------------------------------------------------------------------------
    const sleepCanvas = document.getElementById('weeklySleepChart');
    if (sleepCanvas) {
        const ctx = sleepCanvas.getContext('2d');
        const purpleGradient = ctx.createLinearGradient(0, 0, 0, 260);
        purpleGradient.addColorStop(0, 'rgba(168, 85, 247, 0.5)');
        purpleGradient.addColorStop(1, 'rgba(168, 85, 247, 0.02)');

        new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Sleep Rest (Hours)',
                    data: chartData.sleep || [0, 0, 0, 0, 0, 0, 0],
                    fill: true,
                    backgroundColor: purpleGradient,
                    borderColor: '#a855f7',
                    borderWidth: 3,
                    tension: 0.35,
                    pointBackgroundColor: '#c084fc',
                    pointBorderColor: '#111827',
                    pointBorderWidth: 2,
                    pointRadius: 5,
                    pointHoverRadius: 7
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: '#111827',
                        titleColor: '#f9fafb',
                        bodyColor: '#c084fc',
                        borderColor: 'rgba(168, 85, 247, 0.3)',
                        borderWidth: 1,
                        padding: 10,
                        callbacks: {
                            label: (ctx) => `Duration: ${ctx.parsed.y} hrs / Target: ${chartData.sleep_goal} hrs`
                        }
                    }
                },
                scales: {
                    x: { grid: { display: false } },
                    y: {
                        grid: commonGridOptions,
                        suggestedMin: 0,
                        suggestedMax: 12,
                        ticks: {
                            callback: (v) => v + 'h'
                        }
                    }
                }
            }
        });
    }
});
