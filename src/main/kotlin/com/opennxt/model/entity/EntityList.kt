package com.opennxt.model.entity

/**
 * A list containing entities that automatically assigns indices to the entities that are added
 */
class EntityList<T : Entity>(val capacity: Int) : Iterable<T> {

    /**
     * The entities that are currently present in the list
     */
    private val values = arrayOfNulls<Entity>(capacity)

    /**
     * The current size of the list
     */
    private var size: Int = 0

    init {
        if (capacity > Short.MAX_VALUE)
            throw IllegalArgumentException("Repository can't be bigger than the max short value")
    }

    /**
     * Adds an entity to the list, assigning it an id, and returning whether if the entity was added successfully
     */
    fun add(entity: T): Boolean {
        if(isFull()) return false

        for(i in 0 until capacity) {
            if (values[i] != null) continue
            values[i] = entity
            entity.index = i + 1
            size++
            return true
        }
        return false
    }

    /**
     * Checks if the list is full
     */
    fun isFull(): Boolean = size == capacity

    /**
     * Checks the amount of entities currently in the list
     */
    fun size(): Int = size

    /**
     * Removes an entity from the list by the entity
     */
    fun remove(entity: T) {
        remove(entity.index - 1)
    }

    /**
     * Removes an entity from the list by the index
     */
    fun remove(index: Int) {
        if (index < 0) return
        val current = values[index] ?: return

        if (current.index - 1 != index)
            throw IllegalStateException("Entity is in the wrong spot in EntityList. This should never happen.")

        values[index] = null
        current.index = -1
        size--
    }

    /**
     * Gets an entity by the index of the entity
     */
    @Suppress("UNCHECKED_CAST")
    operator fun get(index: Int): T? {
        if (index < 1 || index > capacity)
            return null
        return values[index - 1] as? T
    }

    override fun iterator(): Iterator<T> = EntityListIterator(this)
}