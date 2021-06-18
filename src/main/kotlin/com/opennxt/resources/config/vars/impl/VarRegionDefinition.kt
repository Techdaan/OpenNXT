package com.opennxt.resources.config.vars.impl

import com.opennxt.api.vars.VarDomain
import com.opennxt.resources.config.vars.VarDefinition

class VarRegionDefinition: VarDefinition(VarDomain.REGION) {
    companion object {
        val DEFAULT = VarRegionDefinition()
    }

    override fun isDefault(): Boolean = this == DEFAULT
}