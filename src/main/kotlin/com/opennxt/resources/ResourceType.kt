package com.opennxt.resources

import com.opennxt.resources.config.enums.EnumDefinition
import com.opennxt.resources.config.params.ParamDefinition
import com.opennxt.resources.config.structs.StructDefinition
import kotlin.reflect.KClass

enum class ResourceType(val identifier: String, val kclass: KClass<*>) {
    ENUM("enum", EnumDefinition::class),
    PARAM("param", ParamDefinition::class),
    STRUCT("struct", StructDefinition::class),

    /*
    TODO:
    VAR_PLAYER(60, VarPlayer::class),
    VAR_NPC(61, VarNpc::class),
    VAR_CLIENT(62, VarClient::class),
    VAR_WORLD(63, VarWorld::class),
    VAR_REGION(64, VarRegion::class),
    VAR_OBJECT(65, VarObject::class),
    VAR_CLAN(66, VarClan::class),
    VAR_CLAN_SETTING(67, VarClanSetting::class),
     */
    ;

    companion object {
        private val values = values()

        fun getArchive(id: Int, size: Int): Int = id.ushr(size)

        fun getFile(id: Int, size: Int): Int = (id and (1 shl size) - 1)

        fun forClass(kclass: KClass<*>): ResourceType? = values.firstOrNull { it.kclass == kclass }
    }
}