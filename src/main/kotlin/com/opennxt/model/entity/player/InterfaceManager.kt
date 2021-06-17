package com.opennxt.model.entity.player

import com.opennxt.content.interfaces.InterfaceSlot
import com.opennxt.model.InterfaceHash
import com.opennxt.model.entity.BasePlayer
import com.opennxt.net.game.serverprot.ifaces.*
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import mu.KotlinLogging

class InterfaceManager(val player: BasePlayer) {

    companion object {
        val logger = KotlinLogging.logger {  }
    }

    /**
     * The current opened root interface
     */
    private var root: OpenedInterface? = null

    /**
     * Checks if an interface is opened or not
     */
    fun isOpened(id: Int): Boolean {
        return root?.findChild(id) != null
    }

    /**
     * Opens a root interface, closing the previous root interface if necessary
     *
     * Overriding a previous root interface is undefined behaviour as of now, and is discouraged.
     */
    fun openTop(id: Int) {
        if (root != null) {
            // TODO Fire on_interface_close event
        }

        root = OpenedInterface(id, walkable = true)
        player.client.write(IfOpenTop(id))
    }

    /**
     * Closes a component from an interface. This will first run all close listeners followed by sending the packet
     */
    fun close(id: Int, component: Int) {
        val base = root?.findChild(id) ?: return

        if (!base.children.containsKey(component)) return

        val removed = base.children.remove(component)
        // TODO Recursively go through removed?
        // TODO Fire on_interface_close event
        println("Closed interface $removed")

//        player.client.write(IfCloseSubPacket(id, component))
    }

    /**
     * Attempts to open an interface on another interface. This requires a root interface to be present, as well as the
     * interface that this interface opens.
     *
     * If this action were to override another interface, the following would happen:
     * - The previous interface would be overwritten
     * - The [IfOpenSubPacket] would be sent again
     * - No on_interface_close listener would be fired
     * - A warning would be logged to the console, regardless of the debug mode
     *
     * If [parent] is set to -1, the interface will be opened on the root interface
     */
    fun open(
        id: Int,
        parent: Int = -1,
        component: Int,
        walkable: Boolean = false
    ) {
        val root = root
            ?: throw IllegalStateException("Attempted to open interface before opening root interface for player ${player.name}")

        val parentId = if (parent == -1) root.id else parent

        val child = root.findChild(parentId)
            ?: throw IllegalStateException("Attempting to open interface $id on parent $parent, but parent interface was not found for player ${player.name}")

        if (child.children[component] != null)
            logger.warn("Overriding an interface on ${child.id}:$component with interface $id for player ${player.name}")
        child.children[component] = OpenedInterface(id, walkable = walkable)

        player.client.write(IfOpenSub(id, walkable, InterfaceHash(parentId, component)))
        // TODO Fire on_interface_open listener
    }

    /**
     * Opens a game interface. The positions of these interfaces are stored in the cache.
     */
    fun open(slot: InterfaceSlot, id: Int, offset: Int = 0, walkable: Boolean = false) {
        this.open(id, slot.parent, slot.component + offset, walkable)
    }

    /**
     * Closes a game interface. The position of these interfaces are stored in the cache
     */
    fun close(slot: InterfaceSlot, offset: Int = 0) {
        this.close(slot.parent, slot.component + offset)
    }

    /**
     * Applies a clickmask to a range of slots of a component
     */
    fun events(id: Int, component: Int, from: Int, to: Int, mask: Int) {
        player.client.write(IfSetevents(InterfaceHash(id, component), from, to, mask))
    }

    /**
     * Sets the text on an interface
     */
    fun text(id: Int, component: Int, text: String) {
        player.client.write(IfSettext(InterfaceHash(id, component), text))
    }

    /**
     * (Un)hides an interface by the interface id + component of the target interface
     */
    fun hide(id: Int, component: Int, hidden: Boolean) {
        player.client.write(IfSethide(InterfaceHash(id, component), hidden))
    }

    /**
     * Closes every modal
     *
     * TODO: Are all walkable interfaces modals? I do think so, but I am not sure.
     */
    fun closeModals() {
        val root = root ?: return

        fun iterateOver(iface: OpenedInterface) {
            iface.children.forEach { (id, child) ->
                if (!child.walkable) {
                    close(iface.id, id)
                    return@forEach
                }
                iterateOver(child)
            }
        }

        iterateOver(root)
    }

    /**
     * Represents an interface that the client has opened. This can be used to validate button presses.
     */
    private data class OpenedInterface(
        val id: Int,
        val children: Int2ObjectOpenHashMap<OpenedInterface> = Int2ObjectOpenHashMap(),
        val walkable: Boolean
    ) {
        /**
         * Finds a child interface of this interface
         */
        fun findChild(id: Int): OpenedInterface? {
            if (this.id == id) return this
            for (child in children) {
                return child.value.findChild(id) ?: continue
            }
            return null
        }
    }

}