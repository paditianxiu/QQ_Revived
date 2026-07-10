package me.padi.qqlite.revived

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import me.padi.qqlite.revived.di.RevivedKoin
import me.padi.qqlite.revived.hooks.aio.AioComposeHook
import me.padi.qqlite.revived.hooks.common.AutoSizeHook
import me.padi.qqlite.revived.hooks.common.BaseHook
import me.padi.qqlite.revived.hooks.common.HostThemeHook
import me.padi.qqlite.revived.hooks.common.HostLongPressMenuHook
import me.padi.qqlite.revived.hooks.common.ModuleHookChain
import me.padi.qqlite.revived.hooks.common.MsfAliveJobHook
import me.padi.qqlite.revived.hooks.common.OrientationHook
import me.padi.qqlite.revived.hooks.common.PhoneResourcesHook
import me.padi.qqlite.revived.hooks.common.StatusBarHook
import me.padi.qqlite.revived.hooks.common.VideoReportHiddenApiHook
import me.padi.qqlite.revived.hooks.common.WatchUiHook
import me.padi.qqlite.revived.hooks.home.HomeComposeHook
import java.lang.reflect.Constructor
import java.lang.reflect.Method

class ModuleMainKt : XposedModule() {
    companion object {
        internal const val TAG = "QQRevived"
        private const val TARGET_PACKAGE = "com.tencent.qqlite"
    }

    private var frameworkHooksInstalled = false
    private var appHooksInstalled = false
    private var cachedModuleContext: Context? = null

    private val frameworkHooks: List<BaseHook> = listOf(
        MsfAliveJobHook,
        PhoneResourcesHook,
        HostThemeHook,
        StatusBarHook,
        OrientationHook
    )
    private val earlyAppHooks: List<BaseHook> = listOf(
    )
    private val appHooks: List<BaseHook> = listOf(
        VideoReportHiddenApiHook,
        HomeComposeHook,
        AioComposeHook,
        HostLongPressMenuHook,
        AutoSizeHook,
        WatchUiHook
    )

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(Log.INFO, TAG, "Loaded in ${param.processName}, API $apiVersion")
    }

    override fun onHotReloading(param: HotReloadingParam): Boolean {
        return true
    }

    override fun onHotReloaded(param: HotReloadedParam) {
        param.oldHookHandles.forEach { it.unhook() }
        frameworkHooksInstalled = false
        appHooksInstalled = false
        cachedModuleContext = null
        (frameworkHooks + earlyAppHooks + appHooks).forEach { it.reset() }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != TARGET_PACKAGE) return
        RevivedKoin.ensureStarted()
        installFrameworkHooks()
        installEarlyAppHooks(param.defaultClassLoader)
        installAppHooks(param.defaultClassLoader)

    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != TARGET_PACKAGE) return
        RevivedKoin.ensureStarted()
        installFrameworkHooks()
        installEarlyAppHooks(param.classLoader)
        installAppHooks(param.classLoader)
        logHook(Log.INFO, "Hooks installed for ${param.packageName}")
    }

    private fun installFrameworkHooks() {
        if (frameworkHooksInstalled) return
        frameworkHooksInstalled = true

        frameworkHooks.forEach { it.install(this, null) }
    }

    private fun installEarlyAppHooks(classLoader: ClassLoader) {
        earlyAppHooks.forEach { it.install(this, classLoader) }
    }

    private fun installAppHooks(classLoader: ClassLoader) {
        if (appHooksInstalled) return
        appHooksInstalled = true

        appHooks.forEach { it.install(this, classLoader) }
    }

    internal fun intercept(method: Method, block: ModuleHookChain.() -> Any?) {
        hook(method).intercept { chain ->
            val hookChain = object : ModuleHookChain {
                override val args: List<Any?>
                    get() = chain.args

                override val thisObject: Any?
                    get() = chain.thisObject

                override fun proceed(): Any? {
                    return chain.proceed()
                }

                override fun proceed(args: Array<Any?>): Any? {
                    return chain.proceed(args)
                }
            }
            hookChain.block()
        }
    }

    internal fun hookConstructor(constructor: Constructor<*>, block: ModuleHookChain.() -> Any?) {
        hook(constructor).intercept { chain ->
            val hookChain = object : ModuleHookChain {
                override val args: List<Any?>
                    get() = chain.args

                override val thisObject: Any?
                    get() = chain.thisObject

                override fun proceed(): Any? {
                    return chain.proceed()
                }

                override fun proceed(args: Array<Any?>): Any? {
                    return chain.proceed(args)
                }
            }
            hookChain.block()
        }
    }

    internal fun moduleContext(hostContext: Context): Context {
        cachedModuleContext?.let { return it }

        return hostContext.createPackageContext(
            moduleApplicationInfo.packageName,
            Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
        ).also {
            cachedModuleContext = it
        }
    }

    internal fun logHook(level: Int, message: String, throwable: Throwable? = null) {
        if (throwable == null) {
            log(level, TAG, message)
        } else {
            log(level, TAG, message, throwable)
        }
    }
}
