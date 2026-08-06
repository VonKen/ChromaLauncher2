package com.chromalauncher.app.manager

import android.content.Context
import com.chromalauncher.app.data.Renderer
import com.chromalauncher.app.plugins.DriverPlugin
import com.chromalauncher.app.plugins.RendererPlugin
import net.kdt.pojavlaunch.PojavApplication

/**
 * Manages renderer and driver plugins. Built-in renderers are configured through
 * the launcher's string arrays and are resolved separately; this manager only
 * tracks renderer plugins installed as separate apps. Ported from Fold Craft
 * Launcher (com.mio.manager.RendererManager).
 */
object RendererManager {
    private var isInit = false
    val rendererList: MutableList<Renderer> = mutableListOf()
        get() {
            if (!isInit) {
                init(PojavApplication.instance)
            }
            return field
        }

    fun init(context: Context) {
        if (isInit) {
            return
        }
        isInit = true
        RendererPlugin.init(context)
        rendererList.addAll(RendererPlugin.rendererList)
        DriverPlugin.init(context)
    }

    fun refresh(context: Context) {
        RendererPlugin.refresh(context)
        rendererList.clear()
        isInit = false
        init(context)
    }

    @JvmStatic
    fun getRenderer(id: String): Renderer {
        return rendererList.find { it.id == id }
            ?: Renderer(
                name = id,
                des = id,
                glName = "",
                eglName = "",
                path = "",
                boatEnv = null,
                pojavEnv = null,
                id = id
            )
    }
}
