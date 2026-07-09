package me.padi.qqlite.revived.hooks.home

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import me.padi.qqlite.revived.hooks.common.findTargetClass
import java.lang.reflect.Field
import java.lang.reflect.Method

internal fun ViewGroup.createFullComposeLayoutParams(): ViewGroup.LayoutParams {
    return when (this) {
        is ConstraintLayout -> ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        }

        is FrameLayout -> FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.CENTER
        }

        else -> ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}

internal fun View.hideTree() {
    visibility = View.GONE
    alpha = 0f
    val params = layoutParams
    if (params != null) {
        params.width = 1
        params.height = 1
        layoutParams = params
    }
    (this as? ViewGroup)?.hideChildren()
}

private fun ViewGroup.hideChildren() {
    for (index in 0 until childCount) {
        getChildAt(index).hideTree()
    }
}

internal fun View.findFirstDescendantByClassName(className: String): View? {
    if (javaClass.name == className) return this
    if (this !is ViewGroup) return null

    for (index in 0 until childCount) {
        getChildAt(index).findFirstDescendantByClassName(className)?.let { return it }
    }
    return null
}

internal fun Any.invokeNoArg(name: String): Any? {
    return runCatching {
        val method = javaClass.methods.firstOrNull {
            it.name == name && it.parameterTypes.isEmpty()
        } ?: return null
        method.invoke(this)
    }.getOrNull()
}

internal fun View.readCurrentViewPagerItem(): Int? {
    return invokeNoArg("getCurrentItem") as? Int
}

internal fun View.setViewPagerCurrentItem(item: Int, smoothScroll: Boolean) {
    runCatching {
        javaClass.methods.firstOrNull {
            it.name == "setCurrentItem" && it.parameterTypes.contentEquals(
                arrayOf(
                    Integer.TYPE, java.lang.Boolean.TYPE
                )
            )
        }?.invoke(this, item, smoothScroll) ?: javaClass.methods.firstOrNull {
            it.name == "setCurrentItem" && it.parameterTypes.contentEquals(arrayOf(Integer.TYPE))
        }?.invoke(this, item)
    }
}

internal fun Any.invokeGetItem(position: Int): Any? {
    return runCatching {
        findMethodInHierarchy(javaClass, "getItem", Integer.TYPE)?.invoke(this, position)
    }.getOrNull()
}

internal fun Any.findFieldValue(name: String): Any? {
    var clazz: Class<*>? = javaClass
    while (clazz != null && clazz != Any::class.java) {
        runCatching {
            return clazz.getDeclaredField(name).apply { isAccessible = true }.get(this)
        }
        clazz = clazz.superclass
    }
    return null
}

internal fun findOptionalField(clazz: Class<*>, name: String): Field? {
    var currentClass: Class<*>? = clazz
    while (currentClass != null && currentClass != Any::class.java) {
        runCatching {
            return currentClass.getDeclaredField(name).apply { isAccessible = true }
        }
        currentClass = currentClass.superclass
    }
    return null
}

internal fun findMethodInHierarchy(
    clazz: Class<*>, name: String, vararg parameterTypes: Class<*>
): Method? {
    var currentClass: Class<*>? = clazz
    while (currentClass != null && currentClass != Any::class.java) {
        runCatching {
            return currentClass.getDeclaredMethod(name, *parameterTypes).apply {
                isAccessible = true
            }
        }
        currentClass = currentClass.superclass
    }
    return null
}

internal fun ClassLoader.findOptionalClass(name: String): Class<*>? {
    return runCatching { findTargetClass(name) }.getOrNull()
}

