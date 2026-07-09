package me.padi.qqlite.revived.hooks.common

import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import me.padi.qqlite.revived.ModuleMainKt
import kotlin.math.roundToInt

internal object PhoneResourcesHook : BaseHook() {
    private val reentrant = ThreadLocal.withInitial { false }
    private val resourcesMetricsField by lazy {
        runCatching {
            Resources::class.java.getDeclaredField("mMetrics").apply { isAccessible = true }
        }.getOrNull()
    }

    override fun install(module: ModuleMainKt, classLoader: ClassLoader?) {
        module.intercept(Resources::class.java.getDeclaredMethod("getDisplayMetrics")) {
            if (reentrant.get() == true) return@intercept proceed()
            val metrics = proceed() as DisplayMetrics
            withReentrant {
                applyPhoneMetrics(metrics)
            }
            metrics
        }

        module.intercept(Resources::class.java.getDeclaredMethod("getConfiguration")) {
            if (reentrant.get() == true) return@intercept proceed()
            val configuration = proceed() as Configuration
            val resources = thisObject as? Resources
            withReentrant {
                val metrics = resources?.peekDisplayMetrics()
                applyPhoneConfiguration(configuration, metrics)
            }
            configuration
        }

        module.intercept(
            Resources::class.java.getDeclaredMethod(
                "updateConfiguration", Configuration::class.java, DisplayMetrics::class.java
            )
        ) {
            if (reentrant.get() == true) return@intercept proceed(args.toTypedArray())
            val configuration = args.getOrNull(0) as? Configuration
            val metrics = args.getOrNull(1) as? DisplayMetrics
            withReentrant {
                metrics?.let(::applyPhoneMetrics)
                configuration?.let { applyPhoneConfiguration(it, metrics) }
            }
            proceed(args.toTypedArray())
        }
    }

    @Suppress("DEPRECATION")
    fun applyPhoneResources(resources: Resources) {
        if (reentrant.get() == true) return
        reentrant.set(true)
        try {
            val metrics = resources.peekDisplayMetrics()
                ?: runCatching { resources.displayMetrics }.getOrNull()
                ?: return
            val configuration = runCatching { resources.configuration }.getOrNull() ?: return
            applyPhoneMetrics(metrics)
            applyPhoneConfiguration(configuration, metrics)
            resources.updateConfiguration(configuration, metrics)
        } finally {
            reentrant.set(false)
        }
    }

    private fun Resources.peekDisplayMetrics(): DisplayMetrics? {
        return runCatching {
            resourcesMetricsField?.get(this) as? DisplayMetrics
        }.getOrNull()
    }

    private inline fun <T> withReentrant(block: () -> T): T {
        val wasReentrant = reentrant.get() == true
        if (!wasReentrant) reentrant.set(true)
        return try {
            block()
        } finally {
            if (!wasReentrant) reentrant.set(false)
        }
    }

    @Suppress("DEPRECATION")
    private fun applyPhoneMetrics(metrics: DisplayMetrics) {
        val shortSidePx = minPositive(metrics.widthPixels, metrics.heightPixels)
        if (shortSidePx <= 0) return

        val oldDensity = metrics.density.takeIf { it > 0f } ?: 1f
        val fontScale =
            (metrics.scaledDensity / oldDensity).takeIf { it.isFinite() && it in 0.5f..2.0f } ?: 1f
        val density = shortSidePx / PhoneProfile.SHORT_WIDTH_DP.toFloat()
        val densityDpi =
            (density * PhoneProfile.BASE_DENSITY_DPI).roundToInt()
                .coerceAtLeast(PhoneProfile.BASE_DENSITY_DPI)

        metrics.density = density
        metrics.scaledDensity = density * fontScale
        metrics.densityDpi = densityDpi
        metrics.xdpi = densityDpi.toFloat()
        metrics.ydpi = densityDpi.toFloat()
    }

    private fun applyPhoneConfiguration(configuration: Configuration, metrics: DisplayMetrics?) {
        if (metrics == null) return

        val widthPx = metrics.widthPixels
        val heightPx = metrics.heightPixels
        val density = metrics.density
        if (widthPx <= 0 || heightPx <= 0 || density <= 0f) return

        configuration.screenWidthDp = (widthPx / density).roundToInt()
        configuration.screenHeightDp = (heightPx / density).roundToInt()
        configuration.smallestScreenWidthDp =
            minPositive(configuration.screenWidthDp, configuration.screenHeightDp)
        configuration.densityDpi = metrics.densityDpi
    }
}
