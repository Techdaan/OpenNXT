package com.opennxt.resources.config.vars.impl

import com.opennxt.api.vars.VarDomain
import com.opennxt.resources.config.vars.VarDefinition

class VarObjectDefinition: VarDefinition(VarDomain.OBJECT) {
    companion object {
        val DEFAULT = VarObjectDefinition()
    }

    override fun isDefault(): Boolean = this == DEFAULT
}