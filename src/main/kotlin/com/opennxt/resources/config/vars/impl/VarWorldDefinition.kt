package com.opennxt.resources.config.vars.impl

import com.opennxt.api.vars.VarDomain
import com.opennxt.resources.config.vars.VarDefinition

class VarWorldDefinition: VarDefinition(VarDomain.WORLD) {
    companion object {
        val DEFAULT = VarWorldDefinition()
    }

    override fun isDefault(): Boolean = this == DEFAULT
}