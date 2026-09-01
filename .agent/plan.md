# Project Plan

Build a complete production-quality Android application: AI-DR (AI/ML-Based Intelligent Dead Reckoning System for Seamless Navigation).
Key features:
1. Smartphone telemetry collection (Accel, Gyro, Mag, Barometer, GNSS).
2. Standardized CSV dataset export saved in Downloads/AI-DR/.
3. Sensor quality validation, monitoring & diagnostics.
4. Data preprocessing, gravity removal, filtering, coordinate transformation.
5. Sensor fusion orientation estimation (Roll, Pitch, Yaw/Heading via Complementary / EKF).
6. Activity detection (Stationary, Walking, Running).
7. Step detection & cadence tracking.
8. Step length estimation (configurable baseline model + ML interface).
9. Heading estimation (Magnetic, Fused, Relative).
10. Pedestrian Dead Reckoning (PDR) engine (Local coordinate system dx/dy and Lat/Lng conversion).
11. GNSS Ground Truth & drift/error evaluation (Horizontal error, RMSE, Max error).
12. GNSS-Denied simulation mode & re-correction.
13. Extended Kalman Filter (EKF) Sensor Fusion (GNSS + IMU + Barometer + PDR).
14. AI/ML architecture & interfaces (TF Lite / baseline fallbacks for Step Length, Heading Correction, Position Error Correction).
15. Training data exporter for ML models.
16. Interactive Map UI displaying GNSS, DR, and Fused trajectories, accuracy, and modes.
17. Diagnostic Screen, Controlled Experiment Mode, Settings screen.
18. Clean Architecture (Data, Domain, UI, Navigation, Utils), Kotlin, Jetpack Compose, ViewModels, Coroutines, Flow.
19. Unit tests & verification.

## Project Brief

# AI-DR (AI/ML-Based Intelligent Dead Reckoning System) - Project Brief

## Features

1. **Multi-Sensor Telemetry & Data Logging**: Real-time acquisition of device telemetry (Accelerometer, Gyroscope, Magnetometer, Barometer, GNSS) and standardized CSV dataset export saved to `Downloads/AI-DR/`.
2. **Sensor Fusion & Pedestrian Dead Reckoning (PDR)**: Real-time sensor fusion engine combining step detection, step length estimation, and orientation tracking (Roll, Pitch, Yaw via Complementary / EKF) for relative positioning.
3. **Interactive Trajectory Map**: Real-time visual comparison map plotting GNSS ground truth, Dead Reckoning (DR), and Fused trajectories alongside accuracy and drift evaluation metrics (RMSE, max error).
4. **GNSS-Denied Simulation & Diagnostics**: Toggleable GNSS-denied mode to simulate indoor/underground loss of satellite signal, evaluate re-correction capability, and monitor live sensor diagnostics.

## High-Level Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Adaptive Strategy**: Compose Material Adaptive library
- **Navigation**: Jetpack Navigation 3 (state-driven)
- **Architecture**: Clean Architecture (Data, Domain, UI) with ViewModel pattern
- **Asynchronous & Streaming**: Kotlin Coroutines & Flow (StateFlow / SharedFlow) for high-frequency sensor data pipelines
- **ML Inference**: TensorFlow Lite runtime (with baseline mathematical fallbacks for step length and heading estimation)
- **Hardware APIs**: Android `SensorManager` & `LocationManager` / Fused Location Provider

## Implementation Steps

### Task_1_CoreArchitectureAndTelemetryLogger: Implement core architecture, runtime permissions, SensorManager acquisition (Accel, Gyro, Mag, Baro), GNSS provider, quality diagnostics, timestamp monitoring, and CSV logging engine to Downloads/AI-DR.
- **Status:** COMPLETED
- **Updates:** Completed core architecture, runtime permissions, SensorCollector (Accel, Gyro, Mag, Barometer with active/stale/unavailable status detection), GnssCollector (FusedLocationProvider), CsvLogger (Channel-buffered writing to Downloads/AI-DR/ with strict schema), DataQualityValidator (timestamp monotonicity & stuck value checks), TelemetryService, and HomeScreen with navigation. Verified with assembleDebug and testDebugUnitTest.
- **Acceptance Criteria:**
  - Project builds successfully
  - Sensor and GNSS acquisition working continuously
  - Data quality diagnostics and monotonicity validation operational
  - CSV logging saves complete sensor telemetry to Downloads/AI-DR

### Task_2_PDREngineCore: Build Pedestrian Dead Reckoning (PDR) engine including orientation estimation (Complementary Filter/Euler angles), gravity filtering, activity detection, peak-detection step counter with adaptive thresholds, step length estimation, fused heading filter, and local metric/GPS coordinate transform.
- **Status:** COMPLETED
- **Updates:** Implemented complete PDR Engine Core: OrientationEstimator (Complementary filter, rotation matrix, device to world transformation, yaw/pitch/roll), AccelerationProcessor (gravity extraction & vertical acceleration isolation), ActivityClassifier (Stationary, Walking, Running), StepDetector (peak detection with adaptive thresholding & refractory window), StepLengthEstimator (Weinberg model with interface for ML), HeadingEstimator (fused gyro/mag with magnetic disturbance detector), PdrEngine (metric displacement dx/dy & WGS84 Lat/Lng conversion). Integrated with TelemetryRepository. Added 25 unit tests in src/test. Build assembleDebug and testDebugUnitTest passed 100%.
- **Acceptance Criteria:**
  - Orientation filter transforms device coordinates to navigation frame
  - Step detection correctly identifies steps and estimates step length
  - Fused heading estimation mitigates magnetic disturbances
  - PDR engine converts metric dx/dy steps into real-time Lat/Lng trajectory updates

### Task_3_EKFFusionAndMLArchitecture: Implement Extended Kalman Filter (EKF) combining GNSS, IMU, Barometer, and PDR. Add GNSS-denied simulation mode, real-time drift RMSE metrics, MLModel interface with TFLite integration shell, and dataset exporter.
- **Status:** COMPLETED
- **Updates:** Completed EKF Sensor Fusion (7-state Extended Kalman Filter with GNSS, IMU step updates, Barometer relative altitude, and ZUPT), GNSS-Denied Simulation Mode, Drift Metrics evaluator (Haversine error, RMSE, max error, trajectory re-correction), ML Model Architecture (interfaces, TFLite loader shell, baseline fallbacks), and ML Training Data Exporter (saved to Downloads/AI-DR/ml_training_*.csv). Integrated with TelemetryRepository. Added unit tests for EKF, Evaluator, ML, and Exporter. Total unit tests passing: 42. Build assembleDebug succeeded.
- **Acceptance Criteria:**
  - EKF sensor fusion operational, blending GNSS, IMU, Barometer and PDR estimates
  - GNSS-denied simulation mode calculates RMSE and maximum drift metrics
  - MLModel interface and dataset exporter ready for TFLite model integration

### Task_4_InteractiveUIScreens: Develop full Jetpack Compose multi-screen UI: Interactive Trajectory Map visualizing real-time GNSS vs DR vs Fused trajectories with accuracy indicators, Diagnostics Screen, Controlled Experiments Screen, and Settings Screen.
- **Status:** COMPLETED
- **Updates:** Developed complete Jetpack Compose UI suite: MapScreen (Interactive gesture Canvas with GNSS, PDR, and Fused trajectory rendering, GNSS-denied toggle, drift metrics RMSE panel, zoom/re-center controls), DiagnosticsScreen (Live sensor sample rate Hz, accuracy rating, latency, stuck/anomaly warnings, overall quality score), ExperimentsScreen (10 protocol benchmarks, metadata input, recording controls, summary reporting), SettingsScreen (Sampling rate selector, Weinberg step scaling factor calibration, EKF fusion toggle, ML toggle, trajectory history limit), and NavigationBar shell. All 46 unit tests passed and assembleDebug built successfully.
- **Acceptance Criteria:**
  - Interactive map visualizes GNSS, PDR, and Fused trajectories with clear color coding
  - Diagnostics screen displays real-time sensor sample rates, SNR, and data health
  - Controlled Experiments screen allows starting/stopping benchmark runs and viewing performance metrics
  - Settings screen configures PDR parameters, logging preferences, and GNSS simulation controls

### Task_5_RunAndVerify: Run unit tests, verify build stability, test end-to-end PDR/EKF pipeline and map UI, verify application stability without crashes, and report critical UI or logic issues.
- **Status:** COMPLETED
- **Updates:** Completed final quality gate check and end-to-end verification. App built successfully, passed all unit tests (46/46), launched and ran on device without crashes, verified live telemetry, CSV logger to Downloads/AI-DR, interactive canvas map with 3 trajectory lines (GNSS, PDR, Fused), GNSS-Denied simulation mode with drift RMSE metrics, diagnostics dashboard, experiments screen, settings controls, and Material 3 design system.
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - application stability confirmed, aligned with user requirements
- **Duration:** N/A

