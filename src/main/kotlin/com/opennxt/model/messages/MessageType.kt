package com.opennxt.model.messages

enum class MessageType(val id: Int) {
    UNFILTERABLE_GAME(0),
    CONSOLE_ERROR(96),
    CONSOLE_AUTOCOMPLETE(98),
    CONSOLE(99),
    FILTERABLE_GAME(109)
}