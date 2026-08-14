/**
 * HealthTrack AI - Client-Side JavaScript
 * Step 6: Water Tracking Module, Liquid Bottle Gauge, Chart.js Integrations, and Modals
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
   2. DASHBOARD QUICK LOG MODAL CONTROLLER
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
    const tabMap = {
        'steps': 'form-log-steps',
        'water': 'form-log-water',
        'sleep': 'form-log-sleep',
        'weight': 'form-log-weight',
        'heart_rate': 'form-log-heart'
    };

    document.querySelectorAll('.modal-tab-pane').forEach(pane => {
        pane.style.display = 'none';
    });

    document.querySelectorAll('.modal-tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });

    const targetFormId = tabMap[tabName] || 'form-log-steps';
    const targetForm = document.getElementById(targetFormId);
    if (targetForm) {
        targetForm.style.display = 'block';
    }

    const buttons = document.querySelectorAll('.modal-tab-btn');
    buttons.forEach(btn => {
        if (btn.getAttribute('onclick') && btn.getAttribute('onclick').includes(`'${tabName}'`)) {
            btn.classList.add('active');
        }
    });
}


/* =============================================================================
   3. STEP TRACKING MODAL CONTROLLER (STEP 5)
   ============================================================================= */
function openStepModal(tabName = 'add') {
    const modal = document.getElementById('stepActionModal');
    if (!modal) return;
    modal.style.display = 'flex';
    switchStepModalTab(tabName);
}

function closeStepModal() {
    const modal = document.getElementById('stepActionModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

function switchStepModalTab(tabName) {
    const tabFormMap = {
        'add': 'form-add-steps',
        'update': 'form-update-steps',
        'goal': 'form-goal-steps'
    };

    const tabBtnMap = {
        'add': 'tab-add',
        'update': 'tab-update',
        'goal': 'tab-goal'
    };

    document.querySelectorAll('.step-modal-pane').forEach(pane => {
        pane.style.display = 'none';
    });

    document.querySelectorAll('.modal-tabs .modal-tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });

    const targetFormId = tabFormMap[tabName] || 'form-add-steps';
    const targetForm = document.getElementById(targetFormId);
    if (targetForm) {
        targetForm.style.display = 'block';
    }

    const targetBtnId = tabBtnMap[tabName] || 'tab-add';
    const targetBtn = document.getElementById(targetBtnId);
    if (targetBtn) {
        targetBtn.classList.add('active');
    }
}


/* =============================================================================
   4. WATER TRACKING MODAL CONTROLLER (STEP 6)
   ============================================================================= */
function openWaterModal(tabName = 'custom') {
    const modal = document.getElementById('waterActionModal');
    if (!modal) return;
    modal.style.display = 'flex';
    switchWaterModalTab(tabName);
}

function closeWaterModal() {
    const modal = document.getElementById('waterActionModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

function switchWaterModalTab(tabName) {
    const tabFormMap = {
        'custom': 'form-custom-water',
        'goal': 'form-goal-water'
    };

    const tabBtnMap = {
        'custom': 'water-tab-custom',
        'goal': 'water-tab-goal'
    };

    document.querySelectorAll('.water-modal-pane').forEach(pane => {
        pane.style.display = 'none';
    });

    document.querySelectorAll('#waterActionModal .modal-tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });

    const targetFormId = tabFormMap[tabName] || 'form-custom-water';
    const targetForm = document.getElementById(targetFormId);
    if (targetForm) {
        targetForm.style.display = 'block';
    }

    const targetBtnId = tabBtnMap[tabName] || 'water-tab-custom';
    const targetBtn = document.getElementById(targetBtnId);
    if (targetBtn) {
        targetBtn.classList.add('active');
    }
}

// Window click-outside dismiss for all modals
window.addEventListener('click', (e) => {
    const quickModal = document.getElementById('quickLogModal');
    if (quickModal && e.target === quickModal) {
        closeQuickLogModal();
    }
    const stepModal = document.getElementById('stepActionModal');
    if (stepModal && e.target === stepModal) {
        closeStepModal();
    }
    const waterModal = document.getElementById('waterActionModal');
    if (waterModal && e.target === waterModal) {
        closeWaterModal();
    }
});


/* =============================================================================
   5. STEP MODULE WEEKLY VS MONTHLY CHART.JS SWITCHER (STEP 5)
   ============================================================================= */
let stepTrendChartInstance = null;
let stepPayloadData = null;

function renderStepTrendChart(period = 'weekly') {
    const canvas = document.getElementById('stepTrendChart');
    if (!canvas || !stepPayloadData) return;

    const ctx = canvas.getContext('2d');
    const isWeekly = period === 'weekly';
    const chartConfig = isWeekly ? stepPayloadData.weekly : stepPayloadData.monthly;
    const goalValue = stepPayloadData.goal || 10000;

    const titleEl = document.getElementById('chart-dynamic-title');
    const subEl = document.getElementById('chart-dynamic-sub');
    const badgeEl = document.getElementById('chart-period-badge');

    if (titleEl) titleEl.textContent = isWeekly ? '👟 Weekly Step Activity (Past 7 Days)' : '🗓️ Monthly Step Activity (Past 30 Days)';
    if (subEl) subEl.textContent = isWeekly ? `Daily step counts vs ${goalValue.toLocaleString()} daily target` : `30-day walking progress vs ${goalValue.toLocaleString()} daily target`;
    if (badgeEl) badgeEl.textContent = isWeekly ? '7-Day History' : '30-Day History';

    const emeraldGrad = ctx.createLinearGradient(0, 0, 0, 300);
    emeraldGrad.addColorStop(0, 'rgba(16, 185, 129, 0.9)');
    emeraldGrad.addColorStop(1, 'rgba(16, 185, 129, 0.25)');

    if (stepTrendChartInstance) {
        stepTrendChartInstance.destroy();
    }

    stepTrendChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: chartConfig.labels,
            datasets: [{
                label: 'Steps Walked',
                data: chartConfig.data,
                backgroundColor: emeraldGrad,
                borderColor: '#10b981',
                borderWidth: 1.5,
                borderRadius: isWeekly ? 8 : 4,
                borderSkipped: false,
                barPercentage: isWeekly ? 0.6 : 0.75
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
                    padding: 12,
                    callbacks: {
                        label: (ctx) => `Steps: ${ctx.parsed.y.toLocaleString()} / Goal: ${goalValue.toLocaleString()}`
                    }
                }
            },
            scales: {
                x: {
                    grid: { display: false },
                    ticks: {
                        maxRotation: isWeekly ? 0 : 45,
                        autoSkip: !isWeekly
                    }
                },
                y: {
                    grid: { color: 'rgba(255, 255, 255, 0.06)' },
                    suggestedMin: 0,
                    suggestedMax: Math.max(...chartConfig.data, goalValue) * 1.15,
                    ticks: {
                        callback: (v) => v >= 1000 ? (v / 1000) + 'k' : v
                    }
                }
            }
        }
    });
}

function showChartPeriod(period) {
    const btnWeekly = document.getElementById('btn-show-weekly');
    const btnMonthly = document.getElementById('btn-show-monthly');

    if (period === 'weekly') {
        if (btnWeekly) btnWeekly.classList.add('active');
        if (btnMonthly) btnMonthly.classList.remove('active');
    } else {
        if (btnMonthly) btnMonthly.classList.add('active');
        if (btnWeekly) btnWeekly.classList.remove('active');
    }

    renderStepTrendChart(period);
}


/* =============================================================================
   6. DOM INITIALIZATION & CHART.JS MOUNTING
   ============================================================================= */
document.addEventListener('DOMContentLoaded', () => {
    console.log("⚡ HealthTrack AI (Modules & Visualizations) active.");

    // Auto-dismiss Alerts
    const flashAlerts = document.querySelectorAll('.alert');
    flashAlerts.forEach(alert => {
        setTimeout(() => {
            alert.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
            alert.style.opacity = '0';
            alert.style.transform = 'translateY(-10px)';
            setTimeout(() => alert.remove(), 500);
        }, 6000);
    });

    // Theme Defaults
    Chart.defaults.color = '#9ca3af';
    Chart.defaults.font.family = "'Plus Jakarta Sans', sans-serif";
    Chart.defaults.font.size = 12;

    const commonGridOptions = {
        color: 'rgba(255, 255, 255, 0.06)',
        drawBorder: false
    };

    // 1. Initialize Step Module Specific Chart
    const stepPayloadEl = document.getElementById('step-chart-payload');
    if (stepPayloadEl) {
        try {
            stepPayloadData = JSON.parse(stepPayloadEl.textContent);
            renderStepTrendChart('weekly');
        } catch (err) {
            console.error("Failed to parse Step Chart Payload:", err);
        }
    }

    // 2. Initialize Water Module Specific Chart (Step 6)
    const waterPayloadEl = document.getElementById('water-chart-payload');
    const waterTrendCanvas = document.getElementById('waterTrendChart');
    if (waterPayloadEl && waterTrendCanvas) {
        try {
            const waterPayload = JSON.parse(waterPayloadEl.textContent);
            const ctx = waterTrendCanvas.getContext('2d');
            const cyanGradient = ctx.createLinearGradient(0, 0, 0, 300);
            cyanGradient.addColorStop(0, 'rgba(6, 182, 212, 0.9)');
            cyanGradient.addColorStop(1, 'rgba(6, 182, 212, 0.25)');

            new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: waterPayload.labels,
                    datasets: [{
                        label: 'Hydration Intake (ml)',
                        data: waterPayload.data,
                        backgroundColor: cyanGradient,
                        borderColor: '#06b6d4',
                        borderWidth: 1.5,
                        borderRadius: 8,
                        borderSkipped: false,
                        barPercentage: 0.6
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
                            padding: 12,
                            callbacks: {
                                label: (ctx) => `Intake: ${ctx.parsed.y.toLocaleString()} ml / Goal: ${waterPayload.goal.toLocaleString()} ml`
                            }
                        }
                    },
                    scales: {
                        x: { grid: { display: false } },
                        y: {
                            grid: commonGridOptions,
                            suggestedMin: 0,
                            suggestedMax: Math.max(...waterPayload.data, waterPayload.goal) * 1.15,
                            ticks: { callback: (v) => v + ' ml' }
                        }
                    }
                }
            });
        } catch (err) {
            console.error("Failed to parse Water Chart Payload:", err);
        }
    }

    // 3. Initialize General Dashboard Charts
    const chartDataEl = document.getElementById('chart-data');
    if (chartDataEl) {
        let chartData = null;
        try {
            chartData = JSON.parse(chartDataEl.textContent);
        } catch (e) {
            console.error("Failed to parse Dashboard Chart JSON data:", e);
            return;
        }

        if (!chartData) return;
        const labels = chartData.short_labels || ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

        // Steps Chart on Dashboard
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
                            ticks: { callback: (v) => v >= 1000 ? (v / 1000) + 'k' : v }
                        }
                    }
                }
            });
        }

        // Water Chart on Dashboard
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
                            ticks: { callback: (v) => v + ' ml' }
                        }
                    }
                }
            });
        }

        // Sleep Chart on Dashboard
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
                            ticks: { callback: (v) => v + 'h' }
                        }
                    }
                }
            });
        }
    }
});
