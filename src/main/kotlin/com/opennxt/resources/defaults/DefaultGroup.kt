package com.opennxt.resources.defaults

import com.opennxt.resources.defaults.stats.StatDefaults
import com.opennxt.resources.defaults.wearpos.WearposDefaults
import io.netty.buffer.ByteBuf
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

enum class DefaultGroup(val fileId: Int, val defaultClass: KClass<out Default>?) {
    MAP(1, null),
    GROUP_2(2, null),
    GRAPHICS(3, null),
    AUDIO(4, null),
    MICROTRANSACTION(5, null),
    WEARPOS(6, WearposDefaults::class),
    KEYBOARD(7, null),
    GROUP_8(8, null),
    STAT(9, StatDefaults::class),
    ERROR(10, null);

    @Suppress("UNCHECKED_CAST")
    fun <T : Default> decode(buffer: ByteBuf): T? {
        return try {
            val instance = defaultClass!!.createInstance()
            instance.decode(buffer)

            instance as T
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        private val classToGroup = Object2ObjectOpenHashMap<KClass<out Default>, DefaultGroup>()

        init {
            for (group in values()) {
                if (group.defaultClass != null)
                    classToGroup[group.defaultClass] = group
            }
        }

        fun getGroup(type: KClass<out Default>): DefaultGroup? = classToGroup[type]
    }
}
