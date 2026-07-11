package me.padi.qqlite.revived.hooks.qav

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.padi.qqlite.revived.ModuleMainKt
import me.padi.qqlite.revived.compose.screens.qav.QavUiController
import me.padi.qqlite.revived.compose.screens.qav.QavUiState
import java.lang.ref.WeakReference

internal class QavBinding(
    private val module: ModuleMainKt,
    activity: Activity,
    private val hostViews: HostViews
) : QavUiController {
    private val activityRef = WeakReference(activity)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastMicEnabled = true
    private var micStateDirty = false
    private var lastCameraEnabled = false
    private var cameraStateDirty = false
    private var lastIsVideoCall = false
    private val _uiState = MutableStateFlow(QavUiState())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            val activity = activityRef.get() ?: return
            hostViews.hideOriginalOverlayViews()
            _uiState.value = readUiState()
            mainHandler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    override val uiState: StateFlow<QavUiState>
        get() = _uiState

    fun start() {
        hostViews.hideOriginalOverlayViews()
        _uiState.value = readUiState()
        refreshNow()
    }

    fun stop() {
        mainHandler.removeCallbacks(refreshRunnable)
        QavRuntimeStore.clearOutgoingCall()
    }

    override fun toggleMic() {
        val clicked = hostViews.micButton?.performClick() == true
        if (!clicked) {
            module.logHook(Log.WARN, "QAV mic click fallback failed")
        } else {
            lastMicEnabled = !lastMicEnabled
            micStateDirty = true
            _uiState.value = _uiState.value.copy(micEnabled = lastMicEnabled)
        }
        refreshSoon()
    }

    override fun toggleCamera() {
        val clicked = hostViews.cameraButton?.performClick() == true
        if (!clicked) {
            activityRef.get()?.invokeNoArg("h")
        }
        lastCameraEnabled = !lastCameraEnabled
        cameraStateDirty = true
        _uiState.value = _uiState.value.copy(cameraEnabled = lastCameraEnabled)
        refreshSoon()
    }

    override fun hangUp() {
        val clicked = hostViews.hangupButton?.performClick() == true
        if (!clicked) {
            activityRef.get()?.invokeNoArg("d")
        }
        QavRuntimeStore.clearOutgoingCall()
        refreshSoon()
    }

    override fun obtainVideoHostView(): View? {
        val activity = activityRef.get() ?: return null
        val isVideoCall = resolveVideoCallState(activity)
        return hostViews.glRootView?.takeIf { isVideoCall }?.also { view ->
            view.visibility = View.VISIBLE
            view.alpha = 1f
        }
    }

    override fun obtainAvatarHostView(): View? {
        hostViews.avatarView?.visibility = View.VISIBLE
        return hostViews.avatarView
    }

    private fun refreshNow() {
        mainHandler.removeCallbacks(refreshRunnable)
        mainHandler.post(refreshRunnable)
    }

    private fun refreshSoon() {
        mainHandler.removeCallbacks(refreshRunnable)
        mainHandler.postDelayed(refreshRunnable, QUICK_REFRESH_DELAY_MS)
    }

    private fun resolveVideoCallState(activity: Activity): Boolean {
        return when {
            activity.readBooleanField("isOpenVideoMsg") -> true
            QavRuntimeStore.peekOutgoingCall() != null -> QavRuntimeStore.peekOutgoingCall() == true
            cameraStateDirty -> true
            lastCameraEnabled -> true
            lastIsVideoCall -> true
            else -> false
        }
    }

    private fun readUiState(): QavUiState {
        val activity = activityRef.get() ?: return QavUiState(statusText = "通话已结束")
        val peerName = activity.readStringField("nickName")
            ?.takeIf(String::isNotBlank)
            ?: hostViews.nicknameView?.text?.toString()?.takeIf(String::isNotBlank)
            ?: "通话中"
        val peerId = activity.readStringField("peerId").orEmpty()
        val isVideoCall = resolveVideoCallState(activity)
        val isConnected = activity.readBooleanField("mIsConnected")
        val isAccept = activity.readBooleanField("mIsAccept")
        val cameraEnabled = when {
            cameraStateDirty -> lastCameraEnabled
            else -> activity.readBooleanField("mCameraEnable")
        }
        val micEnabled = when {
            micStateDirty -> lastMicEnabled
            else -> activity.findFieldValue("mQavBinder")
                ?.invokeBooleanNoArg("p")
                ?.let { !it }
                ?: lastMicEnabled
        }
        lastMicEnabled = micEnabled
        lastCameraEnabled = cameraEnabled
        lastIsVideoCall = isVideoCall
        micStateDirty = false
        cameraStateDirty = false
        val hostStatusText = hostViews.timeTickView?.safeText()
        val statusText = when {
            !hostStatusText.isNullOrBlank() -> hostStatusText
            isConnected -> if (isVideoCall) "视频通话中" else "语音通话中"
            isAccept -> "等待对方接通"
            isVideoCall -> "正在发起视频通话..."
            else -> "正在发起语音通话..."
        }
        return QavUiState(
            peerName = peerName,
            peerId = peerId,
            statusText = statusText,
            isVideoCall = isVideoCall,
            isConnected = isConnected,
            micEnabled = micEnabled,
            cameraEnabled = cameraEnabled
        )
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 500L
        const val QUICK_REFRESH_DELAY_MS = 150L
    }
}

internal data class HostViews(
    val glRootView: View?,
    val avatarView: View?,
    val nicknameView: TextView?,
    val timeTickView: TextView?,
    val micButton: View?,
    val cameraButton: View?,
    val hangupButton: View?,
    val backgroundView: View?,
    val loadingView: View?,
    val buttonContainer: View?
) {
    fun hideOriginalOverlayViews() {
        backgroundView?.visibility = View.GONE
        loadingView?.visibility = View.GONE
        nicknameView?.visibility = View.GONE
        timeTickView?.visibility = View.GONE
        buttonContainer?.visibility = View.GONE
    }
}

private fun Activity.readBooleanField(name: String): Boolean {
    return findFieldValue(name) as? Boolean ?: false
}

private fun Activity.readStringField(name: String): String? {
    return findFieldValue(name)?.toString()?.takeIf { it.isNotBlank() }
}

private fun Any.findFieldValue(name: String): Any? {
    var current: Class<*>? = javaClass
    while (current != null && current != Any::class.java) {
        runCatching {
            return current.getDeclaredField(name).apply { isAccessible = true }.get(this)
        }
        current = current.superclass
    }
    return null
}

private fun Any.invokeNoArg(name: String): Any? {
    return runCatching {
        javaClass.methods.firstOrNull { method ->
            method.name == name && method.parameterTypes.isEmpty()
        }?.invoke(this)
    }.getOrNull()
}

private fun Any.invokeBooleanNoArg(name: String): Boolean? {
    return invokeNoArg(name) as? Boolean
}

private fun TextView.safeText(): String? {
    return text?.toString()?.trim()?.takeIf { it.isNotBlank() }
}
