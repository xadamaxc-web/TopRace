package com.toprace.tracker

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.resume
import java.nio.ByteBuffer
import java.util.Locale

/** Screen 1 observer prototype. Frames are OCR-processed in memory and never written to disk. */
class ScreenCaptureService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var busy = false
    private var lastSignature = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startCaptureNotification()
        logEvent("Observer service started")
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data: Intent? = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_DATA)
        }
        if (resultCode != Activity.RESULT_OK || data == null) {
            logEvent("ERROR: Screen-capture permission missing or cancelled")
            stopSelf(); return START_NOT_STICKY
        }
        logEvent("Screen-capture permission received")
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mgr.getMediaProjection(resultCode, data)
        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { stopSelf() }
        }, android.os.Handler(mainLooper))
        runCatching { beginVirtualDisplay() }.onSuccess {
            logEvent("Virtual display started; waiting for game frames")
        }.onFailure {
            logEvent("ERROR: Capture display failed: ${it.message ?: it.javaClass.simpleName}")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun beginVirtualDisplay() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        val density = resources.displayMetrics.densityDpi
        reader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
        reader?.setOnImageAvailableListener({ source ->
            val image = try { source.acquireLatestImage() } catch (_: Exception) { null } ?: return@setOnImageAvailableListener
            if (busy) { image.close(); return@setOnImageAvailableListener }
            busy = true
            scope.launch {
                try {
                    val bitmap = imageToBitmap(image)
                    image.close()
                    val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitText()
                    bitmap.recycle()
                    consumeText(result)
                } catch (e: Exception) {
                    logEvent("ERROR: Frame/OCR processing failed: ${e.message ?: e.javaClass.simpleName}")
                    runCatching { image.close() }
                } finally { busy = false }
            }
        }, null)
        display = projection?.createVirtualDisplay(
            "TopRaceObserver", metrics.widthPixels, metrics.heightPixels, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, null
        )
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val padded = Bitmap.createBitmap(image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(buffer)
        val cropped = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
        padded.recycle()
        return cropped
    }

    private fun consumeText(raw: String) {
        val upper = raw.uppercase(Locale.ROOT)
        val cars = listOf("THUNDER", "EAGLE", "WHALE", "DRAGON", "POWER")
            .filter { Regex("\\b${it}\\b").containsMatchIn(upper) }
        val roads = listOf("MOUNTAIN", "POTHOLES", "BUMPY", "DESERT", "EXPRESSWAY", "HIGHWAY")
            .filter { Regex("\\b${it}\\b").containsMatchIn(upper) }
        val screen = if (cars.size >= 3) "SCREEN 1 · CAR SELECTION" else "SCREEN NOT CONFIRMED"
        val confidence = if (cars.size == 5) "5/5 car names recognized" else "${cars.size}/5 car names recognized"
        val signature = "$screen|$cars|$roads|$confidence"
        if (signature != lastSignature) {
            lastSignature = signature
            logEvent("OCR: $confidence; cars=${if (cars.isEmpty()) "none" else cars.joinToString()}; roads=${if (roads.isEmpty()) "none" else roads.joinToString()}")
            getSharedPreferences("toprace_observer", MODE_PRIVATE).edit()
                .putString("last_screen", screen).putString("last_cars", cars.joinToString(","))
                .putString("last_roads", roads.joinToString(",")).putString("last_status", confidence)
                .putBoolean("last_valid", cars.size == 5).apply()
            updateNotification("$screen — $confidence${if (roads.isNotEmpty()) " · Road: ${roads.joinToString()}" else ""}")
        }
    }

    private fun logEvent(message: String) {
        val prefs = getSharedPreferences("toprace_observer", MODE_PRIVATE)
        val old = prefs.getString("diagnostic_log", "").orEmpty()
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(java.util.Date())
        val updated = (old.lines().filter { it.isNotBlank() } + "[$timestamp] $message").takeLast(40)
        prefs.edit().putString("diagnostic_log", updated.joinToString("\n"))
            .putString("last_event", message).apply()
        updateNotification(message.take(150))
    }

    private fun startCaptureNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Screen observer", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("TOP RACE · Observer").setContentText("Starting local screen recognition…")
            .setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(42, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(42, n)
    }

    private fun updateNotification(message: String) {
        val n = Notification.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("TOP RACE · Observer").setContentText(message).setOngoing(true).build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(42, n)
    }

    override fun onDestroy() {
        logEvent("Observer service stopped")
        display?.release(); reader?.close(); projection?.stop(); recognizer.close(); scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_RESULT_CODE = "capture_result_code"
        const val EXTRA_DATA = "capture_data"
        private const val CHANNEL = "screen_observer"
    }
}

private suspend fun com.google.android.gms.tasks.Task<com.google.mlkit.vision.text.Text>.awaitText(): String =
    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { if (continuation.isActive) continuation.resume(it.text) }
        addOnFailureListener { if (continuation.isActive) continuation.resumeWith(Result.failure(it)) }
    }
