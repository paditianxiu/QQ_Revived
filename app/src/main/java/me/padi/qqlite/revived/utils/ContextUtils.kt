// ContextUtils.kt
package me.padi.qqlite.revived.utils

import android.content.Context
import me.padi.qqlite.revived.ModuleMainKt


object ContextUtils {
    fun getMobileQQContext(): Context? {
        return try {
            val mobileQQClass = Class.forName("mqq.app.MobileQQ")
            val field = mobileQQClass.getDeclaredField("sMobileQQ")
            field.isAccessible = true
            field.get(null) as? Context
        } catch (e: Exception) {

            null
        }
    }


    fun getModuleContext(module: ModuleMainKt): Context {
        val hostContext = getMobileQQContext()
            ?: throw IllegalStateException("Cannot get MobileQQ context")
        return module.moduleContext(hostContext)
    }
}