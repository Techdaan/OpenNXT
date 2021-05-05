package com.opennxt.resources.config.params

import com.opennxt.resources.DefaultStateChecker
import com.opennxt.resources.config.vars.ScriptVarType

data class ParamDefinition(
    var defaultInt: Int = 0,
    var defaultString: String = "null",
    var membersOnly: Boolean = true,
    var type: ScriptVarType = ScriptVarType.INT
): DefaultStateChecker {
    companion object {
        private val DEFAULT = ParamDefinition()
    }

    override fun isDefault(): Boolean = this == DEFAULT
}