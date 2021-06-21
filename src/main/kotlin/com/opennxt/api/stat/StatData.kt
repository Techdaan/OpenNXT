package com.opennxt.api.stat

interface StatData {
    val stat: Stat
    val experience: Double
    val actualLevel: Int
    val boostedLevel: Int
}