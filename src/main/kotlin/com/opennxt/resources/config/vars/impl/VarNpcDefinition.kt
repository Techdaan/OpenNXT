package com.opennxt.resources.config.vars.impl

import com.opennxt.api.vars.VarDomain
import com.opennxt.resources.config.vars.VarDefinition

class VarNpcDefinition: VarDefinition(VarDomain.NPC) {
    companion object {
        val DEFAULT = VarNpcDefinition()
    }

    override fun isDefault(): Boolean = this == DEFAULT
}