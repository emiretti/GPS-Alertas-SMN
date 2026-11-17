package com.estebanmiretti.gps

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "channel", strict = false)
class Channel {
    @field:Element(name = "copyright")
    private var copyright: String? = null

    @field:ElementList(name = "item", inline = true)
    private var item: List<Item>? = ArrayList<Item>()

    @field:Element(name = "link")
    private var link: String? = null

    @field:Element(name = "description")
    private var description: String? = null

    @field:Element(name = "language")
    private var language: String? = null

    @field:Element(name = "title")
    private var title: String? = null

    /*@field:Element(name = "publisher")
    private var publisher: Publisher? = null

    fun getPublisher(): Publisher? {
        return publisher
    }

    fun setPublisher(value: Publisher?) {
        publisher = value
    }
*/
    constructor() {
        this.item = item
        this.copyright = copyright
        this.link = link
        this.description = description
        this.language = language
        this.title = title
        // this.publisher = publisher
    }


    fun getCopyright(): String? {
        return copyright
    }


    fun getItem(): List<Item?>? {
        return item
    }


    fun getLink(): String? {
        return link
    }


    fun getDescription(): String? {
        return description
    }


    fun getLanguage(): String? {
        return language
    }


    fun getTitle(): String? {
        return title
    }


}
