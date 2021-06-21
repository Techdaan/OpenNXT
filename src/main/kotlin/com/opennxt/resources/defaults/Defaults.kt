package com.opennxt.resources.defaults

import com.opennxt.filesystem.Container
import com.opennxt.filesystem.Filesystem
import io.netty.buffer.Unpooled
import java.util.*

class Defaults(val fs: Filesystem) {
    private val cachedDefaults = EnumMap<DefaultGroup, Default>(DefaultGroup::class.java)

    fun <T : Default> getDefault(group: DefaultGroup): T {
        if (group.defaultClass == null)
            throw UnsupportedOperationException("Group is not implemented: $group")

        if (cachedDefaults.containsKey(group))
            return cachedDefaults.getValue(group) as T


        val buf = Unpooled.wrappedBuffer(Container.decode(fs.read(28, group.fileId)!!).data)
        try {
            val value = group.decode<T>(buf)!!
            cachedDefaults[group] = value
            return value
        } finally {
            buf.release()
        }
    }

    inline fun <reified T : Default> get(): T {
        val group = DefaultGroup.getGroup(T::class) ?: throw UnsupportedOperationException("Default group: ${T::class}")
        return getDefault(group)
    }
}