package com.example.dead_reckoning.utils

object Constants {
    const val DIRECTORY_NAME = "AI-DR"
    const val CSV_HEADER = "timestamp_ns,elapsed_realtime_ns,wall_time_ms,accel_x,accel_y,accel_z,gyro_x,gyro_y,gyro_z,mag_x,mag_y,mag_z,pressure,relative_altitude,latitude,longitude,altitude,gps_speed,gps_accuracy,gps_bearing,sensor_accuracy,activity,step_detected,roll_rad,pitch_rad,yaw_rad,fused_heading_deg,step_count,step_length_m,pdr_x_m,pdr_y_m,pdr_lat,pdr_lng,magnetic_anomaly"

    // Default sensor sampling delay in microseconds (20,000 us = 50 Hz)
    const val SENSOR_DELAY_US = 20_000

    // Sensor inactivity timeout in milliseconds
    const val SENSOR_TIMEOUT_MS = 3000L
}
