package com.opennxt.model.worldlist

enum class WorldFlag(val bit: Int) {
    MEMBERS_ONLY(0),
    QUICK_CHAT(1),
    LOOT_SHARE(2),
    LEVEL_REQUIREMENT(7),
    VETERAN_WORLD(8),
    BETA_WORLD(16),
    HIGH_LEVEL_2000(18),
    HIGH_LEVEL_2600(19),
    VIP_WORLD(20),
    LEGACY_ONLY(22),
    EOC_ONLY(23),
    ;

    companion object {
        fun createFlag(vararg args: WorldFlag): Int {
            var flag = 0
            args.forEach {
                flag = flag or (1 shl it.bit)
            }
            return flag
        }
    }
}