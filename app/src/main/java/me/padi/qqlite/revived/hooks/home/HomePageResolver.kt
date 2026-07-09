package me.padi.qqlite.revived.hooks.home

import android.view.View
import me.padi.qqlite.revived.shared.model.home.HomePage

internal fun readHomePages(viewPager: View): List<HomePage> {
    val adapter = viewPager.invokeNoArg("getAdapter") ?: return defaultPages()
    val itemCount =
        (adapter.invokeNoArg("getItemCount") as? Int)?.coerceAtLeast(0) ?: return defaultPages()
    if (itemCount == 0) return emptyList()

    return (0 until itemCount).map { index ->
        val fragment = adapter.createMainFragment(index)
        val className = fragment?.javaClass?.name.orEmpty()
        HomePage(index, titleForFragment(className, index), className)
    }
}

private fun defaultPages(): List<HomePage> {
    return listOf(
        HomePage(0, "消息", "com.tencent.qqnt.watch.chat.ui.ChatListFragment"),
        HomePage(1, "联系人", "com.tencent.qqnt.watch.contact.ui.ContactListFragment"),
        HomePage(2, "动态", "com.tencent.watch.qzone_impl.frame.QZoneMainFrame"),
        HomePage(3, "我的", "com.tencent.qqnt.watch.selftab.ui.SelfFragment")
    )
}

private fun Any.createMainFragment(index: Int): Any? {
    val method = javaClass.methods.firstOrNull {
        (it.name == "t" || it.name == "f") && it.parameterTypes.contentEquals(arrayOf(Integer.TYPE))
    } ?: return null
    return runCatching { method.invoke(this, index) }.getOrNull()
}

private fun titleForFragment(className: String, index: Int): String {
    return when {
        className.contains("ChatListFragment") -> "消息"
        className.contains("ContactListFragment") -> "联系人"
        className.contains("QZoneMainFrame") -> "动态"
        className.contains("SelfFragment") -> "我的"
        else -> "页面${index + 1}"
    }
}

