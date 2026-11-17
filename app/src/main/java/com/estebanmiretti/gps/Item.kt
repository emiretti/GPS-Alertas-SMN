package com.estebanmiretti.gps

import org.simpleframework.xml.Element
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.Root

@Root(name = "item", strict = false)
class Item {
    @field:Element(name = "link")
    private var link: String? = null

    @field:Element(name = "description", data = true)
    private var description: String? = null

    @field:Element(name = "title", data = true)
    private var title: String? = null

    @Namespace(reference = "http://www.georss.org/georss", prefix = "georss")
    @field:Element(name = "polygon", data = true, required = false)
    private var polygon: String? = null

    fun getPolygon(): String? {
        return polygon
    }
    /*private var polygon: Publisher? = null
fun getPolygon(): Publisher? {
    return polygon
}

fun setPolygon(value: Publisher?) {
    polygon = value
}
@field:Element(name = "georss:polygon")
private var georss: String? = null

fun getGeorss(): String? {
    return georss
}*/

    fun getLink(): String? {
        return link
    }


    fun getDescription(): String? {
        return description
    }


    fun getTitle(): String? {
        return title
    }

}

