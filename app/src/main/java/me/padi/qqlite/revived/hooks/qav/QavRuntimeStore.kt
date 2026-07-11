package me.padi.qqlite.revived.hooks.qav

internal object QavRuntimeStore {
    @Volatile
    private var outgoingVideoCall: Boolean? = null

    fun rememberOutgoingCall(isVideo: Boolean) {
        outgoingVideoCall = isVideo
    }

    fun peekOutgoingCall(): Boolean? = outgoingVideoCall

    fun clearOutgoingCall() {
        outgoingVideoCall = null
    }
}
