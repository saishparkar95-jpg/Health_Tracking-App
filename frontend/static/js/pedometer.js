/**
 * HealthTrack AI - Real-Time Motion Pedometer & Live Step Tracker
 * Uses DeviceMotionEvent (Accelerometer) and peak-filtering to automatically count steps during walking sessions.
 */

class RealTimePedometer {
    constructor() {
        this.isActive = false;
        this.isPaused = false;
        this.sessionSteps = 0;
        this.sessionStartTime = null;
        this.elapsedSeconds = 0;
        this.timerInterval = null;
        
        // Motion sensor filtering variables
        this.lastStepTimestamp = 0;
        this.minStepIntervalMs = 280; // Debounce window (max ~215 steps/min)
        this.stepThreshold = 1.35;    // Dynamic peak magnitude threshold (m/s^2 above gravity)
        
        this.lastMagnitude = 9.8;
        this.prevMagnitude = 9.8;
        this.gravityFilter = { x: 0, y: 0, z: 9.8 };
        this.alpha = 0.85; // Low-pass filter weight for gravity estimation
        
        // Cadence tracking
        this.recentStepTimes = [];
        this.currentCadence = 0; // Steps per minute (SPM)

        // Simulation mode for desktop browsers
        this.isSimulating = false;
        this.simInterval = null;

        this.boundMotionHandler = this.handleDeviceMotion.bind(this);
    }

    init() {
        this.container = document.getElementById('liveWalkTrackerCard');
        this.stepDisplay = document.getElementById('liveWalkSteps');
        this.timerDisplay = document.getElementById('liveWalkTimer');
        this.cadenceDisplay = document.getElementById('liveWalkCadence');
        this.distanceDisplay = document.getElementById('liveWalkDistance');
        this.caloriesDisplay = document.getElementById('liveWalkCalories');
        this.statusBadge = document.getElementById('liveWalkStatusBadge');
        
        this.btnStart = document.getElementById('btnStartWalk');
        this.btnPause = document.getElementById('btnPauseWalk');
        this.btnFinish = document.getElementById('btnFinishWalk');
        this.btnSimulate = document.getElementById('btnSimulateWalk');
    }

    async requestSensorPermission() {
        if (typeof DeviceMotionEvent !== 'undefined' && typeof DeviceMotionEvent.requestPermission === 'function') {
            try {
                const response = await DeviceMotionEvent.requestPermission();
                return response === 'granted';
            } catch (err) {
                console.warn('DeviceMotionEvent permission error:', err);
                return false;
            }
        }
        // Permission not required on Android / standard browsers
        return true;
    }

    async startWalk() {
        this.init();
        if (this.isActive && !this.isPaused) return;

        if (this.isPaused) {
            this.isPaused = false;
            this.updateUI();
            this.startTimer();
            return;
        }

        const permissionGranted = await this.requestSensorPermission();
        if (!permissionGranted) {
            alert('Motion sensor permission is needed to automatically count steps while walking.');
        }

        this.isActive = true;
        this.isPaused = false;
        this.sessionSteps = 0;
        this.elapsedSeconds = 0;
        this.sessionStartTime = Date.now();
        this.recentStepTimes = [];
        this.lastStepTimestamp = 0;

        window.addEventListener('devicemotion', this.boundMotionHandler, false);
        this.startTimer();
        this.updateUI();

        if (navigator.vibrate) navigator.vibrate(80);
    }

    pauseWalk() {
        if (!this.isActive || this.isPaused) return;
        this.isPaused = true;
        clearInterval(this.timerInterval);
        this.updateUI();
    }

    startTimer() {
        clearInterval(this.timerInterval);
        this.timerInterval = setInterval(() => {
            if (this.isActive && !this.isPaused) {
                this.elapsedSeconds++;
                this.updateMetrics();
            }
        }, 1000);
    }

    handleDeviceMotion(event) {
        if (!this.isActive || this.isPaused) return;

        const acc = event.accelerationIncludingGravity || event.acceleration;
        if (!acc || acc.x === null) return;

        const x = acc.x || 0;
        const y = acc.y || 0;
        const z = acc.z || 0;

        // Isolate dynamic acceleration from static gravity using high-pass filter
        this.gravityFilter.x = this.alpha * this.gravityFilter.x + (1 - this.alpha) * x;
        this.gravityFilter.y = this.alpha * this.gravityFilter.y + (1 - this.alpha) * y;
        this.gravityFilter.z = this.alpha * this.gravityFilter.z + (1 - this.alpha) * z;

        const dynX = x - this.gravityFilter.x;
        const dynY = y - this.gravityFilter.y;
        const dynZ = z - this.gravityFilter.z;

        const magnitude = Math.sqrt(dynX * dynX + dynY * dynY + dynZ * dynZ);
        const now = Date.now();

        // Peak detection algorithm: local maximum that crosses the step threshold
        if (this.prevMagnitude > this.lastMagnitude && 
            this.prevMagnitude > magnitude && 
            this.prevMagnitude > this.stepThreshold) {
            
            const timeSinceLast = now - this.lastStepTimestamp;
            if (timeSinceLast >= this.minStepIntervalMs) {
                this.registerStep(now);
                this.lastStepTimestamp = now;
            }
        }

        this.lastMagnitude = this.prevMagnitude;
        this.prevMagnitude = magnitude;
    }

    registerStep(timestamp = Date.now()) {
        this.sessionSteps++;
        
        // Calculate Cadence (Steps per Minute)
        this.recentStepTimes.push(timestamp);
        // Keep steps from the last 10 seconds
        const tenSecAgo = timestamp - 10000;
        this.recentStepTimes = this.recentStepTimes.filter(t => t > tenSecAgo);
        
        if (this.recentStepTimes.length >= 2) {
            const timeSpanSec = (timestamp - this.recentStepTimes[0]) / 1000;
            if (timeSpanSec > 0) {
                this.currentCadence = Math.round((this.recentStepTimes.length / timeSpanSec) * 60);
            }
        }

        // Haptic feedback every 100 steps
        if (this.sessionSteps % 100 === 0 && navigator.vibrate) {
            navigator.vibrate([60, 40, 60]);
        }

        this.updateMetrics();
    }

    updateMetrics() {
        if (this.stepDisplay) this.stepDisplay.textContent = this.sessionSteps.toLocaleString();
        
        // Format Elapsed Time MM:SS or HH:MM:SS
        if (this.timerDisplay) {
            const hrs = Math.floor(this.elapsedSeconds / 3600);
            const mins = Math.floor((this.elapsedSeconds % 3600) / 60);
            const secs = this.elapsedSeconds % 60;
            const pad = (n) => String(n).padStart(2, '0');
            this.timerDisplay.textContent = hrs > 0 ? `${hrs}:${pad(mins)}:${pad(secs)}` : `${pad(mins)}:${pad(secs)}`;
        }

        // Distance: ~0.75m per average step
        const distanceKm = (this.sessionSteps * 0.00075).toFixed(2);
        if (this.distanceDisplay) this.distanceDisplay.textContent = `${distanceKm} km`;

        // Calories: ~0.042 kcal per step
        const calories = Math.round(this.sessionSteps * 0.042);
        if (this.caloriesDisplay) this.caloriesDisplay.textContent = `${calories} kcal`;

        // Cadence / SPM
        if (this.cadenceDisplay) {
            // Decay cadence if no recent step
            const timeSinceLast = Date.now() - this.lastStepTimestamp;
            if (timeSinceLast > 3000) this.currentCadence = 0;
            this.cadenceDisplay.textContent = `${this.currentCadence} spm`;
        }
    }

    updateUI() {
        if (!this.container) return;

        if (!this.isActive) {
            if (this.statusBadge) {
                this.statusBadge.textContent = 'Idle';
                this.statusBadge.className = 'status-badge badge-neutral';
            }
            if (this.btnStart) this.btnStart.style.display = 'inline-flex';
            if (this.btnPause) this.btnPause.style.display = 'none';
            if (this.btnFinish) this.btnFinish.style.display = 'none';
        } else if (this.isPaused) {
            if (this.statusBadge) {
                this.statusBadge.textContent = 'Paused';
                this.statusBadge.className = 'status-badge badge-warning';
            }
            if (this.btnStart) {
                this.btnStart.style.display = 'inline-flex';
                this.btnStart.innerHTML = '▶️ Resume Walk';
            }
            if (this.btnPause) this.btnPause.style.display = 'none';
            if (this.btnFinish) this.btnFinish.style.display = 'inline-flex';
        } else {
            if (this.statusBadge) {
                this.statusBadge.textContent = '🔴 Tracking Walk...';
                this.statusBadge.className = 'status-badge badge-live';
            }
            if (this.btnStart) this.btnStart.style.display = 'none';
            if (this.btnPause) this.btnPause.style.display = 'inline-flex';
            if (this.btnFinish) this.btnFinish.style.display = 'inline-flex';
        }
    }

    toggleSimulation() {
        if (this.isSimulating) {
            this.stopSimulation();
        } else {
            this.startSimulation();
        }
    }

    startSimulation() {
        if (!this.isActive) this.startWalk();
        this.isSimulating = true;
        if (this.btnSimulate) {
            this.btnSimulate.classList.add('active');
            this.btnSimulate.textContent = '⏹️ Stop Simulation';
        }
        
        // Simulate walking pace (~110 steps per minute = step every ~545ms)
        this.simInterval = setInterval(() => {
            if (this.isActive && !this.isPaused) {
                this.registerStep();
            }
        }, 545);
    }

    stopSimulation() {
        this.isSimulating = false;
        clearInterval(this.simInterval);
        if (this.btnSimulate) {
            this.btnSimulate.classList.remove('active');
            this.btnSimulate.textContent = '👟 Test Walk Simulation';
        }
    }

    async finishAndSync() {
        if (this.sessionSteps === 0) {
            if (confirm('No steps were recorded during this session. End session?')) {
                this.stopSession();
            }
            return;
        }

        const stepsToSync = this.sessionSteps;
        const confirmMsg = `Sync ${stepsToSync.toLocaleString()} steps to your daily step count?`;
        if (!confirm(confirmMsg)) return;

        this.stopSession();

        try {
            // POST to existing /steps/add endpoint
            const formData = new FormData();
            formData.append('steps', stepsToSync);

            const response = await fetch('/steps/add', {
                method: 'POST',
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            // Reload page to refresh daily progress circle and charts
            window.location.href = '/steps';
        } catch (err) {
            console.error('Error syncing steps:', err);
            // Fallback form submit
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = '/steps/add';
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = 'steps';
            input.value = stepsToSync;
            form.appendChild(input);
            document.body.appendChild(form);
            form.submit();
        }
    }

    stopSession() {
        this.isActive = false;
        this.isPaused = false;
        this.stopSimulation();
        clearInterval(this.timerInterval);
        window.removeEventListener('devicemotion', this.boundMotionHandler, false);
        this.updateUI();
    }
}

// Global pedometer instance
const livePedometer = new RealTimePedometer();

function startLiveWalk() {
    livePedometer.startWalk();
}

function pauseLiveWalk() {
    livePedometer.pauseWalk();
}

function finishLiveWalk() {
    livePedometer.finishAndSync();
}

function togglePedometerSimulation() {
    livePedometer.toggleSimulation();
}

// Auto-initialize on page load if container exists
document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('liveWalkTrackerCard')) {
        livePedometer.init();
    }
});
