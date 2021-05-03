package com.opennxt.net.login

enum class LoginType(val id: Int) {
    GAME(16),
    LOBBY(19);

    companion object {
        private val VALUES = values()
        fun fromId(id: Int): LoginType? = VALUES.firstOrNull { it.id == id }
    }
}