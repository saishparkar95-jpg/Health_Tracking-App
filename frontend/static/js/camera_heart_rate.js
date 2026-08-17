/**
 * HealthTrack AI - Camera Photoplethysmography (PPG) Heart Rate Monitor
 * Measures pulse/BPM by analyzing light absorption variations in fingertip capillary blood vessels.
 */

class CameraHeartRateMonitor {
    constructor() {
        this.videoStream = null;
        this.videoTrack = null;
        this.videoElement = null;
        this.sampleCanvas = null;
        this.sampleCtx = null;
        this.waveformCanvas = null;
        this.waveformCtx = null;
        
        this.isMeasuring = false;
        this.torchOn = false;
        this.animationFrameId = null;
        this.waveformAnimId = null;
        
        // Signal processing buffers
        this.rawValues = [];
        this.timestamps = [];
        this.filteredSignal = [];
        this.waveformData = [];
        this.maxWaveformPoints = 120;
        
        // Measurement metrics
        this.bpmHistory = [];
        this.currentBpm = null;
        this.stableBpm = null;
        this.fingerDetected = false;
        this.measurementStartTime = null;
        this.requiredDurationSec = 15;
        this.measurementProgress = 0;
        
        // Smoothing filter constants
        this.lastPeakTime = 0;
        this.movingAvgWindow = 5;
    }

    init() {
        this.modal = document.getElementById('cameraHeartModal');
        this.videoElement = document.getElementById('heartCameraVideo');
        this.sampleCanvas = document.createElement('canvas');
        this.sampleCanvas.width = 48;
        this.sampleCanvas.height = 48;
        this.sampleCtx = this.sampleCanvas.getContext('2d', { willReadFrequently: true });
        
        this.waveformCanvas = document.getElementById('ecgWaveformCanvas');
        if (this.waveformCanvas) {
            this.waveformCtx = this.waveformCanvas.getContext('2d');
            this.resizeWaveformCanvas();
            window.addEventListener('resize', () => this.resizeWaveformCanvas());
        }

        this.statusText = document.getElementById('cameraHeartStatus');
        this.bpmDisplay = document.getElementById('cameraLiveBpm');
        this.progressRing = document.getElementById('cameraProgressRing');
        this.progressText = document.getElementById('cameraProgressPct');
        this.fingerGuide = document.getElementById('fingerGuideBox');
        this.torchBtn = document.getElementById('cameraTorchBtn');
        this.saveBtn = document.getElementById('cameraSaveBpmBtn');
    }

    resizeWaveformCanvas() {
        if (!this.waveformCanvas) return;
        const rect = this.waveformCanvas.getBoundingClientRect();
        this.waveformCanvas.width = rect.width * (window.devicePixelRatio || 1);
        this.waveformCanvas.height = 80 * (window.devicePixelRatio || 1);
        if (this.waveformCtx) {
            this.waveformCtx.scale(window.devicePixelRatio || 1, window.devicePixelRatio || 1);
        }
    }

    async start() {
        this.init();
        if (!this.modal) return;
        
        this.modal.style.display = 'flex';
        this.resetState();
        this.updateStatus('Requesting camera access...', 'neutral');

        try {
            // Prefer back camera (environment) with flashlight capability
            const constraints = {
                video: {
                    facingMode: { ideal: 'environment' },
                    width: { ideal: 320 },
                    height: { ideal: 240 },
                    frameRate: { ideal: 30, min: 20 }
                }
            };

            this.videoStream = await navigator.mediaDevices.getUserMedia(constraints);
            this.videoElement.srcObject = this.videoStream;
            await this.videoElement.play();

            // Check and enable torch if supported
            this.videoTrack = this.videoStream.getVideoTracks()[0];
            await this.tryEnableTorch(true);

            this.isMeasuring = true;
            this.measurementStartTime = null;
            this.updateStatus('Place your fingertip gently over the camera lens & flash', 'waiting');
            
            this.processFrames();
            this.drawWaveformLoop();
        } catch (err) {
            console.error('Camera access error:', err);
            this.updateStatus('Camera permission required. Please allow camera access in your browser.', 'error');
        }
    }

    async tryEnableTorch(enable) {
        if (!this.videoTrack) return;
        try {
            const capabilities = this.videoTrack.getCapabilities ? this.videoTrack.getCapabilities() : {};
            if (capabilities.torch) {
                await this.videoTrack.applyConstraints({
                    advanced: [{ torch: enable }]
                });
                this.torchOn = enable;
                if (this.torchBtn) {
                    this.torchBtn.style.display = 'inline-flex';
                    this.torchBtn.innerHTML = enable ? '💡 Torch ON' : '🔦 Torch OFF';
                }
            } else if (this.torchBtn) {
                this.torchBtn.style.display = 'none';
            }
        } catch (e) {
            console.warn('Torch constraint not supported on this device:', e);
        }
    }

    async toggleTorch() {
        await this.tryEnableTorch(!this.torchOn);
    }

    resetState() {
        this.rawValues = [];
        this.timestamps = [];
        this.filteredSignal = [];
        this.waveformData = [];
        this.bpmHistory = [];
        this.currentBpm = null;
        this.stableBpm = null;
        this.fingerDetected = false;
        this.measurementStartTime = null;
        this.measurementProgress = 0;
        this.lastPeakTime = 0;

        if (this.bpmDisplay) this.bpmDisplay.textContent = '--';
        if (this.progressText) this.progressText.textContent = '0%';
        this.updateProgressRing(0);
        if (this.saveBtn) this.saveBtn.style.display = 'none';
        if (this.fingerGuide) this.fingerGuide.classList.remove('detected');
    }

    stop() {
        this.isMeasuring = false;
        if (this.animationFrameId) {
            cancelAnimationFrame(this.animationFrameId);
            this.animationFrameId = null;
        }
        if (this.waveformAnimId) {
            cancelAnimationFrame(this.waveformAnimId);
            this.waveformAnimId = null;
        }
        if (this.videoStream) {
            this.videoStream.getTracks().forEach(track => track.stop());
            this.videoStream = null;
            this.videoTrack = null;
        }
        if (this.modal) {
            this.modal.style.display = 'none';
        }
    }

    processFrames() {
        if (!this.isMeasuring) return;

        if (this.videoElement.readyState >= 2) {
            // Draw current video frame to downscaled sample canvas
            this.sampleCtx.drawImage(this.videoElement, 0, 0, this.sampleCanvas.width, this.sampleCanvas.height);
            const frame = this.sampleCtx.getImageData(0, 0, this.sampleCanvas.width, this.sampleCanvas.height);
            const data = frame.data;
            const length = data.length;

            let totalRed = 0;
            let totalGreen = 0;
            let totalBlue = 0;
            const pixelCount = length / 4;

            for (let i = 0; i < length; i += 4) {
                totalRed += data[i];
                totalGreen += data[i + 1];
                totalBlue += data[i + 2];
            }

            const avgRed = totalRed / pixelCount;
            const avgGreen = totalGreen / pixelCount;
            const avgBlue = totalBlue / pixelCount;

            // Fingertip detection check:
            // When fingertip covers the illuminated lens, red channel dominates and green/blue are deeply absorbed.
            const isRedDominant = (avgRed > 85) && (avgRed > (avgGreen * 1.35)) && (avgRed > (avgBlue * 1.35));
            const now = performance.now();

            if (isRedDominant) {
                if (!this.fingerDetected) {
                    this.fingerDetected = true;
                    this.measurementStartTime = now;
                    this.updateStatus('Finger detected! Measuring pulse. Please hold steady...', 'measuring');
                    if (this.fingerGuide) this.fingerGuide.classList.add('detected');
                }

                // Red light absorption signal
                const rawSignal = avgRed;
                this.rawValues.push(rawSignal);
                this.timestamps.push(now);

                // Keep rolling buffer of last 150 frames (~5 sec)
                if (this.rawValues.length > 150) {
                    this.rawValues.shift();
                    this.timestamps.shift();
                }

                this.analyzePulseSignal(now);
                this.updateMeasurementProgress(now);
            } else {
                if (this.fingerDetected) {
                    this.fingerDetected = false;
                    this.updateStatus('Fingertip moved. Cover the camera lens completely.', 'waiting');
                    if (this.fingerGuide) this.fingerGuide.classList.remove('detected');
                    // Slow decay progress on disconnect
                    this.measurementProgress = Math.max(0, this.measurementProgress - 0.5);
                    this.updateProgressRing(this.measurementProgress);
                }
            }
        }

        this.animationFrameId = requestAnimationFrame(() => this.processFrames());
    }

    analyzePulseSignal(now) {
        if (this.rawValues.length < 30) return;

        // Bandpass Filter: compute short moving average and long moving average
        const shortWindow = 3;
        const longWindow = 25;

        const count = this.rawValues.length;
        let shortSum = 0;
        for (let i = count - shortWindow; i < count; i++) shortSum += this.rawValues[i];
        const shortAvg = shortSum / shortWindow;

        let longSum = 0;
        for (let i = count - longWindow; i < count; i++) longSum += this.rawValues[i];
        const longAvg = longSum / longWindow;

        const bandpassVal = shortAvg - longAvg;
        this.filteredSignal.push(bandpassVal);
        if (this.filteredSignal.length > 100) this.filteredSignal.shift();

        // Feed waveform for canvas rendering
        this.waveformData.push(bandpassVal);
        if (this.waveformData.length > this.maxWaveformPoints) this.waveformData.shift();

        // Peak detection (systolic wave pulse)
        const currentVal = bandpassVal;
        const prevVal = this.filteredSignal[this.filteredSignal.length - 2] || 0;
        const prev2Val = this.filteredSignal[this.filteredSignal.length - 3] || 0;

        // Check if prevVal was a local peak above dynamic threshold
        if (prevVal > prev2Val && prevVal > currentVal && prevVal > 0.4) {
            const timeSinceLastPeak = now - this.lastPeakTime;
            
            // Plausible human heart rate intervals (300ms = 200 BPM, 1500ms = 40 BPM)
            if (timeSinceLastPeak >= 320 && timeSinceLastPeak <= 1500) {
                const instantBpm = Math.round(60000 / timeSinceLastPeak);
                this.lastPeakTime = now;
                
                // Add to history and calculate trimmed average
                this.bpmHistory.push(instantBpm);
                if (this.bpmHistory.length > 10) this.bpmHistory.shift();

                const sorted = [...this.bpmHistory].sort((a, b) => a - b);
                const medianBpm = sorted[Math.floor(sorted.length / 2)];

                this.currentBpm = medianBpm;
                if (this.bpmDisplay) {
                    this.bpmDisplay.textContent = this.currentBpm;
                }
                
                // Trigger pulse animation
                const pulseIcon = document.getElementById('cameraHeartIcon');
                if (pulseIcon) {
                    pulseIcon.classList.remove('pulse-beat');
                    void pulseIcon.offsetWidth; // trigger reflow
                    pulseIcon.classList.add('pulse-beat');
                }
            } else if (timeSinceLastPeak > 1500) {
                this.lastPeakTime = now; // reset timeout
            }
        }
    }

    updateMeasurementProgress(now) {
        if (!this.measurementStartTime) return;
        const elapsedSec = (now - this.measurementStartTime) / 1000;
        this.measurementProgress = Math.min(100, Math.round((elapsedSec / this.requiredDurationSec) * 100));

        this.updateProgressRing(this.measurementProgress);
        if (this.progressText) this.progressText.textContent = `${this.measurementProgress}%`;

        if (this.measurementProgress >= 100 && this.bpmHistory.length >= 4) {
            this.completeMeasurement();
        }
    }

    updateProgressRing(percent) {
        if (!this.progressRing) return;
        const radius = 54;
        const circumference = 2 * Math.PI * radius;
        const offset = circumference - (percent / 100) * circumference;
        this.progressRing.style.strokeDasharray = `${circumference} ${circumference}`;
        this.progressRing.style.strokeDashoffset = offset;
    }

    completeMeasurement() {
        this.isMeasuring = false;
        // Average recent valid BPM readings
        const sum = this.bpmHistory.reduce((a, b) => a + b, 0);
        this.stableBpm = Math.round(sum / this.bpmHistory.length) || this.currentBpm || 72;

        this.updateStatus(`🎉 Measurement Complete! Pulse: ${this.stableBpm} BPM`, 'success');
        if (this.bpmDisplay) this.bpmDisplay.textContent = this.stableBpm;

        if (this.saveBtn) {
            this.saveBtn.style.display = 'inline-flex';
            this.saveBtn.focus();
        }

        // Haptic feedback if supported
        if (navigator.vibrate) navigator.vibrate([100, 50, 100]);
    }

    drawWaveformLoop() {
        if (!this.waveformCtx || !this.waveformCanvas) return;
        
        const ctx = this.waveformCtx;
        const w = this.waveformCanvas.width / (window.devicePixelRatio || 1);
        const h = 80;

        ctx.clearRect(0, 0, w, h);

        // Draw background grid lines (medical monitor style)
        ctx.strokeStyle = 'rgba(255, 255, 255, 0.05)';
        ctx.lineWidth = 1;
        for (let x = 0; x < w; x += 20) {
            ctx.beginPath();
            ctx.moveTo(x, 0);
            ctx.lineTo(x, h);
            ctx.stroke();
        }
        for (let y = 0; y < h; y += 20) {
            ctx.beginPath();
            ctx.moveTo(0, y);
            ctx.lineTo(w, y);
            ctx.stroke();
        }

        if (this.waveformData.length > 1) {
            ctx.beginPath();
            ctx.strokeStyle = '#f43f5e';
            ctx.lineWidth = 2.5;
            ctx.lineCap = 'round';
            ctx.lineJoin = 'round';
            ctx.shadowColor = '#f43f5e';
            ctx.shadowBlur = 8;

            const step = w / this.maxWaveformPoints;
            const startX = w - (this.waveformData.length * step);

            // Find min/max for dynamic vertical scaling
            let maxAmp = 1.5;
            for (let v of this.waveformData) {
                if (Math.abs(v) > maxAmp) maxAmp = Math.abs(v);
            }

            for (let i = 0; i < this.waveformData.length; i++) {
                const x = startX + i * step;
                // Invert so peaks point up
                const normalized = (this.waveformData[i] / maxAmp);
                const y = (h / 2) - (normalized * (h * 0.38));
                if (i === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            }
            ctx.stroke();
            ctx.shadowBlur = 0; // reset
        }

        if (this.isMeasuring || this.modal.style.display !== 'none') {
            this.waveformAnimId = requestAnimationFrame(() => this.drawWaveformLoop());
        }
    }

    updateStatus(message, state) {
        if (!this.statusText) return;
        this.statusText.textContent = message;
        this.statusText.className = `camera-heart-status status-${state}`;
    }

    saveCurrentMeasurement() {
        const bpm = this.stableBpm || this.currentBpm;
        if (!bpm) return;

        // Auto-fill into manual modal or directly submit via form
        const formInput = document.getElementById('inline-bpm-val');
        const modalInput = document.getElementById('modal-heart-bpm');
        const form = document.getElementById('inline-heart-form');

        if (modalInput) modalInput.value = bpm;
        if (formInput) formInput.value = bpm;

        this.stop();

        // Submit to record reading
        if (form) {
            form.submit();
        } else {
            openHeartModal();
        }
    }
}

// Global instance
const cameraHeartTracker = new CameraHeartRateMonitor();

function openCameraHeartModal() {
    cameraHeartTracker.start();
}

function closeCameraHeartModal() {
    cameraHeartTracker.stop();
}

function toggleCameraTorch() {
    cameraHeartTracker.toggleTorch();
}

function saveCameraHeartBpm() {
    cameraHeartTracker.saveCurrentMeasurement();
}
