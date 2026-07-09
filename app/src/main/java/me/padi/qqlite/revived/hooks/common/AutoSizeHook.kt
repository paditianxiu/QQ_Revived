package me.padi.qqlite.revived.hooks.common

import android.app.Activity
import android.app.Application
import android.util.Log
import me.padi.qqlite.revived.ModuleMainKt

internal object AutoSizeHook : BaseHook() {
    override fun install(module: ModuleMainKt, classLoader: ClassLoader?) {
        val targetClassLoader = requireClassLoader(classLoader)
        val autoSizeConfigClass =
            targetClassLoader.findTargetClass("me.jessyan.autosize.AutoSizeConfig")
        val autoSizeClass = targetClassLoader.findTargetClass("me.jessyan.autosize.AutoSize")
        val autoAdaptStrategyClass =
            targetClassLoader.findTargetClass("me.jessyan.autosize.AutoAdaptStrategy")

        hookAutoSizeConfig(module, autoSizeConfigClass, autoAdaptStrategyClass)
        hookAutoSize(module, autoSizeClass)
    }

    private fun hookAutoSizeConfig(
        module: ModuleMainKt,
        configClass: Class<*>,
        autoAdaptStrategyClass: Class<*>
    ) {
        module.intercept(configClass.getDeclaredMethod("getDesignWidthInDp")) {
            PhoneProfile.SHORT_WIDTH_DP
        }

        module.intercept(configClass.getDeclaredMethod("getDesignHeightInDp")) {
            PhoneProfile.LONG_HEIGHT_DP
        }

        module.intercept(configClass.getDeclaredMethod("setDesignWidthInDp", Integer.TYPE)) {
            proceed(arrayOf<Any?>(PhoneProfile.SHORT_WIDTH_DP))
        }

        module.intercept(
            configClass.getDeclaredMethod(
                "setDesignHeightInDp", Integer.TYPE
            )
        ) {
            proceed(arrayOf<Any?>(PhoneProfile.LONG_HEIGHT_DP))
        }

        module.intercept(configClass.getDeclaredMethod("init", Application::class.java)) {
            forceAutoSizeConfig(module, configClass, proceed())
        }

        module.intercept(
            configClass.getDeclaredMethod(
                "init",
                Application::class.java,
                java.lang.Boolean.TYPE
            )
        ) {
            forceAutoSizeConfig(module, configClass, proceed())
        }

        module.intercept(
            configClass.getDeclaredMethod(
                "init", Application::class.java, java.lang.Boolean.TYPE, autoAdaptStrategyClass
            )
        ) {
            forceAutoSizeConfig(module, configClass, proceed())
        }

        runCatching {
            val metadataClassLoader =
                configClass.classLoader ?: error("AutoSizeConfig classLoader is null")
            val metadataRunnableClass =
                metadataClassLoader.findTargetClass("me.jessyan.autosize.AutoSizeConfig\$2")
            module.intercept(metadataRunnableClass.getDeclaredMethod("run")) {
                val result = proceed()
                forceAutoSizeConfig(
                    module, configClass, configClass.getDeclaredMethod("getInstance").invoke(null)
                )
                result
            }
        }.onFailure {
            module.logHook(Log.WARN, "AutoSize metadata hook skipped", it)
        }
    }

    private fun hookAutoSize(module: ModuleMainKt, autoSizeClass: Class<*>) {
        module.intercept(
            autoSizeClass.getDeclaredMethod(
                "autoConvertDensity",
                Activity::class.java,
                java.lang.Float.TYPE,
                java.lang.Boolean.TYPE
            )
        ) {
            val activity = args[0] as Activity
            val baseOnWidth = args[2] as Boolean
            val designDp =
                if (baseOnWidth) PhoneProfile.SHORT_WIDTH_DP else PhoneProfile.LONG_HEIGHT_DP
            val result = proceed(arrayOf<Any?>(activity, designDp.toFloat(), baseOnWidth))
            PhoneResourcesHook.applyPhoneResources(activity.resources)
            result
        }
    }

    private fun forceAutoSizeConfig(
        module: ModuleMainKt,
        configClass: Class<*>,
        config: Any?
    ): Any? {
        if (config == null) return null

        runCatching {
            configClass.getDeclaredMethod("setBaseOnWidth", java.lang.Boolean.TYPE)
                .invoke(config, true)
            configClass.getDeclaredMethod("setDesignWidthInDp", Integer.TYPE)
                .invoke(config, PhoneProfile.SHORT_WIDTH_DP)
            configClass.getDeclaredMethod("setDesignHeightInDp", Integer.TYPE)
                .invoke(config, PhoneProfile.LONG_HEIGHT_DP)
        }.onFailure {
            module.logHook(Log.WARN, "Failed to force AutoSize config", it)
        }

        return config
    }
}
