package me.padi.qqlite.revived.hooks.common

internal fun ClassLoader.findTargetClass(name: String): Class<*> {
    return Class.forName(name, false, this)
}
