package com.chromalauncher.app.manager

import android.content.Context
import com.chromalauncher.app.ChromaApplication
import com.chromalauncher.app.data.Renderer
import com.chromalauncher.app.plugins.DriverPlugin
import com.chromalauncher.app.plugins.RendererPlugin

/**
 * Merges the launcher's built-in renderers with renderers discovered from installed
 * plugin apps. Ported from Fold Craft Launcher (com.mio.manager.RendererManager),
 * keeping Chroma Launcher's existing built-in renderer ids.
 */
object RendererManager {
    private var isInit = false

    val rendererList: MutableList<Renderer> = mutableListOf()
        get() {
            if (!isInit) {
                init(ChromaApplication.instance)
            }
            return field
        }

    fun init(context: Context) {
        if (isInit) return
        isInit = true
        rendererList.clear()
        rendererList.add(
            Renderer(
                "opengles2",
                "GL4ES (OpenGL ES 2)",
                "libgl4es_114.so",
                "libEGL.so",
                "",
                null,
                null,
                "opengles2"
            )
        )
        rendererList.add(
            Renderer(
                "vulkan_zink",
                "Zink (Vulkan)",
                "libOSMesa.so",
                "libEGL_mesa.so",
                "",
                null,
                null,
                "vulkan_zink"
            )
        )
        rendererList.add(
            Renderer(
                "opengles3_ltw",
                "LTW (OpenGL ES 3)",
                "libltw.so",
                "libltw.so",
                "",
                null,
                null,
                "opengles3_ltw"
            )
        )
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
        return rendererList.find { it.id == id } ?: rendererList.first()
    }
}
