package com.opennxt.resources.config.vars

import com.opennxt.api.vars.VarDomain
import com.opennxt.resources.DefaultStateChecker

abstract class VarDefinition(
    val domain: VarDomain,
    var type: ScriptVarType = ScriptVarType.INT,
    var lifetime: Int = 0,
    var forceDefault: Boolean = true
) : DefaultStateChecker