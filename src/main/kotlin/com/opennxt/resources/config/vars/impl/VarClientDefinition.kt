package com.opennxt.resources.config.vars.impl

import com.opennxt.api.vars.VarDomain
import com.opennxt.resources.config.vars.VarDefinition

class VarClientDefinition: VarDefinition(VarDomain.CLIENT) {
    companion object {
        val DEFAULT = VarClientDefinition()
    }

    override fun isDefault(): Boolean = this == DEFAULT
}