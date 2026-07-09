package me.padi.qqlite.revived.hooks.common

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import me.padi.qqlite.revived.ModuleMainKt

internal object WatchUiHook : BaseHook() {
    private const val WATCH_FRAGMENT_CLASS = "com.tencent.qqnt.watch.ui.kit.WatchFragment"
    private const val WATCH_FRAGMENT_BACKGROUND_FIELD = "d"
    private var installed = false

    override fun reset() {
        installed = false
    }

    override fun install(module: ModuleMainKt, classLoader: ClassLoader?) {
        if (installed) return
        val targetClassLoader = requireClassLoader(classLoader)
        module.logHook(Log.INFO, targetClassLoader.toString())
        runCatching {
            val fixUiCallbackClass =
                targetClassLoader.findTargetClass("com.tencent.qqnt.watch.ui.kit.FixUICallback")
            module.intercept(
                fixUiCallbackClass.getDeclaredMethod(
                    "onActivityCreated", Activity::class.java, Bundle::class.java
                )
            ) {
                val activity = args[0] as? Activity
                activity?.let {
                    PhoneResourcesHook.applyPhoneResources(it.resources)
                    StatusBarHook.applyStatusBarPolicy(it)
                }
                null
            }
        }.onFailure {
            module.logHook(Log.WARN, "Watch UI fix hook skipped", it)
        }

        runCatching {
            val watchFragmentClass = targetClassLoader.findTargetClass(WATCH_FRAGMENT_CLASS)
            val backgroundField = watchFragmentClass
                .getDeclaredField(WATCH_FRAGMENT_BACKGROUND_FIELD)
                .apply { isAccessible = true }
            module.intercept(
                watchFragmentClass.getDeclaredMethod(
                    "onCreateView",
                    LayoutInflater::class.java,
                    ViewGroup::class.java,
                    Bundle::class.java
                )
            ) {
                val result = proceed()
                runCatching {
                    val fragment = thisObject ?: return@runCatching
                    val imageView = backgroundField.get(fragment) as? ImageView ?: return@runCatching
                    imageView.setColorFilter(Color.WHITE)
                }.onFailure {
                    module.logHook(Log.WARN, "WatchFragment background image tint failed", it)
                }
                result
            }
            module.logHook(Log.INFO, "WatchFragment background image tint hook installed")
        }.onFailure {
            module.logHook(Log.WARN, "WatchFragment background image tint hook skipped", it)
        }
        installed = true
    }
}
