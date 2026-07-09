package me.padi.qqlite.revived.hooks.aio

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
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        }

        is FrameLayout -> FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.CENTER
        }

        else -> ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}

internal fun View.keepInvisibleDataHost() {
    visibility = View.VISIBLE
    alpha = 0.01f
    isClickable = false
    isLongClickable = false
    isFocusable = false
    isFocusableInTouchMode = false
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    disableViewPagerUserInput()
    (this as? ViewGroup)?.apply {
        clipChildren = false
        clipToPadding = false
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
    }
}

internal fun View.disableViewPagerUserInput() {
    runCatching {
        findMethodInHierarchy(javaClass, "setUserInputEnabled", java.lang.Boolean.TYPE)
            ?.invoke(this, false)
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

internal fun View.forEachDescendant(block: (View) -> Boolean): Boolean {
    if (block(this)) return true
    if (this !is ViewGroup) return false

    for (index in 0 until childCount) {
        if (getChildAt(index).forEachDescendant(block)) return true
    }
    return false
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
                arrayOf(Integer.TYPE, java.lang.Boolean.TYPE)
            )
        }?.invoke(this, item, smoothScroll) ?: javaClass.methods.firstOrNull {
            it.name == "setCurrentItem" && it.parameterTypes.contentEquals(arrayOf(Integer.TYPE))
        }?.invoke(this, item)
    }
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

internal fun findRequiredField(clazz: Class<*>, name: String): Field {
    return findOptionalField(clazz, name) ?: error("Field $name not found in ${clazz.name}")
}

internal fun findMethodInHierarchy(
    clazz: Class<*>,
    name: String,
    vararg parameterTypes: Class<*>
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
