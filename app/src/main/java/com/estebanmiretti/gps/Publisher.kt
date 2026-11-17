package com.estebanmiretti.gps

class Publisher {
    private var prefix: Prefix? = null
    private var text: String? = null

    fun getPrefix(): Prefix? {
        return prefix
    }

    fun setPrefix(value: Prefix?) {
        prefix = value
    }

    fun getText(): String? {
        return text
    }

    fun setText(value: String?) {
        text = value
    }
}
