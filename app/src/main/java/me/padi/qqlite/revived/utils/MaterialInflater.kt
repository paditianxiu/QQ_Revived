package me.padi.qqlite.revived.utils

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.navigationrail.NavigationRailView

/**
 * Material主题的Context包装器，用于在Xposed模块中加载Material组件
 */
class MaterialInflaterContext(
    base: Context,
    themeResId: Int,
    private val materialClassLoader: ClassLoader?
) : ContextThemeWrapper(base, themeResId) {
    private var cachedInflater: LayoutInflater? = null

    override fun getClassLoader(): ClassLoader {
        return materialClassLoader ?: super.getClassLoader()
    }

    override fun getSystemService(name: String): Any? {
        if (name != Context.LAYOUT_INFLATER_SERVICE) {
            return super.getSystemService(name)
        }

        cachedInflater?.let { return it }
        return LayoutInflater.from(baseContext).cloneInContext(this).also { inflater ->
            runCatching {
                inflater.factory2 = MaterialClassLoaderFactory(materialClassLoader)
            }
            cachedInflater = inflater
        }
    }

    /**
     * 便捷方法：使用Material主题inflate布局
     */
    fun inflateMaterialLayout(
        @LayoutRes layoutResId: Int,
        parent: ViewGroup? = null,
        attachToParent: Boolean = false
    ): View? {
        return LayoutInflater.from(this).inflate(layoutResId, parent, attachToParent)
    }
}

/**
 * 自定义Factory，用于加载Material组件
 */
private class MaterialClassLoaderFactory(
    private val materialClassLoader: ClassLoader?
) : LayoutInflater.Factory2 {
    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        return createMaterialView(name, context, attrs)
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return createMaterialView(name, context, attrs)
    }

    private fun createMaterialView(name: String, context: Context, attrs: AttributeSet): View? {
        // 只处理Material组件
        if (!name.startsWith("com.google.android.material.")) {
            return null
        }

        return runCatching {
            val clazz = Class.forName(name, false, materialClassLoader)
                .asSubclass(View::class.java)
            val constructor = clazz.getConstructor(Context::class.java, AttributeSet::class.java)
            constructor.newInstance(context, attrs)
        }.getOrNull()
    }
}

/**
 * 扩展函数：创建Material主题的Context
 */
fun Context.createMaterialContext(
    themeResId: Int = com.google.android.material.R.style.Theme_Material3_Light_NoActionBar,
    classLoader: ClassLoader? = NavigationRailView::class.java.classLoader
): MaterialInflaterContext {
    return MaterialInflaterContext(this, themeResId, classLoader)
}
