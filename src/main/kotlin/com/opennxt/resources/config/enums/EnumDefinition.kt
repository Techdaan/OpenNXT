package com.opennxt.resources.config.enums

import com.opennxt.resources.DefaultStateChecker
import com.opennxt.resources.config.vars.ScriptVarType
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap

data class EnumDefinition(
    var keyType: ScriptVarType = ScriptVarType.INT,
    var valueType: ScriptVarType = ScriptVarType.INT,
    var defaultInt: Int = 0,
    var defaultString: String? = null,
    var values: MutableMap<Int, Any> = Int2ObjectAVLTreeMap()
): DefaultStateChecker {
    companion object {
        private val DEFAULT = EnumDefinition()
    }

    override fun isDefault(): Boolean = this == DEFAULT
}