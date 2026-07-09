package me.padi.qqlite.revived.hooks.common

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import me.padi.qqlite.revived.ModuleMainKt

internal object OrientationHook : BaseHook() {
    private val ORIENTATION_MODE = OrientationMode.FORCE_PORTRAIT

    override fun install(module: ModuleMainKt, classLoader: ClassLoader?) {
        module.intercept(
            Activity::class.java.getDeclaredMethod(
                "setRequestedOrientation", Integer.TYPE
            )
        ) {
            val requestedOrientation = args.getOrNull(0) as? Int ?: return@intercept proceed()
            proceed(arrayOf<Any?>(resolveOrientation(requestedOrientation)))
        }

        module.intercept(
            Activity::class.java.getDeclaredMethod(
                "onCreate", Bundle::class.java
            )
        ) {
            val result = proceed()
            (thisObject as? Activity)?.clearForcedPortraitOrientation()
            result
        }

        module.intercept(Activity::class.java.getDeclaredMethod("onResume")) {
            val result = proceed()
            (thisObject as? Activity)?.clearForcedPortraitOrientation()
            result
        }
    }

    private fun Activity.clearForcedPortraitOrientation() {
        val target = resolveOrientation(requestedOrientation)
        if (requestedOrientation != target) {
            setRequestedOrientation(target)
        }
    }

    private fun resolveOrientation(requestedOrientation: Int): Int {
        return when (ORIENTATION_MODE) {
            OrientationMode.FREE -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            OrientationMode.FORCE_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            OrientationMode.FORCE_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            OrientationMode.HOST -> if (requestedOrientation.isForcedPortraitOrientation()) {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                requestedOrientation
            }
        }
    }

    private fun Int.isForcedPortraitOrientation(): Boolean {
        return when (this) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT -> true
            else -> false
        }
    }

    private enum class OrientationMode {
        HOST,
        FREE,
        FORCE_LANDSCAPE,
        FORCE_PORTRAIT
    }
}
