package com.toprace.tracker

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private var canDraw by mutableStateOf(false)
    private var observerStatus by mutableStateOf("Observer not running")
    private var diagnosticLog by mutableStateOf("No observer events yet. Start the screen observer to begin logging.")

    private val screenCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val service = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_DATA, result.data)
            }
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
            observerStatus = "Observer starting — approve screen sharing"
        } else observerStatus = "Screen capture permission not granted"
    }

    private val storagePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            if (ok) runExport() else Toast.makeText(this, "Storage permission denied", Toast.LENGTH_LONG).show()
        }

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { startOverlay() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("TOP RACE Tracker", style = MaterialTheme.typography.headlineSmall)
                        if (!canDraw) {
                            Text("Overlay permission needed")
                            Button(onClick = {
                                startActivity(
                                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                                )
                            }) { Text("Grant overlay permission") }
                        } else {
                            Button(onClick = { requestStart() }) { Text("Start overlay") }
                            OutlinedButton(onClick = { stopService(Intent(this@MainActivity, OverlayService::class.java)) }) {
                                Text("Stop overlay")
                            }
                        }
                        HorizontalDivider()
                        Text("S9b · Five-car screen observer", style = MaterialTheme.typography.titleMedium)
                        Text(observerStatus)
                        Button(onClick = { requestScreenCapture() }) { Text("Start screen observer") }
                        OutlinedButton(onClick = {
                            stopService(Intent(this@MainActivity, ScreenCaptureService::class.java))
                            observerStatus = "Observer stopped"
                        }) { Text("Stop screen observer") }
                        OutlinedButton(onClick = { refreshDiagnosticLog() }) { Text("Refresh diagnostic log") }
                        Card(
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                                Text("OBSERVER DIAGNOSTICS", style = MaterialTheme.typography.titleSmall)
                                Text(diagnosticLog, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        OutlinedButton(onClick = { exportCsv() }) { Text("[ EXPORT CSV ]") }
                    }
                }
            }
        }
    }

    private fun exportCsv() {
        // Android 9 and below need the legacy storage permission; 10+ writes via MediaStore, no permission
        if (Build.VERSION.SDK_INT < 29 &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else runExport()
    }

    private fun runExport() {
        lifecycleScope.launch {
            val msg = runCatching { CsvExporter.export(this@MainActivity) }.fold(
                onSuccess = { "Saved ${it.rounds} rounds to Download/${it.roundsFile}" },
                onFailure = { "Export failed: ${it.message}" },
            )
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        canDraw = Settings.canDrawOverlays(this)
        refreshDiagnosticLog()
    }

    private fun refreshDiagnosticLog() {
        diagnosticLog = getSharedPreferences("toprace_observer", MODE_PRIVATE)
            .getString("diagnostic_log", "No observer events yet. Start the screen observer to begin logging.")
            .orEmpty()
    }

    private fun requestScreenCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCapture.launch(manager.createScreenCaptureIntent())
    }

    private fun requestStart() {
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else startOverlay()
    }

    private fun startOverlay() {
        startForegroundService(Intent(this, OverlayService::class.java))
    }
}
