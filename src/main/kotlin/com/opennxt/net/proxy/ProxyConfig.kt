package com.opennxt.net.proxy

import com.moandjiezana.toml.Toml
import com.opennxt.config.TomlConfig

class ProxyConfig: TomlConfig() {
    var usernames = ArrayList<String>()

    init {
        usernames.add("username")
        usernames.add("someone else")
    }

    override fun save(map: MutableMap<String, Any>) {
        map["usernames"] = usernames.clone()
    }

    override fun load(toml: Toml) {
        usernames = ArrayList(toml.getList("usernames", usernames).map { it.toLowerCase() })
    }
}