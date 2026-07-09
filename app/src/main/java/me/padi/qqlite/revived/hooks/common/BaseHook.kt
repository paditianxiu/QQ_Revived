package me.padi.qqlite.revived.hooks.common

import me.padi.qqlite.revived.ModuleMainKt

internal abstract class BaseHook {
    abstract fun install(module: ModuleMainKt, classLoader: ClassLoader?)

    open fun reset() = Unit

    protected fun requireClassLoader(classLoader: ClassLoader?): ClassLoader {
        return requireNotNull(classLoader) {
            "${javaClass.simpleName} requires a target ClassLoader"
        }
    }
}
