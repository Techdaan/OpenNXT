package com.opennxt.content.interfaces

import com.opennxt.OpenNXT
import com.opennxt.resources.config.enums.EnumDefinition
import com.opennxt.resources.config.structs.StructDefinition

/**
 * Represents a pre-defined slot on the in-game root interface
 *
 * Please note this enum has variable fields. This is not how enums should work, but is the best way of loading the
 * interface data from the cache. Forgive me for that :(
 */
enum class InterfaceSlot(val id: Int) {
    SKILLS(0),
    BACKPACK(2),
    WORN_EQUIPMENT(3),
    PRAYER_ABILITIES(4),
    MAGIC_BOOK(5),
    MELEE_ABILITIES(6),
    RANGED_ABILITIES(7),
    DEFENSIVE_ABILITIES(8),
    EMOTES(9),
    MUSIC_PLAYER(10),
    NOTES(11),
    FAMILIAR(12),
    FRIENDS_LIST(14),
    FRIENDS_CHAT_LIST(15),
    CLAN_CHAT_LIST(16),
    MINIGAMES(17),
    ALL_CHAT(18),
    PRIVATE_CHAT(19),
    FRIENDS_CHAT(20),
    CLAN_CHAT(21),
    GUEST_CLAN_CHAT(22),
    TRADE_AND_ASSISTANCE(23),
    TWITCH_CHAT(24),
    GROUP_CHAT(25),
    TWITCH_STREAM(26),
    GROUP_CHAT_LIST(27),
    METRICS(28),
    DROPS(29),
    GRAPHS(30),
    QUEST_LIST(31),
    ACHIEVEMENT_TRACKER(32),
    MAGIC_ABILITIES(33),
    COMBAT_SPELLS(34),
    TELEPORT_SPELLS(35),
    SKILLING_SPELLS(36),
    ATTACK_ABILITIES(37),
    STRENGTH_ABILITIES(38),
    DEFENCE_ABILITIES(39),
    CONSTITUTION_ABILITIES(40),
    ACHIEVEMENT_PATHS(41),
    GAME_VIEW(1000),
    BUTTONS(1002),
    ACTION_BAR(1003),
    MINIMAP(1004),
    MINIGAME_HUD(1005),
    GAME_DIALOG(1006),
    CENTRAL_INTERFACE(1007),
    BUFF_BAR(1009),
    GRAVE_TIMER(1010),
    TASK_COMPLETE(1012),
    GRAVE_INTERFACE(1013),
    AREA_STATUS(1014),
    XP_TRACKER(1015),
    SUBSCRIBE(1016),
    BANK(1017),
    CRAFTING_PROGRESS(1018),
    SPLIT_PRIVATE_CHAT(1019),
    BOSS_TIMER(1021),
    GROUP_INVITATIONS(1023),
    PLAYER_INSPECT(1024),
    CLOCK(1025),
    XP_POPUPS(1026),
    JMOD_TOOLBOX(1027),
    LOOT(1028),
    DEBUG_TEXT(1029),
    CHALLENGE_GEM(1030),
    SLAYER_COUNTER(1031),
    SECONDARY_ACTION_BAR(1032),
    TERTIARY_ACTION_BAR(1033),
    QUATERNARY_ACTION_BAR(1034),
    QUINARY_ACTION_BAR(1035),
    BXP_COUNTDOWN(1036),
    EVENTS(1037),
    DEBUFF_BAR(1038),
    CENTRAL_OVERLAY_INTERFACE(1040),
    DUNGEONEERING_MAP(1041),
    COMBAT_INFORMATION(1042),
    MOBILE_ACTION_BAR(1043),
    MOBILE_REVO_BAR(1044),
    EXTRA_ACTION_BUTTON(1045),
    DAY_PLANNER(1046),
    CENTRAL_INTERFACE_LARGE(1047),
    INVENTORY_DRAG_OPTIONS(1048),
    EDIT_MODE(2000),
    PANEL_2001(2001),
    PANEL_2002(2002),
    PANEL_2003(2003),
    PANEL_2004(2004),
    PANEL_2005(2005),
    COMBAT_TARGET(2008),
    ;

    /**
     * The id of the interface where this slot is added
     */
    var parent = -1

    /**
     * The component of the interface where this slot is added
     */
    var component = -1

    /**
     * The struct that defines this slot
     */
    lateinit var struct: StructDefinition

    companion object {
        val VALUES = values()

        /**
         * Reloads the interface slots from the cache
         */
        fun reload() {
            val enum = OpenNXT.resources.get<EnumDefinition>(7716) ?: throw NullPointerException("InterfaceSlot enum not found")

            VALUES.forEach { slot ->
                val struct = OpenNXT.resources.get<StructDefinition>(enum.values[slot.id] as Int) ?: throw NullPointerException("InterfaceSlot illegal struct: ${enum.values[slot.id]} in slot $slot")
                val hash = struct.getInt(3505)

                slot.struct = struct
                slot.parent = hash shr 16
                slot.component = hash and 0xffff
            }
        }
    }
}