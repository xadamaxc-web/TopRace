package com.toprace.tracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.toprace.tracker.db.AppDatabase
import com.toprace.tracker.db.StrategySettings
import com.toprace.tracker.logic.Learner
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.toprace.tracker.ui.SetupScreen
import com.toprace.tracker.ui.SetupState

class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    private lateinit var wm: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private var view: ComposeView? = null
    private var fullHeight = 0
    private val prefs by lazy { getSharedPreferences("toprace", MODE_PRIVATE) }
    private val state = SetupState()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        runCatching { startAsForeground() }
        prefs.getString("state", null)?.let { state.restore(it) }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        addOverlay()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        // persist the in-progress round (survives process death) and follow minimize/restore
        scope.launch { snapshotFlow { state.toJson() }.collect { prefs.edit().putString("state", it).apply() } }
        scope.launch { snapshotFlow { state.minimized }.collect { applyMinimized(it) } }
    }

    private fun addOverlay() {
        val dm = resources.displayMetrics
        val h = (dm.heightPixels * 0.45).toInt()
        fullHeight = h
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            y = (dm.heightPixels * 0.05).toInt()
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        }

        val sprites = SpriteSlicer(this)
        val db = AppDatabase.get(this)
        scope.launch {
            state.settings = withContext(Dispatchers.IO) { db.settingsDao().get() } ?: StrategySettings()
        }
        scope.launch { db.statsDao().all().collect { state.stats = it } }
        scope.launch { db.roundDao().last(ROUNDS_WINDOW).collect { state.rounds = it } }

        val cv = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    SetupScreen(
                        state = state,
                        sprites = sprites,
                        onSaveRound = { r, road, cars, winner ->
                            scope.launch(Dispatchers.IO) { Learner.saveRound(db, r, road, cars, winner) }
                        },
                        onSaveSettings = { s ->
                            state.settings = s
                            scope.launch(Dispatchers.IO) { db.settingsDao().save(s) }
                        },
                        onDrag = { dy -> moveBy(dy.toInt()) },
                        onClose = { stopSelf() },
                    )
                }
            }
        }
        view = cv
        wm.addView(cv, params)
    }

    private fun applyMinimized(min: Boolean) {
        val v = view ?: return
        params.height = if (min) (44 * resources.displayMetrics.density).toInt() else fullHeight
        wm.updateViewLayout(v, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun moveBy(dy: Int) {
        val screenH = resources.displayMetrics.heightPixels
        params.y = (params.y + dy).coerceIn(0, screenH - params.height)
        view?.let { wm.updateViewLayout(it, params) }
    }

    private fun startAsForeground() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Overlay", NotificationManager.IMPORTANCE_LOW),
        )
        val n = Notification.Builder(this, CHANNEL)
            .setContentTitle("TOP RACE Tracker")
            .setContentText("Overlay running")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        view?.let { runCatching { wm.removeView(it) } }
        view = null
        store.clear()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "overlay"
        private const val ROUNDS_WINDOW = 200
    }
}
