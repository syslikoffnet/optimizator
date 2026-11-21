package com.duchamp.root

/**
 * Root + device availability probes. Runs off the main thread and never throws.
 */
class RootAvailability(
    private val runner: CommandRunner,
) {
    /** `Shell.isAppGrantedRoot()` equivalent, parsed from a real `su` probe. */
    suspend fun isRootGranted(): Boolean {
        val result = runner.run("id -u")
        return result.success && result.stdout.trim() == "0"
    }

    /** Reads `ro.product.device`; true only on the target `duchamp` device. */
    suspend fun isTargetDevice(): Boolean {
        val result = runner.run("getprop ro.product.device")
        return result.success && result.stdout.trim().equals("duchamp", ignoreCase = true)
    }

    data class Status(
        val rootGranted: Boolean,
        val targetDevice: Boolean,
        val deviceName: String,
    ) {
        val canControl: Boolean get() = rootGranted && targetDevice
        val monitoringOnly: Boolean get() = !canControl
    }

    suspend fun status(): Status {
        val granted = isRootGranted()
        val deviceRes = runner.run("getprop ro.product.device")
        val deviceName = deviceRes.stdout.trim().ifBlank { "unknown" }
        return Status(
            rootGranted = granted,
            targetDevice = deviceRes.success && deviceName.equals("duchamp", ignoreCase = true),
            deviceName = deviceName,
        )
    }
}
