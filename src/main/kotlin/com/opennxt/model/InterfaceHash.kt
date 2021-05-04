package com.opennxt.model

inline class InterfaceHash(val hash: Int) {
    val parent: Int
        get() = hash shr 16
    val component: Int
        get() = hash and 0xffff

    constructor(parent: Int, component: Int) : this((parent shl 16) or component)

    override fun toString(): String = "InterfaceHash(parent=$parent, component=$component)"
}