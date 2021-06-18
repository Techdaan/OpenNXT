package com.opennxt.api.stat

// Not sealed because we want to allow external experience sources to be defined
abstract class ExperienceSource(val boostFactor: Double) {
    object Default: ExperienceSource(1.0)

    object Lamp: ExperienceSource(1.0)
    object Quest: ExperienceSource(1.0)
}