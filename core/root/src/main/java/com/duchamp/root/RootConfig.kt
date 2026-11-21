package com.duchamp.root

import com.topjohnwu.superuser.Shell

/**
 * One-time libsu configuration. Call [configure] from [android.app.Application.onCreate] before
 * any root command runs, so the main shell is created with the right flags and timeout.
 */
object RootConfig {

    @Volatile
    private var configured = false

    @Synchronized
    fun configure(debug: Boolean = false) {
        if (configured) return
        configured = true
        Shell.enableVerboseLogging = debug
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setTimeout(10)
        )
    }
}
