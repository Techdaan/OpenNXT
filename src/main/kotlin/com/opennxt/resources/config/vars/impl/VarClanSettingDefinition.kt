package com.opennxt.resources.config.vars.impl

import com.opennxt.api.vars.VarDomain
import com.opennxt.resources.config.vars.VarDefinition

class VarClanSettingDefinition: VarDefinition(VarDomain.CLAN_SETTING) {
    companion object {
        val DEFAULT = VarClanSettingDefinition()
    }

    override fun isDefault(): Boolean = this == DEFAULT
}