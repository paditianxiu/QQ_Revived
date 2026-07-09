package me.padi.qqlite.revived.hooks.common

import android.util.Log
import me.padi.qqlite.revived.ModuleMainKt
import java.lang.reflect.Field
import java.lang.reflect.Method

internal object VideoReportHiddenApiHook : BaseHook() {
    private const val REFLECT_UTILS_CLASS =
        "com.tencent.qqlive.module.videoreport.utils.ReflectUtils"
    private const val TOUCH_TARGET_CLASS = "android.view.ViewGroup\$TouchTarget"
    private const val TOUCH_TARGET_NEXT_FIELD = "next"

    private var installed = false

    override fun reset() {
        installed = false
    }

    override fun install(module: ModuleMainKt, classLoader: ClassLoader?) {
        if (installed) return

        val targetClassLoader = requireClassLoader(classLoader)
        runCatching {
            val reflectUtilsClass = targetClassLoader.findTargetClass(REFLECT_UTILS_CLASS)
            var hookCount = 0
            reflectUtilsClass.declaredMethods
                .filter { method -> method.isLikelyFieldAccessor() }
                .forEach { method ->
                    runCatching {
                        module.intercept(method.apply { isAccessible = true }) {
                            if (isTouchTargetNextAccess(args)) {
                                return@intercept method.safeEmptyReturn()
                            }
                            proceed()
                        }
                        hookCount++
                    }.onFailure {
                        module.logHook(
                            Log.WARN,
                            "VideoReport hidden api method hook skipped: ${method.name}",
                            it
                        )
                    }
                }
            if (hookCount <= 0) {
                error("No ReflectUtils field accessor found")
            }
            installed = true
            module.logHook(Log.INFO, "VideoReport hidden api hook installed: $hookCount")
        }.onFailure {
            module.logHook(Log.WARN, "VideoReport hidden api hook skipped", it)
        }
    }

    private fun Method.isLikelyFieldAccessor(): Boolean {
        val lowerName = name.lowercase()
        if (!lowerName.contains("field") && returnType != Field::class.java) return false
        val hasClassParam = parameterTypes.any { it == Class::class.java }
        val hasStringParam = parameterTypes.any { it == String::class.java }
        return hasClassParam && hasStringParam
    }

    private fun Method.safeEmptyReturn(): Any? {
        return when {
            returnType == java.lang.Boolean.TYPE -> false
            returnType == java.lang.Byte.TYPE -> 0.toByte()
            returnType == java.lang.Short.TYPE -> 0.toShort()
            returnType == java.lang.Integer.TYPE -> 0
            returnType == java.lang.Long.TYPE -> 0L
            returnType == java.lang.Float.TYPE -> 0f
            returnType == java.lang.Double.TYPE -> 0.0
            returnType == java.lang.Character.TYPE -> 0.toChar()
            returnType == java.lang.Void.TYPE -> null
            else -> null
        }
    }

    private fun isTouchTargetNextAccess(args: List<Any?>): Boolean {
        val targetClass = args.firstOrNull { it is Class<*> } as? Class<*>
        val fieldName = args.firstOrNull { it is String } as? String
        return targetClass?.name == TOUCH_TARGET_CLASS && fieldName == TOUCH_TARGET_NEXT_FIELD
    }
}
