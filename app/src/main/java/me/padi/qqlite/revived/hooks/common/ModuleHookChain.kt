package me.padi.qqlite.revived.hooks.common

internal interface ModuleHookChain {
    val args: List<Any?>
    val thisObject: Any?

    fun proceed(): Any?
    fun proceed(args: Array<Any?>): Any?
}
