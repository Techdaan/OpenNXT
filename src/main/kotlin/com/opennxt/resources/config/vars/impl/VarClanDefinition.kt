package com.opennxt.resources.config.vars.impl

import com.opennxt.api.vars.VarDomain
import com.opennxt.resources.config.vars.VarDefinition

class VarClanDefinition: VarDefinition(VarDomain.CLAN) {
    companion object {
        val DEFAULT = VarClanDefinition()
    }

    override fun isDefault(): Boolean = this == DEFAULT
}