package me.padi.qqlite.revived.hooks.common

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import me.padi.qqlite.revived.ModuleMainKt
import java.lang.reflect.Modifier

internal object StatusBarHook : BaseHook() {
    override fun install(module: ModuleMainKt, classLoader: ClassLoader?) {
        hookActivityStatusBarPolicy(module)
        hookWindowStatusBarPolicy(module)
    }

    private fun hookActivityStatusBarPolicy(module: ModuleMainKt) {
        module.intercept(
            Activity::class.java.getDeclaredMethod(
                "onCreate", Bundle::class.java
            )
        ) {
            val result = proceed()
            (thisObject as? Activity)?.let(::applyStatusBarPolicy)
            result
        }

        module.intercept(Activity::class.java.getDeclaredMethod("onResume")) {
            val result = proceed()
            (thisObject as? Activity)?.let(::applyStatusBarPolicy)
            result
        }

        module.intercept(
            Activity::class.java.getDeclaredMethod(
                "onWindowFocusChanged",
                java.lang.Boolean.TYPE
            )
        ) {
            val result = proceed()
            if (args[0] as? Boolean == true) {
                (thisObject as? Activity)?.let(::applyStatusBarPolicy)
            }
            result
        }
    }

    @SuppressLint("PrivateApi")
    @Suppress("DEPRECATION")
    private fun hookWindowStatusBarPolicy(module: ModuleMainKt) {
        runCatching {
            val setFlagsMethod =
                Window::class.java.getDeclaredMethod("setFlags", Integer.TYPE, Integer.TYPE)
            if (!Modifier.isAbstract(setFlagsMethod.modifiers)) {
                module.intercept(setFlagsMethod) {
                    val flags =
                        (args[0] as Int) and WindowManager.LayoutParams.FLAG_FULLSCREEN.inv()
                    val mask = args[1] as Int
                    proceed(arrayOf<Any?>(flags, mask))
                }
            }
        }.onFailure {
            module.logHook(Log.WARN, "Window#setFlags hook skipped", it)
        }

        runCatching {
            val addFlagsMethod = Window::class.java.getDeclaredMethod("addFlags", Integer.TYPE)
            if (!Modifier.isAbstract(addFlagsMethod.modifiers)) {
                module.intercept(addFlagsMethod) {
                    val flags = (args[0] as Int) and WindowManager.LayoutParams.FLAG_FULLSCREEN.inv()
                    proceed(arrayOf<Any?>(flags))
                }
            }
        }.onFailure {
            module.logHook(Log.WARN, "Window#addFlags hook skipped", it)
        }

        runCatching {
            val phoneWindowClass = Class.forName("com.android.internal.policy.PhoneWindow")
            val setStatusBarColorMethod =
                phoneWindowClass.getDeclaredMethod("setStatusBarColor", Integer.TYPE)
            if (!Modifier.isAbstract(setStatusBarColorMethod.modifiers)) {
                module.intercept(setStatusBarColorMethod) {
                    proceed(arrayOf<Any?>(Color.TRANSPARENT))
                }
            }
        }.onFailure {
            module.logHook(Log.WARN, "PhoneWindow#setStatusBarColor hook skipped", it)
        }

        runCatching {
            val phoneWindowClass = Class.forName("com.android.internal.policy.PhoneWindow")
            val setNavigationBarColorMethod =
                phoneWindowClass.getDeclaredMethod("setNavigationBarColor", Integer.TYPE)
            if (!Modifier.isAbstract(setNavigationBarColorMethod.modifiers)) {
                module.intercept(setNavigationBarColorMethod) {
                    proceed(arrayOf<Any?>(Color.TRANSPARENT))
                }
            }
        }.onFailure {
            module.logHook(Log.WARN, "PhoneWindow#setNavigationBarColor hook skipped", it)
        }

        runCatching {
            val setDecorFitsSystemWindowsMethod =
                Window::class.java.getDeclaredMethod(
                    "setDecorFitsSystemWindows",
                    java.lang.Boolean.TYPE
                )
            if (!Modifier.isAbstract(setDecorFitsSystemWindowsMethod.modifiers)) {
                module.intercept(setDecorFitsSystemWindowsMethod) {
                    proceed(arrayOf<Any?>(false))
                }
            }
        }.onFailure {
            module.logHook(Log.WARN, "Window#setDecorFitsSystemWindows hook skipped", it)
        }

        runCatching {
            val setNavigationContrastMethod =
                Window::class.java.getDeclaredMethod(
                    "setNavigationBarContrastEnforced",
                    java.lang.Boolean.TYPE
                )
            if (!Modifier.isAbstract(setNavigationContrastMethod.modifiers)) {
                module.intercept(setNavigationContrastMethod) {
                    proceed(arrayOf<Any?>(false))
                }
            }
        }.onFailure {
            module.logHook(Log.WARN, "Window#setNavigationBarContrastEnforced hook skipped", it)
        }

        runCatching {
            val setStatusContrastMethod =
                Window::class.java.getDeclaredMethod(
                    "setStatusBarContrastEnforced",
                    java.lang.Boolean.TYPE
                )
            if (!Modifier.isAbstract(setStatusContrastMethod.modifiers)) {
                module.intercept(setStatusContrastMethod) {
                    proceed(arrayOf<Any?>(false))
                }
            }
        }.onFailure {
            module.logHook(Log.WARN, "Window#setStatusBarContrastEnforced hook skipped", it)
        }

        runCatching {
            module.intercept(
                View::class.java.getDeclaredMethod(
                    "setSystemUiVisibility",
                    Integer.TYPE
                )
            ) {
                val visibility = (args[0] as Int).toVisibleEdgeToEdgeSystemBarsVisibility()
                proceed(arrayOf<Any?>(visibility))
            }
        }.onFailure {
            module.logHook(Log.WARN, "View#setSystemUiVisibility hook skipped", it)
        }
    }

    fun applyStatusBarPolicy(activity: Activity) {
        applyStatusBarPolicy(activity.window)
    }

    @Suppress("DEPRECATION")
    private fun applyStatusBarPolicy(window: Window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        val decorView = window.decorView
        decorView.systemUiVisibility =
            decorView.systemUiVisibility.toVisibleEdgeToEdgeSystemBarsVisibility()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun Int.toVisibleEdgeToEdgeSystemBarsVisibility(): Int {
        val hiddenSystemBarFlags =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LOW_PROFILE

        return (this and hiddenSystemBarFlags.inv()) or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
}
