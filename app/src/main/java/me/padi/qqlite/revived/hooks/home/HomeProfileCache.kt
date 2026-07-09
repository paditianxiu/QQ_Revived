package me.padi.qqlite.revived.hooks.home

import android.content.Context
import me.padi.qqlite.revived.hooks.common.findTargetClass
import me.padi.qqlite.revived.shared.model.home.HomeProfile
import me.padi.qqlite.revived.shared.viewmodel.home.HomeStateStore

internal fun updateHomeProfile(uid: String?, nickName: String?, avatarPath: String?) {
    HomeStateStore.latestProfile = HomeProfile(uid = uid?.takeIf { it.isNotBlank() } ?: HomeStateStore.latestProfile.uid,
        nickName = nickName?.takeIf { it.isNotBlank() } ?: HomeStateStore.latestProfile.nickName,
        avatarPath = avatarPath?.takeIf { it.isNotBlank() } ?: HomeStateStore.latestProfile.avatarPath)

    HomeRuntimeStore.mainHandler.post {
        HomeRuntimeStore.latestBinding?.get()?.updateProfile(HomeStateStore.latestProfile)
    }
}

internal fun loadCachedHomeProfile(context: Context) {
    runCatching {
        val qmmkvClass = context.classLoader.findTargetClass(QMMKV_CLASS)
        val optionEntity =
            qmmkvClass.getDeclaredMethod("a", Context::class.java, String::class.java)
                .apply { isAccessible = true }
                .invoke(null, context.applicationContext, QMMKV_PROFILE_FILE) ?: return

        val uid = optionEntity.readQmmkvString(KEY_LOGIN_UID)
        val nickName = optionEntity.readQmmkvString(KEY_LOGIN_NICKNAME)
        val avatarPath = optionEntity.readQmmkvString(KEY_LOGIN_AVATAR_PATH)
        if (uid != null || nickName != null || avatarPath != null) {
            updateHomeProfile(uid, nickName, avatarPath)
        }
    }
}

private fun Any.readQmmkvString(key: String): String? {
    val method = javaClass.methods.firstOrNull {
        it.returnType == String::class.java && it.parameterTypes.contentEquals(
            arrayOf(
                String::class.java, String::class.java
            )
        )
    } ?: return null

    return runCatching {
        method.isAccessible = true
        method.invoke(this, key, "") as? String
    }.getOrNull()?.takeIf { it.isNotBlank() }
}

