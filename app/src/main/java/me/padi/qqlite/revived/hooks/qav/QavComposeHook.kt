package me.padi.qqlite.revived.hooks.qav

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import me.padi.qqlite.revived.ModuleMainKt
import me.padi.qqlite.revived.compose.screens.qav.QavCallScreen
import me.padi.qqlite.revived.hooks.aio.createFullComposeLayoutParams
import me.padi.qqlite.revived.hooks.common.BaseHook
import me.padi.qqlite.revived.utils.addHostComposeView
import me.padi.qqlite.revived.utils.removeHostComposeView

internal object QavComposeHook : BaseHook() {
    private const val CALL_ACTIVITY_CLASS = "com.tencent.activitys.QQNTC2CWatchActivity"
    private const val WATCH_ACTIVITY_CLASS = "com.tencent.qqnt.watch.ui.kit.WatchActivity"
    private const val TOP_GESTURE_LAYOUT_CLASS = "com.tencent.mobileqq.activity.fling.TopGestureLayout"
    private const val COMPOSE_TAG = "QQRevived.Qav.Compose"
    private const val COMPOSE_BINDING_KEY = 0x71766101
    private const val STATE_BINDING_KEY = 0x71766102
    private var installed = false

    override fun reset() {
        installed = false
    }

    override fun install(module: ModuleMainKt, classLoader: ClassLoader?) {
        if (installed) return
        val targetClassLoader = requireClassLoader(classLoader)
        runCatching {
            val activityClass = targetClassLoader.loadClass(CALL_ACTIVITY_CLASS)
            val watchActivityClass = targetClassLoader.loadClass(WATCH_ACTIVITY_CLASS)
            module.intercept(
                Activity::class.java.getDeclaredMethod("setContentView", View::class.java)
            ) {
                val result = proceed()
                val activity = thisObject as? Activity
                if (activity?.javaClass == activityClass) {
                    runCatching {
                        debugLog("setContentView hit activity=${activity.javaClass.name}")
                        scheduleBindCompose(module, activity)
                    }.onFailure {
                        module.logHook(Log.WARN, "QAV compose setContentView bind failed", it)
                        debugLog("setContentView bind failed: ${it.stackTraceToString()}")
                    }
                }
                result
            }
            module.intercept(
                activityClass.getDeclaredMethod("onCreate", Bundle::class.java)
            ) {
                val result = proceed()
                runCatching {
                    val activity = thisObject as? Activity ?: return@runCatching
                    debugLog("onCreate hit activity=${activity.javaClass.name}")
                    scheduleBindCompose(module, activity)
                }.onFailure {
                    module.logHook(Log.WARN, "QAV compose bind failed", it)
                    debugLog("onCreate bind failed: ${it.stackTraceToString()}")
                }
                result
            }
            module.intercept(
                watchActivityClass.getDeclaredMethod("onStart")
            ) {
                val result = proceed()
                val activity = thisObject as? Activity
                if (activity?.javaClass == activityClass) {
                    runCatching {
                        debugLog("onStart hit activity=${activity.javaClass.name}")
                        scheduleBindCompose(module, activity)
                    }.onFailure {
                        module.logHook(Log.WARN, "QAV compose start bind failed", it)
                        debugLog("onStart bind failed: ${it.stackTraceToString()}")
                    }
                }
                result
            }
            module.intercept(
                activityClass.getDeclaredMethod("onResume")
            ) {
                val result = proceed()
                runCatching {
                    val activity = thisObject as? Activity ?: return@runCatching
                    debugLog("onResume hit activity=${activity.javaClass.name}")
                    scheduleBindCompose(module, activity)
                }.onFailure {
                    module.logHook(Log.WARN, "QAV compose resume bind failed", it)
                    debugLog("onResume bind failed: ${it.stackTraceToString()}")
                }
                result
            }
            module.intercept(
                activityClass.getDeclaredMethod("onWindowFocusChanged", Boolean::class.javaPrimitiveType)
            ) {
                val result = proceed()
                runCatching {
                    val activity = thisObject as? Activity ?: return@runCatching
                    debugLog("onWindowFocusChanged hit activity=${activity.javaClass.name}")
                    scheduleBindCompose(module, activity)
                }.onFailure {
                    module.logHook(Log.WARN, "QAV compose focus bind failed", it)
                    debugLog("onWindowFocusChanged bind failed: ${it.stackTraceToString()}")
                }
                result
            }
            module.intercept(
                activityClass.getDeclaredMethod("onDestroy")
            ) {
                val activity = thisObject as? Activity
                runCatching {
                    releaseCompose(activity)
                }.onFailure {
                    module.logHook(Log.WARN, "QAV compose release failed", it)
                }
                proceed()
            }
            installed = true
            module.logHook(Log.INFO, "QAV compose hook installed")
        }.onFailure {
            module.logHook(Log.WARN, "QAV compose hook skipped", it)
            debugLog("hook install skipped: ${it.stackTraceToString()}")
        }
    }

    private fun scheduleBindCompose(module: ModuleMainKt, activity: Activity) {
        debugLog("scheduleBindCompose start activity=${activity.javaClass.name}")
        bindCompose(module, activity)
        val contentRoot = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        contentRoot.post { bindCompose(module, activity) }
        contentRoot.postDelayed({ bindCompose(module, activity) }, 120L)
        contentRoot.postDelayed({ bindCompose(module, activity) }, 360L)
        contentRoot.postDelayed({ bindCompose(module, activity) }, 800L)
    }

    private fun bindCompose(module: ModuleMainKt, activity: Activity) {
        val mountRoot = activity.resolveMountRoot()
        if (mountRoot == null) {
            debugLog("bindCompose skip mountRoot=null activity=${activity.javaClass.name}")
            debugLog(dumpActivityRoots(activity))
            return
        }
        debugLog(
            "bindCompose mountRoot=${mountRoot.javaClass.name} id=${mountRoot.safeEntryName(activity)} " +
                "childCount=${mountRoot.childCount} existingCompose=${mountRoot.findViewWithTag<View>(COMPOSE_TAG) != null}"
        )
        val existing = mountRoot.getTag(STATE_BINDING_KEY) as? QavBinding
        val binding = existing ?: QavBinding(
            module = module,
            activity = activity,
            hostViews = resolveHostViews(activity)
        ).also {
            mountRoot.setTag(STATE_BINDING_KEY, it)
        }
        binding.start()
        if (existing != null && mountRoot.findViewWithTag<View>(COMPOSE_TAG) != null) {
            hideOriginalHostContent(activity, mountRoot)
            mountRoot.keepComposeQavOverlayOnTop()
            return
        }
        mountRoot.removeHostComposeView(COMPOSE_TAG, COMPOSE_BINDING_KEY)
        mountRoot.addHostComposeView(
            tag = COMPOSE_TAG,
            bindingKey = COMPOSE_BINDING_KEY,
            layoutParamsFactory = { createFullComposeLayoutParams() },
            lifecycleAnchor = mountRoot,
            wrapInContainer = false,
            configure = {
                isClickable = true
                isFocusable = true
                isFocusableInTouchMode = true
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                alpha = 1f
                elevation = 128f
                translationZ = 128f
            }
        ) {
            QavCallScreen(binding)
        }
        hideOriginalHostContent(activity, mountRoot)
        debugLog(
            "bindCompose added compose=${mountRoot.findViewWithTag<View>(COMPOSE_TAG)?.javaClass?.name} " +
                "tree=\n${mountRoot.dumpViewTree(activity)}"
        )
        mountRoot.keepComposeQavOverlayOnTop()
        mountRoot.post { mountRoot.keepComposeQavOverlayOnTop() }
        mountRoot.postDelayed({ mountRoot.keepComposeQavOverlayOnTop() }, 300L)
    }

    private fun releaseCompose(activity: Activity?) {
        val mountRoot = activity?.resolveMountRoot()
            ?: return
        (mountRoot.getTag(STATE_BINDING_KEY) as? QavBinding)?.stop()
        mountRoot.removeHostComposeView(COMPOSE_TAG, COMPOSE_BINDING_KEY)
        mountRoot.setTag(STATE_BINDING_KEY, null)
    }

    private fun resolveHostViews(activity: Activity): HostViews {
        return HostViews(
            glRootView = activity.findHostView("av_video_glview"),
            avatarView = activity.findHostView("avatar"),
            nicknameView = activity.findHostView("nickname") as? TextView,
            timeTickView = activity.findHostView("time_tick") as? TextView,
            micButton = activity.findHostView("switch_micro"),
            cameraButton = activity.findHostView("switch_camera"),
            hangupButton = activity.findHostView("finish"),
            backgroundView = activity.findHostView("ivBg"),
            loadingView = activity.findHostView("loading_view"),
            buttonContainer = activity.findHostView("button_container")
        )
    }

    private fun Activity.findHostView(name: String): View? {
        val id = resources.getIdentifier(name, "id", packageName)
        if (id == 0) return null
        return findViewById(id)
    }

    private fun Activity.resolveMountRoot(): ViewGroup? {
        val decorView = window?.decorView as? ViewGroup
        val topGestureLayout = decorView?.findFirstDescendantByClassName(TOP_GESTURE_LAYOUT_CLASS)
        if (topGestureLayout != null) {
            debugLog("resolveMountRoot hit TopGestureLayout childCount=${topGestureLayout.childCount}")
            return topGestureLayout
        }
        val contentRoot = findViewById<ViewGroup>(android.R.id.content)
        if (contentRoot != null) {
            debugLog("resolveMountRoot fallback android.R.id.content childCount=${contentRoot.childCount}")
        }
        return contentRoot
            ?: (findHostView("root") as? ViewGroup)
    }

    private fun hideOriginalHostContent(activity: Activity, mountRoot: ViewGroup) {
        val hostRoot = activity.findHostView("root")
        if (hostRoot != null && hostRoot !== mountRoot) {
            hostRoot.alpha = 0f
            hostRoot.visibility = View.INVISIBLE
            hostRoot.isClickable = false
            hostRoot.isFocusable = false
            hostRoot.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
        for (index in 0 until mountRoot.childCount) {
            val child = mountRoot.getChildAt(index)
            if (child.tag == COMPOSE_TAG) continue
            if (hostRoot != null && child === hostRoot) continue
            child.alpha = 0f
            child.visibility = View.INVISIBLE
            child.isClickable = false
            child.isFocusable = false
            child.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
    }

    private fun ViewGroup.findFirstDescendantByClassName(className: String): ViewGroup? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.javaClass.name == className && child is ViewGroup) {
                return child
            }
            val nested = (child as? ViewGroup)?.findFirstDescendantByClassName(className)
            if (nested != null) {
                return nested
            }
        }
        return null
    }

    private fun ViewGroup.keepComposeQavOverlayOnTop() {
        findViewWithTag<View>(COMPOSE_TAG)?.let { composeView ->
            composeView.visibility = View.VISIBLE
            composeView.isEnabled = true
            composeView.isClickable = true
            composeView.isFocusable = true
            composeView.isFocusableInTouchMode = true
            composeView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            composeView.alpha = 1f
            composeView.elevation = 128f
            composeView.translationZ = 128f
            composeView.bringToFront()
            invalidate()
            debugLog(
                "keepComposeQavOverlayOnTop compose=${composeView.javaClass.name} parent=${javaClass.name} " +
                    "index=${indexOfChild(composeView)} childCount=$childCount"
            )
        }
    }

    private fun dumpActivityRoots(activity: Activity): String {
        val decorView = activity.window?.decorView as? ViewGroup
        val contentRoot = activity.findViewById<ViewGroup>(android.R.id.content)
        return buildString {
            appendLine("activity=${activity.javaClass.name}")
            appendLine("decor=${decorView?.javaClass?.name} childCount=${decorView?.childCount ?: -1}")
            appendLine("content=${contentRoot?.javaClass?.name} childCount=${contentRoot?.childCount ?: -1}")
            decorView?.let {
                appendLine("decorTree=")
                append(it.dumpViewTree(activity))
            }
        }
    }

    private fun View.dumpViewTree(activity: Activity, depth: Int = 0, maxDepth: Int = 4): String {
        val indent = "  ".repeat(depth)
        val base = buildString {
            append(indent)
            append(javaClass.name)
            append(" id=")
            append(safeEntryName(activity))
            append(" vis=")
            append(visibility)
            append(" alpha=")
            append(alpha)
            append(" tag=")
            append(tag)
        }
        if (this !is ViewGroup || depth >= maxDepth) {
            return base
        }
        return buildString {
            appendLine(base)
            for (index in 0 until childCount) {
                appendLine(getChildAt(index).dumpViewTree(activity, depth + 1, maxDepth))
            }
        }.trimEnd()
    }

    private fun View.safeEntryName(activity: Activity): String {
        val viewId = id
        if (viewId == View.NO_ID || viewId == 0) return "no_id"
        return runCatching {
            activity.resources.getResourceEntryName(viewId)
        }.getOrElse { viewId.toString() }
    }

    private fun debugLog(message: String) {
        Log.e("QQRevived", message)
    }
}
