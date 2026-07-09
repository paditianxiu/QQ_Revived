package me.padi.qqlite.revived.utils

import android.util.Log


const val MODULE_TAG = "MyModule"

inline fun <reified T> Any.getField(name: String): T? {
    return try {
        val field = this.javaClass.getDeclaredField(name)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST") field.get(this) as? T
    } catch (e: NoSuchFieldException) {
        Log.w(MODULE_TAG, "❌ 字段 '$name' 不存在在类 ${this.javaClass.simpleName}")
        null
    } catch (e: Exception) {
        Log.e(MODULE_TAG, "❌ 获取字段 '$name' 失败: ${e.message}")
        null
    }
}

fun Any.printAllFields(tag: String) {
    try {
        val clazz = this.javaClass
        Log.i(MODULE_TAG, "========== $tag ==========")
        Log.i(MODULE_TAG, "Class: ${clazz.name}")

        var currentClass: Class<*>? = clazz
        while (currentClass != null && currentClass != Any::class.java) {
            for (field in currentClass.declaredFields) {
                field.isAccessible = true
                try {
                    val value = field.get(this)
                    val valueStr = when (value) {
                        null -> "null"
                        is String -> "\"$value\""
                        else -> value.toString()
                    }
                    Log.i(MODULE_TAG, "  ${currentClass.simpleName}.${field.name} = $valueStr")
                } catch (e: Exception) {
                    Log.i(
                        MODULE_TAG,
                        "  ${currentClass.simpleName}.${field.name} = <error: ${e.message}>"
                    )
                }
            }
            currentClass = currentClass.superclass
        }
        Log.i(MODULE_TAG, "=================================")
    } catch (e: Exception) {
        Log.e(MODULE_TAG, "printAllFields 失败: ${e.message}", e)
    }
}

fun findMethodInClassAndParents(
    clazz: Class<*>, methodName: String, vararg paramTypes: Class<*>
): java.lang.reflect.Method? {
    var currentClass: Class<*>? = clazz
    while (currentClass != null && currentClass != Any::class.java) {
        try {
            val method = currentClass.getDeclaredMethod(methodName, *paramTypes)
            return method
        } catch (e: NoSuchMethodException) {
        }
        currentClass = currentClass.superclass
    }
    return null
}
