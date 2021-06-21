package com.opennxt.api.util

interface Cleanable {
    fun markDirty()
    fun isDirty(): Boolean
    fun clean()
}