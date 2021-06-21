package com.opennxt.resources.config.vars.impl

import com.opennxt.api.vars.VarDomain
import com.opennxt.resources.config.vars.VarDefinition

class VarPlayerDefinition: VarDefinition(VarDomain.PLAYER) {
    companion object {
        val DEFAULT = VarPlayerDefinition()
    }

    override fun isDefault(): Boolean = this == DEFAULT
}