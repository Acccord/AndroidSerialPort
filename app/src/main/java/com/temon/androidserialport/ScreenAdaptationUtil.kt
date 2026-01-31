package com.temon.androidserialport

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlin.math.min

/**
 * Principle: adjust DisplayMetrics.density for scaling.
 *
 * Usage:
 * 1. Initialize in Application: ScreenAdaptationUtil.init(application, designWidthDp)
 * 2. Call in Activity onCreate: ScreenAdaptationUtil.setCustomDensity(activity, application)
 */
object ScreenAdaptationUtil {

    // Design width in dp.
    private var designWidthDp = 360f

    // Initialization flag.
    private var isInit = false

    // Activities to exclude.
    private val excludeActivities = mutableSetOf<String>()

    // Density safety bounds.
    private const val PHONE_MAX_DENSITY = 3.2f
    private const val LARGE_SCREEN_MAX_DENSITY = 2.2f

    fun init(application: Application, designWidthDp: Float = 360f) {
        this.designWidthDp = designWidthDp
        isInit = true

        application.registerComponentCallbacks(object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                if (newConfig.fontScale > 0) {
                    val dm = application.resources.displayMetrics
                    dm.scaledDensity = dm.density * newConfig.fontScale
                }
            }

            override fun onLowMemory() {}
        })
    }

    fun setCustomDensity(activity: Activity, application: Application) {
        if (!isInit) return
        if (isInEditMode(activity)) return
        if (excludeActivities.contains(activity.javaClass.name)) return

        val appDm = application.resources.displayMetrics
        val actDm = activity.resources.displayMetrics
        val config = activity.resources.configuration

        // Resolve screen size using WindowManager if needed.
        var widthPixels = appDm.widthPixels
        var heightPixels = appDm.heightPixels

        if (widthPixels <= 0 || heightPixels <= 0 || widthPixels < 200 || heightPixels < 200) {
            val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (windowManager != null) {
                val realMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getRealMetrics(realMetrics)
                widthPixels = realMetrics.widthPixels
                heightPixels = realMetrics.heightPixels

                if (widthPixels <= 0 || heightPixels <= 0) {
                    windowManager.defaultDisplay.getMetrics(realMetrics)
                    widthPixels = realMetrics.widthPixels
                    heightPixels = realMetrics.heightPixels
                }
            }
        }

        // Fallback defaults to avoid crashes.
        if (widthPixels <= 0 || heightPixels <= 0) {
            widthPixels = 1080
            heightPixels = 1920
        }

        // 1) Shortest side for portrait/landscape safety.
        val shortestPx = min(widthPixels, heightPixels)

        // 2) Base density.
        val rawDensity = shortestPx / designWidthDp

        // 3) Large screen check (tablet / kiosk).
        val isLargeScreen = config.smallestScreenWidthDp >= 600

        // 4) Density clamp strategy.
        val targetDensity = when {
            isLargeScreen -> rawDensity.coerceIn(1.6f, LARGE_SCREEN_MAX_DENSITY)
            else -> rawDensity.coerceIn(1.6f, PHONE_MAX_DENSITY)
        }

        val targetDensityDpi = (160 * targetDensity).toInt()

        // Font scale.
        val fontScale = config.fontScale.takeIf { it > 0 } ?: 1f
        val targetScaledDensity = targetDensity * fontScale

        // Application metrics.
        appDm.density = targetDensity
        appDm.densityDpi = targetDensityDpi
        appDm.scaledDensity = targetScaledDensity

        // Activity metrics.
        actDm.density = targetDensity
        actDm.densityDpi = targetDensityDpi
        actDm.scaledDensity = targetScaledDensity
    }

    fun addExcludeActivity(clazz: Class<out Activity>) {
        excludeActivities.add(clazz.name)
    }

    fun removeExcludeActivity(clazz: Class<out Activity>) {
        excludeActivities.remove(clazz.name)
    }

    fun clearExcludeActivities() {
        excludeActivities.clear()
    }

    private fun isInEditMode(context: Context): Boolean {
        return try {
            Class.forName("com.android.layoutlib.bridge.impl.RenderSession")
            true
        } catch (e: Throwable) {
            false
        }
    }
}