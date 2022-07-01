package com.github.stevenlmcgraw.intellijcodegenerator2.config.include

import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXB
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType

@XmlAccessorType(XmlAccessType.FIELD)
class IncludeList(private var includes: MutableList<Include> = mutableListOf()) {

    constructor(include: Include) : this(mutableListOf(include))

    fun getIncludes(): MutableList<Include> {
        this.includes.forEach { it.regenerateId() }
        return this.includes
    }

    companion object {

        fun fromXML(xml: String): List<Include> {
            val includeList = JAXB.unmarshal(StringReader(xml), IncludeList::class.java)
            return includeList.getIncludes()
        }

        fun toXML(includes: MutableList<Include>): String {
            val includeList = IncludeList(includes)
            val stringWriter = StringWriter()
            JAXB.marshal(includeList, stringWriter)
            return stringWriter.toString()
        }

        fun toXML(include: Include): String {
            val includeList = IncludeList(include)
            val stringWriter = StringWriter()
            JAXB.marshal(includeList, stringWriter)
            return stringWriter.toString()
        }
    }
}