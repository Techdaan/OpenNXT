package com.opennxt

import java.nio.file.Paths

object Constants {
    val DATA_PATH = Paths.get("./data/")
    val CLIENTS_PATH = DATA_PATH.resolve("clients")
    val LAUNCHERS_PATH = DATA_PATH.resolve("launchers")
    val CONFIG_PATH = DATA_PATH.resolve("config")
    val CACHE_PATH = DATA_PATH.resolve("cache")
    val PROT_PATH = DATA_PATH.resolve("prot")
    val RESOURCE_PATH = DATA_PATH.resolve("resources")
    val TEXTURES_PATH_DXT = DATA_PATH.resolve("textures/dxt")
    val TEXTURES_PATH_PNG = DATA_PATH.resolve("textures/png")
    val TEXTURES_PATH_MIP = DATA_PATH.resolve("textures/mipmap")
    val TEXTURES_PATH_ETC = DATA_PATH.resolve("textures/etc")
}