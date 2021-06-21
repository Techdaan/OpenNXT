package com.opennxt.resources.defaults

import io.netty.buffer.ByteBuf

/**
 * Represents a default in a cache. A default is a lot like a config, but defaults are singletons, whereas configs are
 * templates and have many instances.
 */
interface Default {

    /**
     * @return The type of this default
     */
    val group: DefaultGroup

    fun decode(buf: ByteBuf)

}
