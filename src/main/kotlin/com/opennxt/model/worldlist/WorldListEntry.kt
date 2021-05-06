package com.opennxt.model.worldlist

import com.google.common.base.Objects

data class WorldListEntry(
    val id: Int,
    val location: WorldListLocation,
    val flag: Int,
    val locationOverride: WorldListLocation? = null,
    val activity: String,
    val host: String,
    var playercount: Int = 0
) {
    // Hash that only hashes the immutable fields
    val partialHash = Objects.hashCode(id, location, flag, locationOverride, activity, host)
}