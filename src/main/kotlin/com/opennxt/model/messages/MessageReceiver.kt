package com.opennxt.model.messages

interface MessageReceiver {
    fun message(message: Message)
    fun message(message: String)
    fun error(message: String)
}