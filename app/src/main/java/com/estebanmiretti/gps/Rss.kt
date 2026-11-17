package com.estebanmiretti.gps

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(name = "rss", strict = false)
class Rss {

    @field:Element(name = "channel")
    private var channel: Channel? = null
    /*@field:Attribute(name = "xmlns:georss")
    private var xmlnsGeorss: String? = null
    @field:Attribute(name = "xmlns:geo")
    private var xmlnsGeo: String? = null
    @field:Attribute(name = "xmlns:gc")
    private var xmlnsDc: String? = null
    @field:Attribute(name = "version")
    private var version: String? = null*/


    fun getChannel(): Channel? {
        return channel
    }

    /*fun getXmlnsGeorss(): String? {
        return xmlnsGeorss
    }



    fun getXmlnsGeo(): String? {
        return xmlnsGeo
    }



    fun getXmlnsDc(): String? {
        return xmlnsDc
    }



    fun getVersion(): String? {
        return version
    }



*/


}