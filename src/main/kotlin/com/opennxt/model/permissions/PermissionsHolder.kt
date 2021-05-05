package com.opennxt.model.permissions

interface PermissionsHolder {
    fun hasPermissions(node: String): Boolean
}