package me.padi.qqlite.revived.hooks.home

import android.os.Handler
import android.os.Looper
import android.view.View
import me.padi.qqlite.revived.shared.viewmodel.home.HomeStateStore
import java.lang.ref.WeakReference
import java.util.WeakHashMap

internal object HomeRuntimeStore {
    var latestBinding: WeakReference<HomeBinding>? = null
    val mainHandler = Handler(Looper.getMainLooper())
    val bindingsByViewPager = WeakHashMap<View, WeakReference<HomeBinding>>()

    fun reset() {
        HomeStateStore.reset()
        latestBinding = null
        bindingsByViewPager.clear()
    }
}
