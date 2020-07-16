package com.zenaton.api.support

import java.util.Properties

class BuildInfo(private val properties: Properties) {
    val version: String
        get() = properties.getProperty("version")
}
