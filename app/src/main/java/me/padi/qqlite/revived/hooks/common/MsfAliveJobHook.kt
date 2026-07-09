package me.padi.qqlite.revived.hooks.common

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.util.Log
import me.padi.qqlite.revived.ModuleMainKt

internal object MsfAliveJobHook : BaseHook() {
    private const val JOB_SCHEDULER_IMPL_CLASS = "android.app.JobSchedulerImpl"
    private const val MSF_ALIVE_JOB_SERVICE = "com.tencent.mobileqq.msf.service.MSFAliveJobService"

    private var installed = false

    override fun reset() {
        installed = false
    }

    @SuppressLint("PrivateApi")
    override fun install(module: ModuleMainKt, classLoader: ClassLoader?) {
        if (installed) return

        runCatching {
            val jobSchedulerImplClass = Class.forName(JOB_SCHEDULER_IMPL_CLASS)
            module.intercept(
                jobSchedulerImplClass.getDeclaredMethod(
                    "schedule",
                    JobInfo::class.java
                ).apply { isAccessible = true }
            ) {
                val jobInfo = args.getOrNull(0) as? JobInfo
                if (jobInfo?.service?.className == MSF_ALIVE_JOB_SERVICE) {
                    runCatching {
                        proceed()
                    }.getOrElse { throwable ->
                        if (throwable.isMissingMsfAliveJobService()) {
                            module.logHook(
                                Log.WARN,
                                "MSF alive job schedule ignored: missing service",
                                throwable
                            )
                            JobScheduler.RESULT_FAILURE
                        } else {
                            throw throwable
                        }
                    }
                } else {
                    proceed()
                }
            }
            installed = true
            module.logHook(Log.INFO, "MSF alive job hook installed")
        }.onFailure {
            module.logHook(Log.WARN, "MSF alive job hook skipped", it)
        }
    }

    private fun Throwable.isMissingMsfAliveJobService(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            val message = current.message.orEmpty()
            if (current is IllegalArgumentException &&
                message.contains("non-existent component") &&
                message.contains(MSF_ALIVE_JOB_SERVICE)
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}
