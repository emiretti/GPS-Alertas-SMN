package com.estebanmiretti.gps


import java.io.IOException;

enum class Prefix {
    DC, GEORSS;

    open fun toValue(): String? {
        return when (this) {
            DC -> "dc"
            GEORSS -> "georss"
        }
        return null
    }

    @Throws(IOException::class)
    open fun forValue(value: String): Prefix? {
        if (value == "dc") return DC
        if (value == "georss") return GEORSS
        throw IOException("Cannot deserialize Prefix")
    }
}