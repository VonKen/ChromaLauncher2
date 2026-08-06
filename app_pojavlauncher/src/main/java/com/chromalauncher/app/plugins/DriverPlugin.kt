package com.chromalauncher.app.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import net.kdt.pojavlaunch.PojavApplication

/**
 * Discovers GPU driver plugins installed as separate apps (e.g. Turnip drivers).
 * A plugin declares fclPlugin=true and driver="<name>" meta-data on its main activity.
 * Ported from Fold Craft Launcher (com.tungsten.fclauncher.plugins.DriverPlugin).
 */
object DriverPlugin {
    data class Driver(val driver: String, val path: String)

    private var isInit = false
    private const val PACKAGE_FLAGS =
        PackageManager.GET_META_DATA or PackageManager.GET_SHARED_LIBRARY_FILES

    val driverList: MutableList<Driver> = mutableListOf()
        get() {
            if (!isInit) {
                init(PojavApplication.instance)
            }
            return field
        }

    @JvmStatic
    var selected: Driver = Driver("Turnip", "")

    /** Selects the driver matching [name], falling back to the first driver if not found. */
    @JvmStatic
    fun selectDriver(name: String) {
        selected = driverList.find { it.driver == name } ?: driverList.first()
    }

    @JvmStatic
    @SuppressLint("QueryPermissionsNeeded")
    fun init(context: Context) {
        isInit = true
        driverList.clear()
        driverList.add(Driver("Turnip", context.applicationInfo.nativeLibraryDir))
        selected = driverList.first()
        scan(context)
    }

    /** Re-scans installed driver plugins, keeping the currently selected driver. */
    @JvmStatic
    @SuppressLint("QueryPermissionsNeeded")
    fun refresh(context: Context) {
        val previous = selected
        init(context)
        val kept = driverList.find { it.driver == previous.driver }
        if (kept != null) selected = kept
    }

    private fun scan(context: Context) {
        val queryIntentActivities =
            context.packageManager.queryIntentActivities(Intent("android.intent.action.MAIN"),
                PACKAGE_FLAGS
            )
        queryIntentActivities.forEach {
            parse(it.activityInfo.applicationInfo)
        }
    }

    private fun parse(info: ApplicationInfo) {
        if (info.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            val metaData = info.metaData ?: return
            if (metaData.getBoolean("fclPlugin", false)) {
                val driver = metaData.getString("driver") ?: return
                val nativeLibraryDir = info.nativeLibraryDir
                add(
                    Driver(
                        driver,
                        nativeLibraryDir
                    )
                )
            }
        }
    }

    private fun add(driver: Driver) {
        driverList.removeIf { it.path == driver.path }
        driverList.add(driver)
    }
}
