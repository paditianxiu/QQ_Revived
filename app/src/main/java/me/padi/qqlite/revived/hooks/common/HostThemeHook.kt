package me.padi.qqlite.revived.hooks.common

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Window
import me.padi.qqlite.revived.ModuleMainKt

internal object HostThemeHook : BaseHook() {
    private val targetActivities = setOf(
        "com.tencent.qqnt.watch.app.JumpActivity",
        "com.tencent.qqnt.watch.mainframe.MainActivity",
        "com.tencent.qqnt.watch.mainframe.UpdateActivity",
        "com.tencent.qqnt.watch.runtime.container.PluginDefaultProxyActivity",
        "com.tencent.qqnt.watch.runtime.container.PluginDefaultProxySingleInsActivity"
    )

    override fun install(module: ModuleMainKt, classLoader: ClassLoader?) {
        module.intercept(
            Activity::class.java.getDeclaredMethod(
                "onCreate", Bundle::class.java
            )
        ) {
            val activity = thisObject as? Activity ?: return@intercept proceed()
            applyMaterialHostTheme(module, activity)
            proceed()
        }
    }

    private fun applyMaterialHostTheme(module: ModuleMainKt, activity: Activity) {
        if (activity.javaClass.name !in targetActivities) return

        runCatching {
            val resources = activity.resources
            val packageName = activity.packageName
            val materialFullscreenThemeId =
                resources.getIdentifier("MaterialFullscreenTheme", "style", packageName)
            val appCompatNoActionBarThemeId =
                resources.getIdentifier("Theme_AppCompat_Light_NoActionBar", "style", packageName)
            val targetThemeId = when {
                materialFullscreenThemeId != 0 -> materialFullscreenThemeId
                appCompatNoActionBarThemeId != 0 -> appCompatNoActionBarThemeId
                else -> 0
            }
            if (targetThemeId == 0) return

            activity.setTheme(targetThemeId)
            activity.requestWindowFeature(Window.FEATURE_NO_TITLE)
            activity.actionBar?.hide()
        }.onFailure {
            module.logHook(Log.WARN, "Host material theme override failed", it)
        }
    }
}
